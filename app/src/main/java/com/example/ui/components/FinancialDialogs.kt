package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ParsedDraftTransaction
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Bill
import com.example.data.model.Budget
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.PaymentMethod
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.util.FormatUtils
import com.google.firebase.Timestamp

private val expenseCategories = listOf(
    "Makanan", "Transportasi", "Belanja", "Tagihan", "Listrik", "Air", "Internet",
    "Tempat Tinggal", "Pendidikan", "Kesehatan", "Hiburan", "Cicilan", "Sedekah", "Lainnya"
)

private val incomeCategories = listOf(
    "Gaji", "Bisnis", "Penjualan", "Bonus", "Investasi", "Hadiah", "Freelance", "Lainnya"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionDialog(
    initialTransaction: Transaction? = null,
    draftData: ParsedDraftTransaction? = null,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var type by remember {
        mutableStateOf(draftData?.type ?: initialTransaction?.type ?: TransactionType.EXPENSE.name)
    }
    var amountText by remember {
        val initialAmount = draftData?.amount ?: initialTransaction?.amount
        mutableStateOf(if (initialAmount != null && initialAmount > 0) initialAmount.toLong().toString() else "")
    }
    var category by remember {
        mutableStateOf(
            draftData?.category ?: initialTransaction?.category ?: if (type == TransactionType.INCOME.name) "Gaji" else "Makanan"
        )
    }
    var selectedAccountId by remember {
        mutableStateOf(
            initialTransaction?.accountId ?: accounts.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedToAccountId by remember {
        mutableStateOf(
            initialTransaction?.toAccountId ?: accounts.getOrNull(1)?.id.orEmpty()
        )
    }
    var paymentMethod by remember {
        mutableStateOf(draftData?.paymentMethod ?: initialTransaction?.paymentMethod ?: PaymentMethod.TUNAI.name)
    }
    var notes by remember {
        mutableStateOf(draftData?.notes ?: initialTransaction?.notes.orEmpty())
    }
    var tag by remember {
        mutableStateOf(draftData?.tag ?: initialTransaction?.tag.orEmpty())
    }
    var isRecurring by remember {
        mutableStateOf(initialTransaction?.isRecurring ?: false)
    }

    val isTransfer = type == TransactionType.TRANSFER.name
    val activeCategories = if (type == TransactionType.INCOME.name) incomeCategories else expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTransaction == null) "Tambah Transaksi" else "Edit Transaksi",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Type Selector (Income, Expense, Transfer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeTabButton(
                        label = "Pengeluaran",
                        selected = type == TransactionType.EXPENSE.name,
                        selectedColor = Color(0xFFEF4444),
                        onClick = {
                            type = TransactionType.EXPENSE.name
                            category = "Makanan"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TypeTabButton(
                        label = "Pemasukan",
                        selected = type == TransactionType.INCOME.name,
                        selectedColor = Color(0xFF10B981),
                        onClick = {
                            type = TransactionType.INCOME.name
                            category = "Gaji"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TypeTabButton(
                        label = "Transfer",
                        selected = type == TransactionType.TRANSFER.name,
                        selectedColor = Color(0xFF3B82F6),
                        onClick = {
                            type = TransactionType.TRANSFER.name
                            category = "Transfer"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Nominal Uang (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_amount"),
                    singleLine = true
                )

                if (amountText.isNotBlank()) {
                    val previewVal = amountText.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "Terbaca: ${FormatUtils.formatRupiah(previewVal)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips (only for non-transfer)
                if (!isTransfer) {
                    Text(
                        text = "Pilih Kategori",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        activeCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Account Selection
                Text(
                    text = if (isTransfer) "Rekening Asal" else "Rekening / Dompet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${acc.name} (${FormatUtils.formatRupiah(acc.balance)})", fontSize = 12.sp) }
                        )
                    }
                }

                if (isTransfer) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Rekening Tujuan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            FilterChip(
                                selected = selectedToAccountId == acc.id,
                                onClick = { selectedToAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method
                Text(
                    text = "Metode Pembayaran",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.values().forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method.name,
                            onClick = { paymentMethod = method.name },
                            label = { Text(method.label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes & Tag
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Keterangan") },
                    placeholder = { Text("Contoh: Makan siang warteg, Beli susu anak") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Label / Tag (Opsional)") },
                    placeholder = { Text("Contoh: Kantor, Liburan, Pribadi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recurring Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaksi Berulang (Bulanan)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && selectedAccountId.isNotBlank()) {
                        val finalTx = (initialTransaction ?: Transaction()).copy(
                            type = type,
                            amount = amount,
                            category = if (isTransfer) "Transfer" else category,
                            accountId = selectedAccountId,
                            toAccountId = if (isTransfer) selectedToAccountId else "",
                            paymentMethod = paymentMethod,
                            notes = notes,
                            tag = tag,
                            isRecurring = isRecurring,
                            recurringFrequency = if (isRecurring) "MONTHLY" else "NONE",
                            date = initialTransaction?.date ?: Timestamp.now()
                        )
                        onSave(finalTx)
                    }
                },
                enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true && selectedAccountId.isNotBlank(),
                modifier = Modifier.testTag("btn_save_tx")
            ) {
                Text("Simpan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun QuickAiInputDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmitText: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Catat Otomatis via AI", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Ketik transaksi Anda dalam bahasa sehari-hari. AI akan otomatis mendeteksi nominal, kategori, jenis, dan tanggal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Contoh: Tadi siang beli makan nasi padang 25.000 bayar pakai gopay") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("ai_natural_text_input"),
                    enabled = !isLoading
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI sedang menganalisis teks...", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitText(textInput) },
                enabled = textInput.isNotBlank() && !isLoading,
                modifier = Modifier.testTag("btn_submit_ai_text")
            ) {
                Text("Proses AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onTransfer: (fromId: String, toId: String, amount: Double, notes: String) -> Unit
) {
    var fromAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var toAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id.orEmpty()) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val fromAccount = accounts.find { it.id == fromAccountId }
    val amountVal = amountText.toDoubleOrNull() ?: 0.0
    val isBalanceSufficient = fromAccount != null && amountVal <= fromAccount.balance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Antar Rekening", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Rekening Asal", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                accounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fromAccountId = acc.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (fromAccountId == acc.id) "● " else "○ ",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("${acc.name} (Saldo: ${FormatUtils.formatRupiah(acc.balance)})", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Rekening Tujuan", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                accounts.filter { it.id != fromAccountId }.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toAccountId = acc.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (toAccountId == acc.id) "● " else "○ ",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(acc.name, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Nominal Transfer (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (fromAccount != null && amountVal > fromAccount.balance) {
                    Text(
                        text = "Saldo rekening asal tidak mencukupi!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan Transfer (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountVal > 0 && isBalanceSufficient && toAccountId.isNotBlank()) {
                        onTransfer(fromAccountId, toAccountId, amountVal, notes)
                    }
                },
                enabled = amountVal > 0 && isBalanceSufficient && toAccountId.isNotBlank()
            ) {
                Text("Kirim Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK.name) }
    var balanceText by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#10B981") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Rekening Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Rekening / Dompet") },
                    placeholder = { Text("Contoh: BCA Utama, GoPay, Dompet Tunai") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Jenis Rekening", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AccountType.values().take(3).forEach { t ->
                        FilterChip(
                            selected = type == t.name,
                            onClick = { type = t.name },
                            label = { Text(t.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) balanceText = it },
                    label = { Text("Saldo Awal (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Nomor Rekening (Opsional)") },
                    placeholder = { Text("Contoh: 1234567890") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSave(
                            Account(
                                name = name,
                                type = type,
                                balance = bal,
                                initialBalance = bal,
                                accountNumber = accountNumber,
                                colorHex = colorHex
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan Rekening")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AddEditBudgetDialog(
    initialBudget: Budget? = null,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    var category by remember { mutableStateOf(initialBudget?.category ?: expenseCategories.first()) }
    var amountText by remember { mutableStateOf(if (initialBudget != null) initialBudget.amount.toLong().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBudget == null) "Tetapkan Anggaran Bulanan" else "Edit Anggaran", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Kategori Pengeluaran", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.height(140.dp).verticalScroll(rememberScrollState())) {
                    expenseCategories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { category = cat }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (category == cat) "● " else "○ ", color = MaterialTheme.colorScheme.primary)
                            Text(cat, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Batas Anggaran Bulanan (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(
                            (initialBudget ?: Budget()).copy(
                                category = category,
                                amount = amt,
                                month = FormatUtils.currentMonthString()
                            )
                        )
                    }
                },
                enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Simpan Anggaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AddEditBillDialog(
    initialBill: Bill? = null,
    onDismiss: () -> Unit,
    onSave: (Bill) -> Unit
) {
    var name by remember { mutableStateOf(initialBill?.name.orEmpty()) }
    var amountText by remember { mutableStateOf(if (initialBill != null) initialBill.amount.toLong().toString() else "") }
    var category by remember { mutableStateOf(initialBill?.category ?: "Tagihan") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBill == null) "Tambah Tagihan Rutin" else "Edit Tagihan", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Tagihan") },
                    placeholder = { Text("Contoh: Listrik PLN, WiFi Indihome, Cicilan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Nominal Tagihan (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0) {
                        onSave(
                            (initialBill ?: Bill()).copy(
                                name = name,
                                amount = amt,
                                category = category,
                                dueDate = Timestamp.now()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && amountText.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Simpan Tagihan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AddEditDebtDialog(
    initialDebt: Debt? = null,
    onDismiss: () -> Unit,
    onSave: (Debt) -> Unit
) {
    var type by remember { mutableStateOf(initialDebt?.type ?: "DEBT") }
    var personName by remember { mutableStateOf(initialDebt?.personName.orEmpty()) }
    var amountText by remember { mutableStateOf(if (initialDebt != null) initialDebt.amount.toLong().toString() else "") }
    var notes by remember { mutableStateOf(initialDebt?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialDebt == null) "Catat Utang / Piutang" else "Edit Catatan", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeTabButton(
                        label = "Utang Saya",
                        selected = type == "DEBT",
                        selectedColor = Color(0xFFEF4444),
                        onClick = { type = "DEBT" },
                        modifier = Modifier.weight(1f)
                    )
                    TypeTabButton(
                        label = "Piutang Orang",
                        selected = type == "RECEIVABLE",
                        selectedColor = Color(0xFF10B981),
                        onClick = { type = "RECEIVABLE" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(if (type == "DEBT") "Berutang Kepada" else "Dipinjam Oleh") },
                    placeholder = { Text("Contoh: Budi Santoso") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Nominal Pokok (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan") },
                    placeholder = { Text("Contoh: Pinjaman modal usaha, DP motor") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (personName.isNotBlank() && amt > 0) {
                        onSave(
                            (initialDebt ?: Debt()).copy(
                                type = type,
                                personName = personName,
                                amount = amt,
                                notes = notes,
                                dueDate = Timestamp.now()
                            )
                        )
                    }
                },
                enabled = personName.isNotBlank() && amountText.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AddEditGoalDialog(
    initialGoal: Goal? = null,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title.orEmpty()) }
    var targetText by remember { mutableStateOf(if (initialGoal != null) initialGoal.targetAmount.toLong().toString() else "") }
    var currentText by remember { mutableStateOf(if (initialGoal != null) initialGoal.currentAmount.toLong().toString() else "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGoal == null) "Target Tabungan Baru" else "Edit Target", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Target Tabungan") },
                    placeholder = { Text("Contoh: Beli Laptop Baru, Dana Darurat") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetText = it },
                    label = { Text("Target Dana (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) currentText = it },
                    label = { Text("Dana Terkumpul Awal (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tgt = targetText.toDoubleOrNull() ?: 0.0
                    val cur = currentText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && tgt > 0) {
                        onSave(
                            (initialGoal ?: Goal()).copy(
                                title = title,
                                targetAmount = tgt,
                                currentAmount = cur,
                                targetDate = Timestamp.now()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && targetText.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Simpan Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun TypeTabButton(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
