package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Bill
import com.example.data.model.Budget
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.ProfileType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk terlebih dahulu.")
    }

    // --- ACCOUNTS ---

    private fun accountsCol(uid: String) =
        db.collection("users").document(uid).collection("accounts")

    fun observeAccounts(profile: ProfileType): Flow<List<Account>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/accounts"
        emitAll(
            accountsCol(uid)
                .whereEqualTo("profile", profile.name)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Account::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createAccount(account: Account): String {
        val uid = requireUserId()
        val docRef = accountsCol(uid).document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "name" to account.name,
            "type" to account.type,
            "balance" to account.balance,
            "initialBalance" to account.balance,
            "accountNumber" to account.accountNumber,
            "colorHex" to account.colorHex,
            "profile" to account.profile,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAccount(account: Account) {
        val uid = requireUserId()
        require(account.id.isNotBlank()) { "Account ID tidak boleh kosong" }
        val docRef = accountsCol(uid).document(account.id)
        val payload = hashMapOf<String, Any>(
            "name" to account.name,
            "type" to account.type,
            "balance" to account.balance,
            "accountNumber" to account.accountNumber,
            "colorHex" to account.colorHex,
            "profile" to account.profile,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAccount(accountId: String) {
        val uid = requireUserId()
        val docRef = accountsCol(uid).document(accountId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun ensureDefaultAccounts(profile: ProfileType) {
        val uid = requireUserId()
        val existing = accountsCol(uid).whereEqualTo("profile", profile.name).limit(1).get().await()
        if (existing.isEmpty) {
            createAccount(
                Account(
                    name = "Dompet Tunai",
                    type = AccountType.CASH.name,
                    balance = 500000.0,
                    initialBalance = 500000.0,
                    colorHex = "#10B981",
                    profile = profile.name
                )
            )
            createAccount(
                Account(
                    name = "Rekening Bank Utama",
                    type = AccountType.BANK.name,
                    balance = 2500000.0,
                    initialBalance = 2500000.0,
                    accountNumber = "BCA - 1234567890",
                    colorHex = "#3B82F6",
                    profile = profile.name
                )
            )
        }
    }

    // --- TRANSACTIONS ---

    private fun transactionsCol(uid: String) =
        db.collection("users").document(uid).collection("transactions")

    fun observeTransactions(profile: ProfileType): Flow<List<Transaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            transactionsCol(uid)
                .whereEqualTo("profile", profile.name)
                .orderBy("date", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Transaction::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addTransaction(transaction: Transaction) {
        val uid = requireUserId()
        val docRef = transactionsCol(uid).document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "type" to transaction.type,
            "amount" to transaction.amount,
            "category" to transaction.category,
            "date" to (transaction.date ?: Timestamp.now()),
            "accountId" to transaction.accountId,
            "toAccountId" to transaction.toAccountId,
            "paymentMethod" to transaction.paymentMethod,
            "notes" to transaction.notes,
            "tag" to transaction.tag,
            "isRecurring" to transaction.isRecurring,
            "recurringFrequency" to transaction.recurringFrequency,
            "status" to transaction.status,
            "receiptUrl" to transaction.receiptUrl,
            "profile" to transaction.profile,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            db.runTransaction { tx ->
                // READS FIRST
                val accRef = accountsCol(uid).document(transaction.accountId)
                val accSnap = tx.get(accRef)
                val currBalance = accSnap.getDouble("balance") ?: 0.0

                val toRef = if (transaction.type == TransactionType.TRANSFER.name && transaction.toAccountId.isNotBlank()) {
                    accountsCol(uid).document(transaction.toAccountId)
                } else null
                val toSnap = toRef?.let { tx.get(it) }
                val toBalance = toSnap?.getDouble("balance") ?: 0.0

                // WRITES SECOND
                tx.set(docRef, payload)
                when (transaction.type) {
                    TransactionType.EXPENSE.name -> {
                        tx.update(accRef, "balance", currBalance - transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                    }
                    TransactionType.INCOME.name -> {
                        tx.update(accRef, "balance", currBalance + transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                    }
                    TransactionType.TRANSFER.name -> {
                        tx.update(accRef, "balance", currBalance - transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                        if (toRef != null) {
                            tx.update(toRef, "balance", toBalance + transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                        }
                    }
                }
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        val uid = requireUserId()
        val docRef = transactionsCol(uid).document(transaction.id)
        try {
            db.runTransaction { tx ->
                // READS FIRST
                val accRef = accountsCol(uid).document(transaction.accountId)
                val accSnap = tx.get(accRef)
                val currBalance = if (accSnap.exists()) accSnap.getDouble("balance") ?: 0.0 else null

                val toRef = if (transaction.type == TransactionType.TRANSFER.name && transaction.toAccountId.isNotBlank()) {
                    accountsCol(uid).document(transaction.toAccountId)
                } else null
                val toSnap = toRef?.let { tx.get(it) }
                val toBalance = if (toSnap != null && toSnap.exists()) toSnap.getDouble("balance") ?: 0.0 else null

                // WRITES SECOND
                tx.delete(docRef)
                when (transaction.type) {
                    TransactionType.EXPENSE.name -> {
                        if (currBalance != null) {
                            tx.update(accRef, "balance", currBalance + transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                        }
                    }
                    TransactionType.INCOME.name -> {
                        if (currBalance != null) {
                            tx.update(accRef, "balance", currBalance - transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                        }
                    }
                    TransactionType.TRANSFER.name -> {
                        if (currBalance != null) {
                            tx.update(accRef, "balance", currBalance + transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                        }
                        if (toRef != null && toBalance != null) {
                            tx.update(toRef, "balance", toBalance - transaction.amount, "updatedAt", FieldValue.serverTimestamp())
                        }
                    }
                }
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction) {
        // Revert old transaction and apply new transaction atomically
        deleteTransaction(oldTx)
        addTransaction(newTx)
    }

    // --- BUDGETS ---

    private fun budgetsCol(uid: String) =
        db.collection("users").document(uid).collection("budgets")

    fun observeBudgets(month: String, profile: ProfileType): Flow<List<Budget>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/budgets"
        emitAll(
            budgetsCol(uid)
                .whereEqualTo("profile", profile.name)
                .whereEqualTo("month", month)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Budget::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveBudget(budget: Budget) {
        val uid = requireUserId()
        val docRef = if (budget.id.isNotBlank()) budgetsCol(uid).document(budget.id) else budgetsCol(uid).document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "category" to budget.category,
            "amount" to budget.amount,
            "month" to budget.month,
            "profile" to budget.profile,
            "createdAt" to (budget.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBudget(budgetId: String) {
        val uid = requireUserId()
        val docRef = budgetsCol(uid).document(budgetId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- BILLS ---

    private fun billsCol(uid: String) =
        db.collection("users").document(uid).collection("bills")

    fun observeBills(profile: ProfileType): Flow<List<Bill>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/bills"
        emitAll(
            billsCol(uid)
                .whereEqualTo("profile", profile.name)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Bill::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveBill(bill: Bill) {
        val uid = requireUserId()
        val docRef = if (bill.id.isNotBlank()) billsCol(uid).document(bill.id) else billsCol(uid).document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "name" to bill.name,
            "amount" to bill.amount,
            "dueDate" to (bill.dueDate ?: Timestamp.now()),
            "category" to bill.category,
            "isPaid" to bill.isPaid,
            "recurring" to bill.recurring,
            "profile" to bill.profile,
            "createdAt" to (bill.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun toggleBillPaid(bill: Bill) {
        val uid = requireUserId()
        val docRef = billsCol(uid).document(bill.id)
        try {
            docRef.update(
                mapOf(
                    "isPaid" to !bill.isPaid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBill(billId: String) {
        val uid = requireUserId()
        val docRef = billsCol(uid).document(billId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- DEBTS (Utang & Piutang) ---

    private fun debtsCol(uid: String) =
        db.collection("users").document(uid).collection("debts")

    fun observeDebts(profile: ProfileType): Flow<List<Debt>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/debts"
        emitAll(
            debtsCol(uid)
                .whereEqualTo("profile", profile.name)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Debt::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveDebt(debt: Debt) {
        val uid = requireUserId()
        val docRef = if (debt.id.isNotBlank()) debtsCol(uid).document(debt.id) else debtsCol(uid).document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "type" to debt.type,
            "personName" to debt.personName,
            "amount" to debt.amount,
            "paidAmount" to debt.paidAmount,
            "dueDate" to (debt.dueDate ?: Timestamp.now()),
            "notes" to debt.notes,
            "isSettled" to debt.isSettled,
            "profile" to debt.profile,
            "createdAt" to (debt.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun addDebtPayment(debt: Debt, paymentAmount: Double) {
        val uid = requireUserId()
        val docRef = debtsCol(uid).document(debt.id)
        val newPaid = debt.paidAmount + paymentAmount
        val settled = newPaid >= debt.amount
        try {
            docRef.update(
                mapOf(
                    "paidAmount" to newPaid,
                    "isSettled" to settled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDebt(debtId: String) {
        val uid = requireUserId()
        val docRef = debtsCol(uid).document(debtId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- SAVINGS GOALS (Celengan Impian) ---

    private fun goalsCol(uid: String) =
        db.collection("users").document(uid).collection("goals")

    fun observeGoals(profile: ProfileType): Flow<List<Goal>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/goals"
        emitAll(
            goalsCol(uid)
                .whereEqualTo("profile", profile.name)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Goal::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveGoal(goal: Goal) {
        val uid = requireUserId()
        val docRef = if (goal.id.isNotBlank()) goalsCol(uid).document(goal.id) else goalsCol(uid).document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "targetDate" to (goal.targetDate ?: Timestamp.now()),
            "icon" to goal.icon,
            "profile" to goal.profile,
            "createdAt" to (goal.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun addGoalDeposit(goal: Goal, depositAmount: Double) {
        val uid = requireUserId()
        val docRef = goalsCol(uid).document(goal.id)
        val newAmount = goal.currentAmount + depositAmount
        try {
            docRef.update(
                mapOf(
                    "currentAmount" to newAmount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteGoal(goalId: String) {
        val uid = requireUserId()
        val docRef = goalsCol(uid).document(goalId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
