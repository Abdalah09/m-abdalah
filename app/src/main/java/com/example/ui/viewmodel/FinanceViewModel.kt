package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.FinanceAiService
import com.example.data.ai.ParsedDraftTransaction
import com.example.data.model.Account
import com.example.data.model.Bill
import com.example.data.model.Budget
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.ProfileType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FirestoreRepository
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class PeriodFilter(val label: String) {
    TODAY("Hari Ini"),
    THIS_WEEK("Minggu Ini"),
    THIS_MONTH("Bulan Ini"),
    THIS_YEAR("Tahun Ini"),
    ALL("Semua")
}

enum class SortOrder(val label: String) {
    NEWEST("Terbaru"),
    OLDEST("Terlama"),
    HIGHEST("Nominal Terbesar"),
    LOWEST("Nominal Terkecil")
}

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val expenseRatio: Double = 0.0,
    val savingsRate: Double = 0.0,
    val predictedEndOfMonthBalance: Double = 0.0,
    val unusualExpenseWarning: String? = null
)

data class FilterParams(
    val period: PeriodFilter = PeriodFilter.THIS_MONTH,
    val query: String = "",
    val category: String? = null,
    val type: String? = null,
    val sort: SortOrder = SortOrder.NEWEST
)

class FinanceViewModel(
    private val repository: FirestoreRepository,
    private val aiService: FinanceAiService = FinanceAiService()
) : ViewModel() {

    private val _activeProfile = MutableStateFlow(ProfileType.PERSONAL)
    val activeProfile: StateFlow<ProfileType> = _activeProfile.asStateFlow()

    private val _periodFilter = MutableStateFlow(PeriodFilter.THIS_MONTH)
    val periodFilter: StateFlow<PeriodFilter> = _periodFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow<String?>(null)
    val typeFilter: StateFlow<String?> = _typeFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    val bills: StateFlow<List<Bill>> = _bills.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    // AI States
    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _nlpDraft = MutableStateFlow<ParsedDraftTransaction?>(null)
    val nlpDraft: StateFlow<ParsedDraftTransaction?> = _nlpDraft.asStateFlow()

    private val _isNlpLoading = MutableStateFlow(false)
    val isNlpLoading: StateFlow<Boolean> = _isNlpLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        loadDataForProfile(_activeProfile.value)
    }

    fun toggleDarkMode(enable: Boolean) {
        _isDarkMode.value = enable
    }

    fun switchProfile(profile: ProfileType) {
        _activeProfile.value = profile
        loadDataForProfile(profile)
    }

    private fun loadDataForProfile(profile: ProfileType) {
        viewModelScope.launch {
            repository.ensureDefaultAccounts(profile)
        }

        viewModelScope.launch {
            repository.observeAccounts(profile).collect { list ->
                _accounts.value = list
            }
        }

        viewModelScope.launch {
            repository.observeTransactions(profile).collect { list ->
                _transactions.value = list
            }
        }

        viewModelScope.launch {
            val currentMonth = FormatUtils.currentMonthString()
            repository.observeBudgets(currentMonth, profile).collect { list ->
                _budgets.value = list
            }
        }

        viewModelScope.launch {
            repository.observeBills(profile).collect { list ->
                _bills.value = list
            }
        }

        viewModelScope.launch {
            repository.observeDebts(profile).collect { list ->
                _debts.value = list
            }
        }

        viewModelScope.launch {
            repository.observeGoals(profile).collect { list ->
                _goals.value = list
            }
        }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        _periodFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun setTypeFilter(type: String?) {
        _typeFilter.value = type
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Filtered Transactions
    private val filterParams = combine(
        _periodFilter,
        _searchQuery,
        _categoryFilter,
        _typeFilter,
        _sortOrder
    ) { period, query, cat, type, sort ->
        FilterParams(period, query, cat, type, sort)
    }

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _transactions,
        filterParams
    ) { txs, filters ->
        val now = Calendar.getInstance()
        val filteredByPeriod = txs.filter { tx ->
            val txCal = Calendar.getInstance().apply {
                time = tx.date?.toDate() ?: now.time
            }
            when (filters.period) {
                PeriodFilter.TODAY -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    txCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                }
                PeriodFilter.THIS_WEEK -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    txCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                }
                PeriodFilter.THIS_MONTH -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    txCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
                PeriodFilter.THIS_YEAR -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
                PeriodFilter.ALL -> true
            }
        }

        val filteredByQuery = if (filters.query.isBlank()) {
            filteredByPeriod
        } else {
            val q = filters.query.trim().lowercase()
            filteredByPeriod.filter {
                it.notes.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.tag.lowercase().contains(q)
            }
        }

        val filteredByCat = if (filters.category == null) filteredByQuery else filteredByQuery.filter { it.category == filters.category }
        val filteredByType = if (filters.type == null) filteredByCat else filteredByCat.filter { it.type == filters.type }

        when (filters.sort) {
            SortOrder.NEWEST -> filteredByType.sortedByDescending { it.date?.seconds ?: 0L }
            SortOrder.OLDEST -> filteredByType.sortedBy { it.date?.seconds ?: 0L }
            SortOrder.HIGHEST -> filteredByType.sortedByDescending { it.amount }
            SortOrder.LOWEST -> filteredByType.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary calculations
    val summary: StateFlow<FinancialSummary> = combine(
        _accounts,
        filteredTransactions,
        _bills
    ) { accs, txs, billsList ->
        val totalBal = accs.sumOf { it.balance }
        val income = txs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val expense = txs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val net = income - expense
        val ratio = if (income > 0) (expense / income) * 100.0 else 0.0
        val savings = if (income > 0) (net / income) * 100.0 else 0.0

        // End of month prediction
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val remainingDays = (maxDays - currentDay).coerceAtLeast(0)
        val dailyBurn = if (currentDay > 0) expense / currentDay else 0.0
        val upcomingUnpaidBills = billsList.filter { !it.isPaid }.sumOf { it.amount }
        val forecastedBalance = (totalBal - (dailyBurn * remainingDays) - upcomingUnpaidBills).coerceAtLeast(0.0)

        // Anomaly Detection: check if any single expense exceeds 40% of total expense and > Rp500.000
        val highExpense = txs.filter { it.type == TransactionType.EXPENSE.name && it.amount > 500000.0 && it.amount > (expense * 0.4) }
            .maxByOrNull { it.amount }
        val warning = if (highExpense != null) {
            "Peringatan: Pengeluaran untuk '${highExpense.category}' sebesar ${FormatUtils.formatRupiah(highExpense.amount)} mencapai ${(highExpense.amount / expense * 100).toInt()}% dari total pengeluaran periode ini."
        } else null

        FinancialSummary(
            totalBalance = totalBal,
            totalIncome = income,
            totalExpense = expense,
            netCashFlow = net,
            expenseRatio = ratio,
            savingsRate = savings,
            predictedEndOfMonthBalance = forecastedBalance,
            unusualExpenseWarning = warning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    // CRUD Actions
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.addTransaction(transaction.copy(profile = _activeProfile.value.name))
                _snackbarMessage.value = "Transaksi berhasil dicatat"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menambah transaksi: ${e.localizedMessage}"
            }
        }
    }

    fun updateTransaction(oldTx: Transaction, newTx: Transaction) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(oldTx, newTx.copy(profile = _activeProfile.value.name))
                _snackbarMessage.value = "Transaksi berhasil diperbarui"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal memperbarui transaksi: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                _snackbarMessage.value = "Transaksi berhasil dihapus"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menghapus transaksi: ${e.localizedMessage}"
            }
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.createAccount(account.copy(profile = _activeProfile.value.name))
                _snackbarMessage.value = "Rekening '${account.name}' berhasil ditambahkan"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal membuat rekening: ${e.localizedMessage}"
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.updateAccount(account)
                _snackbarMessage.value = "Rekening diperbarui"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal memperbarui rekening: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
                _snackbarMessage.value = "Rekening dihapus"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menghapus rekening: ${e.localizedMessage}"
            }
        }
    }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                repository.saveBudget(budget.copy(profile = _activeProfile.value.name))
                _snackbarMessage.value = "Anggaran berhasil disimpan"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menyimpan anggaran: ${e.localizedMessage}"
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBudget(budgetId)
                _snackbarMessage.value = "Anggaran dihapus"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menghapus anggaran"
            }
        }
    }

    fun saveBill(bill: Bill) {
        viewModelScope.launch {
            try {
                repository.saveBill(bill.copy(profile = _activeProfile.value.name))
                _snackbarMessage.value = "Tagihan berhasil disimpan"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menyimpan tagihan"
            }
        }
    }

    fun toggleBillPaid(bill: Bill) {
        viewModelScope.launch {
            try {
                repository.toggleBillPaid(bill)
                _snackbarMessage.value = if (!bill.isPaid) "Tagihan ditandai Lunas" else "Tagihan ditandai Belum Lunas"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal mengubah status tagihan"
            }
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBill(billId)
                _snackbarMessage.value = "Tagihan dihapus"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menghapus tagihan"
            }
        }
    }

    fun saveDebt(debt: Debt) {
        viewModelScope.launch {
            try {
                repository.saveDebt(debt.copy(profile = _activeProfile.value.name))
                _snackbarMessage.value = "Catatan utang/piutang disimpan"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menyimpan data utang"
            }
        }
    }

    fun addDebtPayment(debt: Debt, amount: Double) {
        viewModelScope.launch {
            try {
                repository.addDebtPayment(debt, amount)
                _snackbarMessage.value = "Pembayaran sebesar ${FormatUtils.formatRupiah(amount)} dicatat"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal mencatat pembayaran"
            }
        }
    }

    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDebt(debtId)
                _snackbarMessage.value = "Data utang/piutang dihapus"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menghapus utang"
            }
        }
    }

    fun saveGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                repository.saveGoal(goal.copy(profile = _activeProfile.value.name))
                _snackbarMessage.value = "Target tabungan disimpan"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menyimpan target"
            }
        }
    }

    fun addGoalDeposit(goal: Goal, amount: Double) {
        viewModelScope.launch {
            try {
                repository.addGoalDeposit(goal, amount)
                _snackbarMessage.value = "Setoran tabungan sebesar ${FormatUtils.formatRupiah(amount)} berhasil!"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menambah setoran"
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                repository.deleteGoal(goalId)
                _snackbarMessage.value = "Target tabungan dihapus"
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal menghapus target"
            }
        }
    }

    // AI Natural Text Transaction Input
    fun parseNaturalLanguageTransaction(text: String, onResult: (ParsedDraftTransaction?) -> Unit) {
        viewModelScope.launch {
            _isNlpLoading.value = true
            val result = aiService.parseNaturalTextTransaction(text)
            _isNlpLoading.value = false
            if (result.isSuccess) {
                _nlpDraft.value = result.getOrNull()
                onResult(result.getOrNull())
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Gagal memproses teks dengan AI"
                onResult(null)
            }
        }
    }

    // AI Receipt Scanner
    fun parseReceiptImage(bitmap: Bitmap, onResult: (ParsedDraftTransaction?) -> Unit) {
        viewModelScope.launch {
            _isNlpLoading.value = true
            val result = aiService.parseReceiptImage(bitmap)
            _isNlpLoading.value = false
            if (result.isSuccess) {
                _nlpDraft.value = result.getOrNull()
                onResult(result.getOrNull())
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Gagal memindai struk belanja"
                onResult(null)
            }
        }
    }

    fun clearNlpDraft() {
        _nlpDraft.value = null
    }

    // AI Financial Advisor Consultation
    fun askFinancialAdvisor(question: String? = null) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiAdvice.value = null
            val result = aiService.getFinancialAdvice(
                accounts = _accounts.value,
                transactions = _transactions.value,
                budgets = _budgets.value,
                userQuestion = question
            )
            _isAiLoading.value = false
            if (result.isSuccess) {
                _aiAdvice.value = result.getOrNull()
            } else {
                _aiAdvice.value = "Maaf, terjadi kendala saat menghubungi asisten AI: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
