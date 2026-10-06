package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PinLockScreen
import com.example.ui.components.QuickAddSheet
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.debts.DebtsScreen
import com.example.ui.screens.goals.GoalsScreen
import com.example.ui.screens.networth.NetWorthScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.reminders.RemindersScreen
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

enum class NavigationDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Dashboard),
    TRANSACTIONS("Ledger", Icons.Default.ReceiptLong),
    ACCOUNTS("Vaults", Icons.Default.AccountBalanceWallet),
    DEBTS("Debts", Icons.Default.PeopleAlt),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    NET_WORTH("Net Worth", Icons.Default.TrendingUp),
    GOALS("Goals", Icons.Default.Savings),
    REMINDERS("Reminders", Icons.Default.Alarm),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun MainFinanceApp(viewModel: FinanceViewModel) {
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(NavigationDestination.DASHBOARD) }
    var showQuickAddSheet by remember { mutableStateOf(false) }

    if (isLocked) {
        PinLockScreen(
            onUnlock = { pin -> viewModel.unlockWithPin(pin) },
            onBiometricUnlock = { viewModel.unlockWithPin(viewModel.userProfile.value?.pinCode ?: "1234") }
        )
        return
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddSheet = true },
                containerColor = CyanNeon,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("main_quick_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.5.dp,
                        color = DarkCardBorder,
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
            ) {
                val primaryNavItems = listOf(
                    NavigationDestination.DASHBOARD,
                    NavigationDestination.TRANSACTIONS,
                    NavigationDestination.ACCOUNTS,
                    NavigationDestination.DEBTS,
                    NavigationDestination.ANALYTICS,
                    NavigationDestination.PROFILE
                )

                primaryNavItems.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanNeon,
                            selectedTextColor = CyanNeon,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
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
                NavigationDestination.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = { currentDestination = NavigationDestination.TRANSACTIONS },
                        onNavigateToAccounts = { currentDestination = NavigationDestination.ACCOUNTS },
                        onNavigateToDebts = { currentDestination = NavigationDestination.DEBTS },
                        onNavigateToAnalytics = { currentDestination = NavigationDestination.ANALYTICS },
                        onNavigateToNetWorth = { currentDestination = NavigationDestination.NET_WORTH },
                        onNavigateToProfile = { currentDestination = NavigationDestination.PROFILE }
                    )
                }
                NavigationDestination.TRANSACTIONS -> {
                    TransactionsScreen(viewModel = viewModel)
                }
                NavigationDestination.ACCOUNTS -> {
                    AccountsScreen(viewModel = viewModel)
                }
                NavigationDestination.DEBTS -> {
                    DebtsScreen(viewModel = viewModel)
                }
                NavigationDestination.ANALYTICS -> {
                    AnalyticsScreen(viewModel = viewModel)
                }
                NavigationDestination.NET_WORTH -> {
                    NetWorthScreen(viewModel = viewModel)
                }
                NavigationDestination.GOALS -> {
                    GoalsScreen(viewModel = viewModel)
                }
                NavigationDestination.REMINDERS -> {
                    RemindersScreen(viewModel = viewModel)
                }
                NavigationDestination.PROFILE -> {
                    ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showQuickAddSheet) {
        QuickAddSheet(
            viewModel = viewModel,
            accounts = accounts,
            onDismiss = { showQuickAddSheet = false }
        )
    }
}
