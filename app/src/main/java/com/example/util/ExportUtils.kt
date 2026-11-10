package com.example.util

import com.example.data.model.Account
import com.example.data.model.Bill
import com.example.data.model.Budget
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.Transaction
import org.json.JSONArray
import org.json.JSONObject

object ExportUtils {

    fun exportTransactionsToCsv(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append("Tanggal,Jenis,Kategori,Nominal,Metode,Catatan,Label,Status\n")
        transactions.forEach { tx ->
            val dateStr = FormatUtils.formatIndonesianDate(tx.date).replace(",", " ")
            val notesClean = tx.notes.replace("\"", "\"\"").replace("\n", " ")
            sb.append("\"$dateStr\",")
            sb.append("\"${tx.type}\",")
            sb.append("\"${tx.category}\",")
            sb.append("${tx.amount.toLong()},")
            sb.append("\"${tx.paymentMethod}\",")
            sb.append("\"$notesClean\",")
            sb.append("\"${tx.tag}\",")
            sb.append("\"${tx.status}\"\n")
        }
        return sb.toString()
    }

    fun exportBackupJson(
        accounts: List<Account>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        bills: List<Bill>,
        debts: List<Debt>,
        goals: List<Goal>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Dompet Pintar")
        root.put("exportedAt", System.currentTimeMillis())

        val accArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type)
                put("balance", acc.balance)
                put("accountNumber", acc.accountNumber)
                put("colorHex", acc.colorHex)
                put("profile", acc.profile)
            }
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("amount", tx.amount)
                put("category", tx.category)
                put("accountId", tx.accountId)
                put("toAccountId", tx.toAccountId)
                put("paymentMethod", tx.paymentMethod)
                put("notes", tx.notes)
                put("tag", tx.tag)
                put("profile", tx.profile)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }
}
