package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.ai.ParsedDraftTransaction
import com.example.data.model.Transaction
import com.example.data.repository.FirestoreRepository
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddEditBillDialog
import com.example.ui.components.AddEditBudgetDialog
import com.example.ui.components.AddEditDebtDialog
import com.example.ui.components.AddEditGoalDialog
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.QuickAiInputDialog
import com.example.ui.components.TransferDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SmartFeaturesScreen
import com.example.ui.screens.TransactionListScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.DompetPintarTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class NavDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Beranda", Icons.Default.Home),
    TRANSACTIONS("Transaksi", Icons.Default.Receipt),
    BUDGETS("Anggaran", Icons.Default.PieChart),
    REPORTS("Laporan", Icons.Default.BarChart),
    SMART("Pintar", Icons.Default.AutoAwesome),
    PROFILE("Profil", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure App Check before any UI or Firebase operations
        configureAppCheck(this, intent)

        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(context) }

            if (currentUser == null) {
                DompetPintarTheme(darkTheme = true) {
                    AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
                }
            } else {
                val repository = remember { FirestoreRepository(context) }
                val viewModel = remember { FinanceViewModel(repository) }
                val isDarkMode by viewModel.isDarkMode.collectAsState()

                DompetPintarTheme(darkTheme = isDarkMode) {
                    MainApp(
                        viewModel = viewModel,
                        onSignOut = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = { currentUser = null },
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: FinanceViewModel,
    onSignOut: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val accounts by viewModel.accounts.collectAsState()

    // Dialog state holders
    var showAddTxDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var activeDraftTx by remember { mutableStateOf<ParsedDraftTransaction?>(null) }

    var showQuickAiDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    val isNlpLoading by viewModel.isNlpLoading.collectAsState()

    // Handle back button when inside a secondary screen to return to HOME
    if (currentDestination != NavDestination.HOME) {
        BackHandler {
            currentDestination = NavDestination.HOME
        }
    }

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage!!)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                NavDestination.values().forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label, fontSize = 10.sp) },
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                NavDestination.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateTransactions = { currentDestination = NavDestination.TRANSACTIONS },
                    onNavigateBudgets = { currentDestination = NavDestination.BUDGETS },
                    onNavigateSmartFeatures = { currentDestination = NavDestination.SMART },
                    onAddTransaction = {
                        editingTransaction = null
                        activeDraftTx = null
                        showAddTxDialog = true
                    },
                    onQuickAiText = { showQuickAiDialog = true },
                    onScanReceipt = {
                        // Quick scan simulation / open text input
                        showQuickAiDialog = true
                    },
                    onTransfer = { showTransferDialog = true },
                    onAddAccount = { showAddAccountDialog = true },
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                        activeDraftTx = null
                        showAddTxDialog = true
                    }
                )

                NavDestination.TRANSACTIONS -> TransactionListScreen(
                    viewModel = viewModel,
                    onAddTransaction = {
                        editingTransaction = null
                        activeDraftTx = null
                        showAddTxDialog = true
                    },
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                        activeDraftTx = null
                        showAddTxDialog = true
                    }
                )

                NavDestination.BUDGETS -> BudgetScreen(
                    viewModel = viewModel,
                    onAddBudget = { showAddBudgetDialog = true }
                )

                NavDestination.REPORTS -> ReportScreen(
                    viewModel = viewModel
                )

                NavDestination.SMART -> SmartFeaturesScreen(
                    viewModel = viewModel,
                    onAddBill = { showAddBillDialog = true },
                    onAddDebt = { showAddDebtDialog = true },
                    onAddGoal = { showAddGoalDialog = true }
                )

                NavDestination.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    onSignOut = onSignOut
                )
            }
        }
    }

    // --- DIALOGS ---

    if (showAddTxDialog) {
        AddEditTransactionDialog(
            initialTransaction = editingTransaction,
            draftData = activeDraftTx,
            accounts = accounts,
            onDismiss = {
                showAddTxDialog = false
                editingTransaction = null
                activeDraftTx = null
            },
            onSave = { tx ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(editingTransaction!!, tx)
                } else {
                    viewModel.addTransaction(tx)
                }
                showAddTxDialog = false
                editingTransaction = null
                activeDraftTx = null
            }
        )
    }

    if (showQuickAiDialog) {
        QuickAiInputDialog(
            isLoading = isNlpLoading,
            onDismiss = { showQuickAiDialog = false },
            onSubmitText = { text ->
                viewModel.parseNaturalLanguageTransaction(text) { draft ->
                    showQuickAiDialog = false
                    if (draft != null) {
                        activeDraftTx = draft
                        editingTransaction = null
                        showAddTxDialog = true
                    }
                }
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromId, toId, amount, notes ->
                viewModel.addTransaction(
                    Transaction(
                        type = "TRANSFER",
                        amount = amount,
                        category = "Transfer",
                        accountId = fromId,
                        toAccountId = toId,
                        notes = if (notes.isNotBlank()) notes else "Transfer antar rekening",
                        date = com.google.firebase.Timestamp.now()
                    )
                )
                showTransferDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { acc ->
                viewModel.addAccount(acc)
                showAddAccountDialog = false
            }
        )
    }

    if (showAddBudgetDialog) {
        AddEditBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onSave = { b ->
                viewModel.saveBudget(b)
                showAddBudgetDialog = false
            }
        )
    }

    if (showAddBillDialog) {
        AddEditBillDialog(
            onDismiss = { showAddBillDialog = false },
            onSave = { bill ->
                viewModel.saveBill(bill)
                showAddBillDialog = false
            }
        )
    }

    if (showAddDebtDialog) {
        AddEditDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { debt ->
                viewModel.saveDebt(debt)
                showAddDebtDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddEditGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { goal ->
                viewModel.saveGoal(goal)
                showAddGoalDialog = false
            }
        )
    }
}
