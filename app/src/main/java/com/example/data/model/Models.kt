package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProfileType {
    PERSONAL,
    FAMILY
}

enum class AccountType(val label: String) {
    CASH("Uang Tunai"),
    BANK("Rekening Bank"),
    E_WALLET("E-Wallet"),
    SAVINGS("Tabungan"),
    BUSINESS("Bisnis")
}

enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer")
}

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    DEBIT("Kartu Debit"),
    KREDIT("Kartu Kredit"),
    E_WALLET("E-Wallet"),
    QRIS("QRIS"),
    TRANSFER_BANK("Transfer Bank")
}

enum class RecurringFrequency(val label: String) {
    NONE("Tidak Berulang"),
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan")
}

@IgnoreExtraProperties
data class Account(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = AccountType.CASH.name,
    val balance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val accountNumber: String = "",
    val colorHex: String = "#10B981",
    val profile: String = ProfileType.PERSONAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double = 0.0,
    val category: String = "Makanan",
    val date: Timestamp? = null,
    val accountId: String = "",
    val toAccountId: String = "",
    val paymentMethod: String = PaymentMethod.TUNAI.name,
    val notes: String = "",
    val tag: String = "",
    val isRecurring: Boolean = false,
    val recurringFrequency: String = RecurringFrequency.NONE.name,
    val status: String = "COMPLETED",
    val receiptUrl: String = "",
    val profile: String = ProfileType.PERSONAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Budget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val month: String = "", // Format: "yyyy-MM" (misal: "2026-10")
    val profile: String = ProfileType.PERSONAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Bill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val dueDate: Timestamp? = null,
    val category: String = "Tagihan",
    val isPaid: Boolean = false,
    val recurring: String = RecurringFrequency.MONTHLY.name,
    val profile: String = ProfileType.PERSONAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Debt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "DEBT", // "DEBT" (utang saya ke orang) atau "RECEIVABLE" (orang berutang ke saya)
    val personName: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDate: Timestamp? = null,
    val notes: String = "",
    val isSettled: Boolean = false,
    val profile: String = ProfileType.PERSONAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Goal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: Timestamp? = null,
    val icon: String = "savings",
    val profile: String = ProfileType.PERSONAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
