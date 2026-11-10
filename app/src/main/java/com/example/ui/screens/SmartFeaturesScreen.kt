package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.FormatUtils

@Composable
fun SmartFeaturesScreen(
    viewModel: FinanceViewModel,
    onAddBill: () -> Unit,
    onAddDebt: () -> Unit,
    onAddGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Asisten AI", "Tagihan", "Utang/Piutang", "Target Tabungan")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Screen Header
        Text(
            text = "Fitur Pintar Finansial",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> AiAdvisorTab(viewModel = viewModel)
            1 -> BillsTab(viewModel = viewModel, onAddBill = onAddBill)
            2 -> DebtsTab(viewModel = viewModel, onAddDebt = onAddDebt)
            3 -> GoalsTab(viewModel = viewModel, onAddGoal = onAddGoal)
        }
    }
}

// 1. Asisten Keuangan AI Tab
@Composable
private fun AiAdvisorTab(viewModel: FinanceViewModel) {
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var userPrompt by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Asisten Keuangan Cerdas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Dapatkan diagnosis kesehatan keuangan, deteksi kebocoran anggaran, dan tips berhemat langsung berdasarkan data transaksi riil Anda.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Quick prompts
        item {
            Text("Pertanyaan Cepat:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.askFinancialAdvisor("Bagaimana kondisi kesehatan keuangan dan rasio tabungan saya bulan ini?") },
                    label = { Text("Kesehatan Keuangan", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.askFinancialAdvisor("Berikan 3 cara menghemat pengeluaran terbesar saya bulan ini.") },
                    label = { Text("Tips Berhemat", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Input prompt
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = { Text("Tanyakan apa saja tentang keuangan Anda...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_advisor_prompt_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    enabled = !isAiLoading
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (userPrompt.isNotBlank()) {
                            viewModel.askFinancialAdvisor(userPrompt)
                            userPrompt = ""
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    enabled = userPrompt.isNotBlank() && !isAiLoading,
                    modifier = Modifier.testTag("btn_send_ai_advisor")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Kirim", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Response or Loading
        if (isAiLoading) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI sedang menganalisis transaksi Anda...", fontSize = 13.sp)
                    }
                }
            }
        } else if (aiAdvice != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saran Asisten Finansial", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiAdvice!!,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// 2. Pengingat Tagihan Tab
@Composable
private fun BillsTab(viewModel: FinanceViewModel, onAddBill: () -> Unit) {
    val bills by viewModel.bills.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar Tagihan Rutin (${bills.size})", fontWeight = FontWeight.Bold)
                Button(onClick = onAddBill, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Tagihan", fontSize = 11.sp)
                }
            }
        }

        if (bills.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada tagihan dicatat. Tambahkan tagihan listrik, internet, atau cicilan Anda.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(bills) { bill ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleBillPaid(bill) }) {
                            Icon(
                                imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Status Bayar",
                                tint = if (bill.isPaid) Color(0xFF10B981) else Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bill.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Jatuh tempo: ${FormatUtils.formatShortDate(bill.dueDate)} • ${bill.category}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatRupiah(bill.amount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (bill.isPaid) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            IconButton(onClick = { viewModel.deleteBill(bill.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Utang dan Piutang Tab
@Composable
private fun DebtsTab(viewModel: FinanceViewModel, onAddDebt: () -> Unit) {
    val debts by viewModel.debts.collectAsState()
    var debtForPayment by remember { mutableStateOf<Debt?>(null) }
    var paymentInput by remember { mutableStateOf("") }

    if (debtForPayment != null) {
        val debt = debtForPayment!!
        AlertDialog(
            onDismissRequest = { debtForPayment = null },
            title = { Text("Catat Cicilan / Pembayaran", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pihak: ${debt.personName}")
                    Text("Total Pokok: ${FormatUtils.formatRupiah(debt.amount)}")
                    Text("Sisa Belum Dibayar: ${FormatUtils.formatRupiah(debt.amount - debt.paidAmount)}")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = paymentInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) paymentInput = it },
                        label = { Text("Nominal Pembayaran (Rp)") },
                        prefix = { Text("Rp ") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.addDebtPayment(debt, amt)
                            debtForPayment = null
                            paymentInput = ""
                        }
                    }
                ) { Text("Bayar") }
            },
            dismissButton = { TextButton(onClick = { debtForPayment = null }) { Text("Batal") } }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Buku Utang & Piutang", fontWeight = FontWeight.Bold)
                Button(onClick = onAddDebt, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Baru", fontSize = 11.sp)
                }
            }
        }

        if (debts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada catatan utang atau piutang.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(debts) { debt ->
                val isPayable = debt.type == "DEBT"
                val remaining = (debt.amount - debt.paidAmount).coerceAtLeast(0.0)
                val progress = if (debt.amount > 0) (debt.paidAmount / debt.amount).toFloat().coerceIn(0f, 1f) else 0f

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPayable) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                                ) {
                                    Text(
                                        text = if (isPayable) "Utang Saya" else "Piutang Saya",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPayable) Color(0xFFDC2626) else Color(0xFF059669),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(debt.personName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (debt.isSettled) {
                                Text("Lunas", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Button(
                                    onClick = { debtForPayment = debt },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Bayar Cicilan", fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total: ${FormatUtils.formatRupiah(debt.amount)}", fontSize = 12.sp)
                            Text("Sisa: ${FormatUtils.formatRupiah(remaining)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isPayable) Color(0xFF3B82F6) else Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

// 4. Target Tabungan (Celengan Impian) Tab
@Composable
private fun GoalsTab(viewModel: FinanceViewModel, onAddGoal: () -> Unit) {
    val goals by viewModel.goals.collectAsState()
    var goalForDeposit by remember { mutableStateOf<Goal?>(null) }
    var depositInput by remember { mutableStateOf("") }

    if (goalForDeposit != null) {
        val goal = goalForDeposit!!
        AlertDialog(
            onDismissRequest = { goalForDeposit = null },
            title = { Text("Tambah Setoran Tabungan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Target: ${goal.title}")
                    Text("Terkumpul: ${FormatUtils.formatRupiah(goal.currentAmount)}")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = depositInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) depositInput = it },
                        label = { Text("Nominal Setoran (Rp)") },
                        prefix = { Text("Rp ") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depositInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.addGoalDeposit(goal, amt)
                            goalForDeposit = null
                            depositInput = ""
                        }
                    }
                ) { Text("Setor") }
            },
            dismissButton = { TextButton(onClick = { goalForDeposit = null }) { Text("Batal") } }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Celengan & Target Tabungan", fontWeight = FontWeight.Bold)
                Button(onClick = onAddGoal, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buat Target", fontSize = 11.sp)
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada target tabungan dibuat. Buat target seperti liburan, laptop baru, atau dana darurat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(goals) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val sisa = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD1FAE5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Target: ${FormatUtils.formatRupiah(goal.targetAmount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Button(
                                onClick = { goalForDeposit = goal },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Nabung", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terkumpul: ${FormatUtils.formatRupiah(goal.currentAmount)} (${(progress * 100).toInt()}%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Sisa: ${FormatUtils.formatRupiah(sisa)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
