package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ElectricityBill
import com.example.data.repository.BijliRepository
import com.example.notifications.ReminderScheduler
import com.example.ui.components.AddBillSheet
import com.example.ui.components.AddConnectionSheet
import com.example.ui.components.AddReadingSheet
import com.example.ui.components.AddReminderSheet
import com.example.ui.components.PaymentBottomSheet
import com.example.ui.components.QuickPayJvvnlSheet
import com.example.ui.components.ReceiptModalSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.UsageScreen
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.BijliDashboardData
import com.example.ui.viewmodel.BijliViewModel
import com.example.ui.viewmodel.UiState

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_HISTORY = "history"
const val ROUTE_USAGE = "usage"
const val ROUTE_REMINDERS = "reminders"

private enum class BijliTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(ROUTE_DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY(ROUTE_HISTORY, "History", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    USAGE(ROUTE_USAGE, "Usage", Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    REMINDERS(ROUTE_REMINDERS, "Reminders", Icons.Filled.NotificationsActive, Icons.Outlined.Notifications)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current

    val viewModel: BijliViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                BijliViewModel(BijliRepository(app.applicationContext))
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val historyFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedUsageConnId by viewModel.selectedConnectionIdForUsage.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    var showQuickPayJvvnl by remember { mutableStateOf(false) }
    var showAddConnection by remember { mutableStateOf(false) }
    var showAddBill by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    var showAddReading by remember { mutableStateOf(false) }
    var billToPay by remember { mutableStateOf<ElectricityBill?>(null) }
    var billForReceipt by remember { mutableStateOf<ElectricityBill?>(null) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MidnightTeal,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = SolarAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "BijliPay",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "JVVNL • Jaipur Discom",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        BijliTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.size(12.dp))
                        BijliTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)

                    when (val state = dashboardState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        is UiState.Success<BijliDashboardData> -> {
                            val data = state.data
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_DASHBOARD,
                                modifier = contentModifier
                            ) {
                                composable(ROUTE_DASHBOARD) {
                                    DashboardScreen(
                                        data = data,
                                        onQuickPayJvvnlClick = { showQuickPayJvvnl = true },
                                        onAddConnectionClick = { showAddConnection = true },
                                        onAddBillClick = {
                                            if (data.connections.isEmpty()) showAddConnection = true
                                            else showAddBill = true
                                        },
                                        onAddReminderClick = { showAddReminder = true },
                                        onLogReadingClick = {
                                            if (data.connections.isEmpty()) showAddConnection = true
                                            else showAddReading = true
                                        },
                                        onPayBillClick = { bill ->
                                            viewModel.resetPaymentState()
                                            billToPay = bill
                                        },
                                        onViewReceiptClick = { bill -> billForReceipt = bill },
                                        onDeleteConnectionClick = { viewModel.deleteConnection(it) },
                                        onNavigateToUsage = { navigateToTab(ROUTE_USAGE) }
                                    )
                                }

                                composable(ROUTE_HISTORY) {
                                    HistoryScreen(
                                        data = data,
                                        selectedFilter = historyFilter,
                                        searchQuery = searchQuery,
                                        onFilterChange = viewModel::setHistoryFilter,
                                        onSearchChange = viewModel::setSearchQuery,
                                        onPayBillClick = { bill ->
                                            viewModel.resetPaymentState()
                                            billToPay = bill
                                        },
                                        onViewReceiptClick = { bill -> billForReceipt = bill }
                                    )
                                }

                                composable(ROUTE_USAGE) {
                                    UsageScreen(
                                        data = data,
                                        selectedConnectionId = selectedUsageConnId,
                                        onSelectConnection = viewModel::selectConnectionForUsage,
                                        onLogReadingClick = {
                                            if (data.connections.isEmpty()) showAddConnection = true
                                            else showAddReading = true
                                        },
                                        onUpdateTargetClick = viewModel::updateConnectionTarget,
                                        onDeleteReadingClick = viewModel::deleteUsageReading
                                    )
                                }

                                composable(ROUTE_REMINDERS) {
                                    RemindersScreen(
                                        data = data,
                                        onAddReminderClick = { showAddReminder = true },
                                        onToggleReminder = { reminder, enabled ->
                                            viewModel.toggleReminder(reminder, enabled) { updated ->
                                                if (updated.enabled) {
                                                    ReminderScheduler.scheduleReminder(context, updated)
                                                } else {
                                                    ReminderScheduler.cancelReminder(context, updated.id)
                                                }
                                            }
                                        },
                                        onDeleteReminder = { reminderId ->
                                            ReminderScheduler.cancelReminder(context, reminderId)
                                            viewModel.deleteReminder(reminderId)
                                        }
                                    )
                                }
                            }

                            if (showQuickPayJvvnl) {
                                QuickPayJvvnlSheet(
                                    connections = data.connections,
                                    onProceedToPay = { existingConn, circle, kNo, nick, month, units, amt, dueMillis ->
                                        viewModel.prepareQuickJvvnlBill(
                                            existingConnection = existingConn,
                                            circleName = circle,
                                            kNumber = kNo,
                                            nickname = nick,
                                            billingMonth = month,
                                            unitsConsumed = units,
                                            amount = amt,
                                            dueDateMillis = dueMillis
                                        ) { bill ->
                                            showQuickPayJvvnl = false
                                            viewModel.resetPaymentState()
                                            billToPay = bill
                                        }
                                    },
                                    onDismiss = { showQuickPayJvvnl = false }
                                )
                            }

                            if (showAddConnection) {
                                AddConnectionSheet(
                                    onSave = { provider, consumerNo, nick, type, rate, target, day ->
                                        viewModel.addConnection(provider, consumerNo, nick, type, rate, target, day) {
                                            showAddConnection = false
                                        }
                                    },
                                    onDismiss = { showAddConnection = false }
                                )
                            }

                            if (showAddBill) {
                                AddBillSheet(
                                    connections = data.connections,
                                    onSave = { conn, month, units, amount, lateFee, dueMillis ->
                                        viewModel.addBill(conn, month, units, amount, lateFee, dueMillis) {
                                            showAddBill = false
                                        }
                                    },
                                    onDismiss = { showAddBill = false }
                                )
                            }

                            if (showAddReminder) {
                                AddReminderSheet(
                                    pendingBills = data.pendingBills,
                                    onSave = { billId, title, consumerNo, amt, dueMillis, daysBefore, note ->
                                        viewModel.addReminder(billId, title, consumerNo, amt, dueMillis, daysBefore, note) { created ->
                                            ReminderScheduler.scheduleReminder(context, created)
                                            showAddReminder = false
                                        }
                                    },
                                    onDismiss = { showAddReminder = false }
                                )
                            }

                            if (showAddReading) {
                                val lastReading = data.usageReadings
                                    .maxByOrNull { it.yearMonth }
                                    ?.currentReading
                                AddReadingSheet(
                                    connections = data.connections,
                                    lastReadingForConnection = lastReading,
                                    onSave = { conn, label, ym, prev, curr, dateMillis, notes ->
                                        viewModel.addUsageReading(conn, label, ym, prev, curr, dateMillis, notes) {
                                            showAddReading = false
                                        }
                                    },
                                    onDismiss = { showAddReading = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activePayBill = billToPay
    if (activePayBill != null) {
        PaymentBottomSheet(
            bill = activePayBill,
            paymentState = paymentState,
            onPayConfirm = { methodLabel ->
                viewModel.payBillOnline(activePayBill, methodLabel)
            },
            onDismiss = {
                billToPay = null
                viewModel.resetPaymentState()
            }
        )
    }

    val receiptBill = billForReceipt
    if (receiptBill != null) {
        ReceiptModalSheet(
            bill = receiptBill,
            onDismiss = { billForReceipt = null }
        )
    }
}
