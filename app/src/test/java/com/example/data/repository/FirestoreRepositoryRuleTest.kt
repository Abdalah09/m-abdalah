package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.ProfileType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canCreateAccountAndAddTransaction() = runBlocking {
        val uid = signInTestUser("alice_test@example.com")
        val repository = FirestoreRepository(firestore, auth)

        val account = Account(
            name = "Dompet Tunai Test",
            type = AccountType.CASH.name,
            balance = 100000.0,
            initialBalance = 100000.0,
            profile = ProfileType.PERSONAL.name
        )
        val accId = repository.createAccount(account)
        assertTrue(accId.isNotBlank())

        val accounts = repository.observeAccounts(ProfileType.PERSONAL).first()
        assertTrue(accounts.any { it.name == "Dompet Tunai Test" })

        val tx = Transaction(
            type = TransactionType.EXPENSE.name,
            amount = 25000.0,
            category = "Makanan",
            accountId = accId,
            notes = "Makan siang test",
            profile = ProfileType.PERSONAL.name,
            date = Timestamp.now()
        )
        repository.addTransaction(tx)

        val transactions = repository.observeTransactions(ProfileType.PERSONAL).first()
        assertTrue(transactions.any { it.notes == "Makan siang test" })
    }

    @Test(expected = IllegalStateException::class)
    fun unauthenticatedUser_cannotAccessRepository() = runBlocking {
        auth.signOut()
        val repository = FirestoreRepository(firestore, auth)
        val account = Account(
            name = "Illegal Account",
            type = AccountType.CASH.name,
            balance = 50000.0,
            profile = ProfileType.PERSONAL.name
        )
        repository.createAccount(account)
        Unit
    }
}
