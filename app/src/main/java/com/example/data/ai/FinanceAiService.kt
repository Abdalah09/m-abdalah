package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.Account
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "FinanceAiService"

data class ParsedDraftTransaction(
    val type: String, // INCOME or EXPENSE
    val amount: Double,
    val category: String,
    val notes: String,
    val paymentMethod: String,
    val tag: String
)

class FinanceAiService {

    // 1. Model for Structured Transaction Extraction from Free Text / Natural Language
    private val nlpExtractSchema = Schema.obj(
        properties = mapOf(
            "type" to Schema.enumeration(listOf("INCOME", "EXPENSE"), description = "Jenis transaksi (INCOME atau EXPENSE)"),
            "amount" to Schema.double(description = "Nominal uang dalam Rupiah angka murni, misal 25000"),
            "category" to Schema.string(description = "Kategori umum transaksi, misal Makanan, Transportasi, Belanja, Gaji, dll."),
            "notes" to Schema.string(description = "Catatan atau keterangan transaksi"),
            "paymentMethod" to Schema.string(description = "Metode pembayaran terdeteksi seperti TUNAI, E_WALLET, QRIS, DEBIT, atau TRANSFER_BANK"),
            "tag" to Schema.string(description = "Tag atau label, misal Pribadi, Kantor, Liburan")
        ),
        optionalProperties = listOf("paymentMethod", "tag")
    )

    private val nlpModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = nlpExtractSchema
            }
        )
    }

    // 2. Model for Financial Advisor & Freeform Text Analysis
    private val advisorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    // Parse free-form natural language text into a structured draft transaction
    suspend fun parseNaturalTextTransaction(inputText: String): Result<ParsedDraftTransaction> {
        return try {
            val prompt = """
                Ekstrak detail transaksi keuangan dalam bahasa Indonesia dari kalimat berikut:
                "$inputText"
                Pastikan nominal dalam angka murni Rupiah, tentukan jenis transaksi (INCOME atau EXPENSE), kategori yang relevan, catatan, dan metode pembayaran jika disebutkan.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                nlpModel.generateContent(prompt)
            }

            val jsonText = response.text ?: throw IllegalStateException("Model tidak memberikan respon.")
            val json = JSONObject(jsonText)
            val draft = ParsedDraftTransaction(
                type = json.optString("type", "EXPENSE"),
                amount = json.optDouble("amount", 0.0),
                category = json.optString("category", "Lainnya"),
                notes = json.optString("notes", inputText),
                paymentMethod = json.optString("paymentMethod", "TUNAI"),
                tag = json.optString("tag", "")
            )
            Result.success(draft)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Parsing transaksi timeout", e)
            Result.failure(Exception("Waktu proses melebihi batas. Silakan coba lagi."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai teks transaksi dengan AI", e)
            Result.failure(e)
        }
    }

    // Multimodal receipt / invoice image scanner
    suspend fun parseReceiptImage(bitmap: Bitmap): Result<ParsedDraftTransaction> {
        return try {
            val promptText = """
                Analisis foto struk / kuitansi / invoice belanja ini.
                Ekstrak informasi berikut dalam JSON:
                - type: "EXPENSE"
                - amount: total belanja akhir dalam angka Rupiah
                - category: kategori yang cocok (misal: Makanan, Belanja, Transportasi, Kesehatan)
                - notes: nama toko / merchant dan item utama
                - paymentMethod: metode pembayaran jika ada di struk (TUNAI, QRIS, DEBIT, E_WALLET)
                - tag: "Struk"
            """.trimIndent()

            val response = withTimeout(60_000L) {
                nlpModel.generateContent(
                    content {
                        image(bitmap)
                        text(promptText)
                    }
                )
            }

            val jsonText = response.text ?: throw IllegalStateException("Struk tidak dapat dibaca.")
            val json = JSONObject(jsonText)
            val draft = ParsedDraftTransaction(
                type = json.optString("type", "EXPENSE"),
                amount = json.optDouble("amount", 0.0),
                category = json.optString("category", "Belanja"),
                notes = json.optString("notes", "Belanja struk"),
                paymentMethod = json.optString("paymentMethod", "TUNAI"),
                tag = json.optString("tag", "Struk")
            )
            Result.success(draft)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Scan struk timeout", e)
            Result.failure(Exception("Scan struk melebihi batas waktu 60 detik."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca struk dengan AI", e)
            Result.failure(e)
        }
    }

    // AI Financial Advisor: Real financial diagnosis and recommendations
    suspend fun getFinancialAdvice(
        accounts: List<Account>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        userQuestion: String?
    ): Result<String> {
        return try {
            val totalBalance = accounts.sumOf { it.balance }
            val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val topCategories = transactions.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .joinToString(", ") { "${it.first}: Rp${it.second.toLong()}" }

            val budgetSummary = budgets.joinToString("; ") { b ->
                val spent = transactions.filter { it.type == "EXPENSE" && it.category.equals(b.category, true) }
                    .sumOf { it.amount }
                "${b.category}: Batas Rp${b.amount.toLong()} (Terpakai Rp${spent.toLong()})"
            }

            val prompt = """
                Anda adalah Asisten Keuangan Pribadi profesional dan ramah untuk aplikasi "Dompet Pintar".
                Data keuangan aktual pengguna:
                - Total Saldo: Rp${totalBalance.toLong()}
                - Total Pemasukan: Rp${totalIncome.toLong()}
                - Total Pengeluaran: Rp${totalExpense.toLong()}
                - Arus Kas Bersih: Rp${(totalIncome - totalExpense).toLong()}
                - Kategori Pengeluaran Terbesar: $topCategories
                - Anggaran Kategori: $budgetSummary

                Pertanyaan / Permintaan Pengguna:
                ${userQuestion ?: "Berikan analisis kesehatan keuangan saya bulan ini, evaluasi apakah ada pengeluaran boros, dan 3 saran penghematan konkret yang realistis."}

                Berikan jawaban terstruktur dalam bahasa Indonesia yang ramah, sopan, bernas, dan memotivasi dengan poin-poin jelas tanpa bertele-tele.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                advisorModel.generateContent(prompt)
            }

            val text = response.text ?: "Tidak ada saran yang dapat dibuat saat ini."
            Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Asisten AI memerlukan waktu lebih lama. Silakan coba lagi."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
