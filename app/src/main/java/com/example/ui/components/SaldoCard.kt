package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProfileType
import com.example.ui.theme.CashFlowGreen
import com.example.ui.theme.CashFlowRed
import com.example.ui.viewmodel.FinancialSummary
import com.example.util.FormatUtils

@Composable
fun SaldoCard(
    summary: FinancialSummary,
    activeProfile: ProfileType,
    onProfileSwitch: (ProfileType) -> Unit,
    onAddTransaction: () -> Unit,
    onQuickAiText: () -> Unit,
    onScanReceipt: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showForecastDetails by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("main_saldo_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F172A), // Deep navy
                                Color(0xFF064E3B), // Deep emerald
                                Color(0xFF047857)  // Emerald accent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Header: Profile Switcher & Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Saldo Tersedia",
                                color = Color(0xFFA7F3D0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatUtils.formatRupiah(summary.totalBalance),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.testTag("total_balance_text")
                            )
                        }

                        // Profile Toggle (Personal / Family)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.8f),
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                ProfilePill(
                                    label = "Pribadi",
                                    selected = activeProfile == ProfileType.PERSONAL,
                                    onClick = { onProfileSwitch(ProfileType.PERSONAL) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                ProfilePill(
                                    label = "Keluarga",
                                    selected = activeProfile == ProfileType.FAMILY,
                                    onClick = { onProfileSwitch(ProfileType.FAMILY) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Income & Expense Summary Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pemasukan
                        SummaryPill(
                            icon = Icons.Default.ArrowUpward,
                            iconColor = CashFlowGreen,
                            title = "Pemasukan",
                            amount = FormatUtils.formatRupiah(summary.totalIncome),
                            modifier = Modifier.weight(1f)
                        )

                        // Pengeluaran
                        SummaryPill(
                            icon = Icons.Default.ArrowDownward,
                            iconColor = CashFlowRed,
                            title = "Pengeluaran",
                            amount = FormatUtils.formatRupiah(summary.totalExpense),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Arus Kas Bersih & Estimasi Akhir Bulan Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { showForecastDetails = !showForecastDetails }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Arus Kas: ",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = (if (summary.netCashFlow >= 0) "+" else "") + FormatUtils.formatRupiah(summary.netCashFlow),
                                color = if (summary.netCashFlow >= 0) Color(0xFFA7F3D0) else Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rasio Simpan: ${summary.savingsRate.toInt()}%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Predictive End of Month Forecast
                    AnimatedVisibility(visible = showForecastDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "💡 Prediksi Saldo Akhir Bulan (Estimasi)",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Berdasarkan rata-rata harian dan tagihan: ${FormatUtils.formatRupiah(summary.predictedEndOfMonthBalance)}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Unusual Expense Anomaly Warning if detected
        if (summary.unusualExpenseWarning != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEF3C7),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Peringatan",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.unusualExpenseWarning,
                        color = Color(0xFF92400E),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Actions Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Catat",
                tag = "btn_add_transaction",
                onClick = onAddTransaction
            )
            QuickActionButton(
                icon = Icons.Default.AutoAwesome,
                label = "Teks AI",
                tag = "btn_quick_ai_text",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onQuickAiText
            )
            QuickActionButton(
                icon = Icons.Default.CameraAlt,
                label = "Scan Struk",
                tag = "btn_scan_receipt",
                onClick = onScanReceipt
            )
            QuickActionButton(
                icon = Icons.Default.SwapHoriz,
                label = "Transfer",
                tag = "btn_transfer_account",
                onClick = onTransfer
            )
        }
    }
}

@Composable
private fun ProfilePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF10B981) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SummaryPill(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
                Text(
                    text = amount,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tag: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
