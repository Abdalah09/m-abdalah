package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CashFlowGreen
import com.example.ui.theme.CashFlowRed
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.PeriodFilter
import com.example.util.ExportUtils
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary by viewModel.summary.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val periodFilter by viewModel.periodFilter.collectAsState()

    // Calculate top expense categories
    val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
    val categoryTotals = expenseTransactions
        .groupBy { it.category }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Day with highest expense
    val dayTotals = expenseTransactions
        .groupBy { tx ->
            if (tx.date != null) {
                val sdf = SimpleDateFormat("EEEE, dd MMM", Locale("id", "ID"))
                sdf.format(tx.date.toDate())
            } else "Lainnya"
        }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
        .maxByOrNull { it.value }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with CSV Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Laporan Keuangan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Analisis Pemasukan, Pengeluaran & Arus Kas",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = {
                        val csv = ExportUtils.exportTransactionsToCsv(transactions)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Ekspor Laporan CSV"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_export_csv")
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor CSV", fontSize = 11.sp)
                }
            }
        }

        // Period Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PeriodFilter.values()) { filter ->
                    FilterChip(
                        selected = periodFilter == filter,
                        onClick = { viewModel.setPeriodFilter(filter) },
                        label = { Text(filter.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Cash Flow Comparison Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Perbandingan Pemasukan & Pengeluaran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Incomes vs Expense Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = CashFlowGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pemasukan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = FormatUtils.formatRupiah(summary.totalIncome),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CashFlowGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = CashFlowRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pengeluaran", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = FormatUtils.formatRupiah(summary.totalExpense),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CashFlowRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val totalFlow = (summary.totalIncome + summary.totalExpense).coerceAtLeast(1.0)
                    val incomeShare = (summary.totalIncome / totalFlow).toFloat().coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { incomeShare },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = CashFlowGreen,
                        trackColor = CashFlowRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(
                            label = "Arus Kas Bersih",
                            value = (if (summary.netCashFlow >= 0) "+" else "") + FormatUtils.formatRupiah(summary.netCashFlow),
                            color = if (summary.netCashFlow >= 0) CashFlowGreen else CashFlowRed
                        )
                        MetricItem(
                            label = "Rasio Tabungan",
                            value = "${summary.savingsRate.toInt()}%",
                            color = MaterialTheme.colorScheme.primary
                        )
                        MetricItem(
                            label = "Rasio Beban",
                            value = "${summary.expenseRatio.toInt()}%",
                            color = if (summary.expenseRatio > 80) CashFlowRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Peak Expense Day Insight
        if (dayTotals != null && dayTotals.value > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hari Pengeluaran Tertinggi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "${dayTotals.key} dengan total ${FormatUtils.formatRupiah(dayTotals.value)}",
                                fontSize = 12.sp,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }
        }

        // Top Expense Categories Section
        item {
            Text(
                text = "Kategori Pengeluaran Terbesar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (categoryTotals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada data pengeluaran pada periode ini.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    }
                }
            }
        } else {
            val maxCatValue = categoryTotals.first().second.coerceAtLeast(1.0)
            items(categoryTotals.take(6)) { (cat, total) ->
                val percentage = if (summary.totalExpense > 0) ((total / summary.totalExpense) * 100).toInt() else 0
                val barProgress = (total / maxCatValue).toFloat().coerceIn(0f, 1f)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = "${FormatUtils.formatRupiah(total)} ($percentage%)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
