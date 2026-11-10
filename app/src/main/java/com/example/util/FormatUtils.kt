package com.example.util

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FormatUtils {
    private val localeId = Locale("id", "ID")
    private val timeZoneJakarta = TimeZone.getTimeZone("Asia/Jakarta")

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(localeId)
        format.maximumFractionDigits = 0
        val formatted = format.format(amount)
        // Adjust standard output "Rp 1.000.000" to "Rp1.000.000" per prompt requirement
        return formatted.replace("Rp ", "Rp").replace("Rp", "Rp")
    }

    fun formatIndonesianDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", localeId)
        sdf.timeZone = timeZoneJakarta
        return sdf.format(timestamp.toDate())
    }

    fun formatShortDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy", localeId)
        sdf.timeZone = timeZoneJakarta
        return sdf.format(timestamp.toDate())
    }

    fun formatDayMonth(timestamp: Timestamp?): String {
        if (timestamp == null) return "-"
        val sdf = SimpleDateFormat("d MMM", localeId)
        sdf.timeZone = timeZoneJakarta
        return sdf.format(timestamp.toDate())
    }

    fun currentMonthString(): String {
        val sdf = SimpleDateFormat("yyyy-MM", localeId)
        sdf.timeZone = timeZoneJakarta
        return sdf.format(Date())
    }

    fun formatMonthString(yearMonth: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", localeId)
            val date = parser.parse(yearMonth) ?: return yearMonth
            val formatter = SimpleDateFormat("MMMM yyyy", localeId)
            formatter.format(date)
        } catch (e: Exception) {
            yearMonth
        }
    }
}
