package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.InsightType
import com.example.data.model.TransactionType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToNetWorth: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalAssets by viewModel.totalAssets.collectAsStateWithLifecycle()
    val totalLiabilities by viewModel.totalLiabilities.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val totalReceivable by viewModel.totalReceivable.collectAsStateWithLifecycle()
    val totalPayable by viewModel.totalPayable.collectAsStateWithLifecycle()
    val currentMonthStats by viewModel.currentMonthStats.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.currentMonthCategoryBreakdown.collectAsStateWithLifecycle()
    val cashFlowData by viewModel.last6MonthsCashFlow.collectAsStateWithLifecycle()
    val smartInsights by viewModel.smartInsights.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()

    val currencySymbol = profile?.currencySymbol ?: "₹"
    val userName = profile?.fullName?.split(" ")?.firstOrNull() ?: "Niyas"

    // Time-based greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Night Shift"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting, $userName",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Financial OS",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleMaskAmounts() },
                        modifier = Modifier.testTag("dashboard_mask_toggle")
                    ) {
                        Icon(
                            imageVector = if (isMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mask Balance",
                            tint = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.15f))
                            .border(1.dp, CyanNeon, CircleShape)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = CyanNeon,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Net Worth Master Card
        item {
            FuturisticCard(
                borderColor = CyanNeon,
                glowColor = CyanGlow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_worth_card"),
                onClick = onNavigateToNetWorth
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("NET WORTH", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMasked) "••••••••" else viewModel.formatCurrency(netWorth),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    StatBadge(text = "↑ 8.4% MoM", color = EmeraldNeon)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown line (Assets & Liabilities & Balance)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL ASSETS", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(totalAssets),
                            color = EmeraldNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("LIABILITIES", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(totalLiabilities),
                            color = RoseNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("TOTAL BALANCE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(totalBalance),
                            color = CyanNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Spline Chart
                NetWorthSplineChart(
                    points = listOf(420000f, 435000f, 442000f, 458000f, 472000f, 485250f),
                    height = 60.dp
                )
            }
        }

        // Metrics Grid (Income, Expense, Receivables, Payables)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Income this month",
                        value = viewModel.formatCurrency(currentMonthStats.income),
                        subtitle = "Recorded credits",
                        icon = Icons.Default.ArrowDownward,
                        iconTint = EmeraldNeon,
                        accentColor = EmeraldNeon,
                        isMasked = isMasked,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Expenses this month",
                        value = viewModel.formatCurrency(currentMonthStats.expenses),
                        subtitle = "Recorded debits",
                        icon = Icons.Default.ArrowUpward,
                        iconTint = RoseNeon,
                        accentColor = RoseNeon,
                        isMasked = isMasked,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Money owed to me",
                        value = viewModel.formatCurrency(totalReceivable),
                        subtitle = "Receivable from people",
                        icon = Icons.Default.CallReceived,
                        iconTint = CyanNeon,
                        accentColor = CyanNeon,
                        isMasked = isMasked,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDebts
                    )
                    MetricCard(
                        title = "Money I owe others",
                        value = viewModel.formatCurrency(totalPayable),
                        subtitle = "Payable debts",
                        icon = Icons.Default.CallMade,
                        iconTint = AmberNeon,
                        accentColor = AmberNeon,
                        isMasked = isMasked,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDebts
                    )
                }
            }
        }

        // Smart Financial Insights Carousel
        item {
            Column {
                SectionHeader(title = "Smart Financial Insights")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(smartInsights) { insight ->
                        val tint = when (insight.type) {
                            InsightType.POSITIVE -> EmeraldNeon
                            InsightType.WARNING -> AmberNeon
                            InsightType.ALERT -> RoseNeon
                            InsightType.INFO -> CyanNeon
                        }
                        FuturisticCard(
                            borderColor = tint,
                            modifier = Modifier.width(280.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tint)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = insight.title,
                                    color = tint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = insight.message,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Cash Flow Chart Section
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Cash Flow",
                    actionLabel = "Analytics",
                    onActionClick = onNavigateToAnalytics
                )
                Spacer(modifier = Modifier.height(8.dp))
                CashFlowChart(
                    data = cashFlowData,
                    currencySymbol = currencySymbol
                )
            }
        }

        // Spending Breakdown Section
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Spending Breakdown")
                Spacer(modifier = Modifier.height(8.dp))
                SpendingBreakdownBar(
                    breakdown = categoryBreakdown,
                    currencySymbol = currencySymbol
                )
            }
        }

        // Upcoming Payment Reminders
        item {
            val pendingReminders = reminders.filter { !it.isPaid }.take(3)
            Column {
                SectionHeader(
                    title = "Upcoming Payments",
                    actionLabel = if (pendingReminders.isNotEmpty()) "See All" else null,
                    onActionClick = onNavigateToTransactions
                )

                if (pendingReminders.isEmpty()) {
                    FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No pending payments. You're all settled up!",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingReminders.forEach { reminder ->
                            ReminderItemCard(
                                reminder = reminder,
                                currencySymbol = currencySymbol,
                                onMarkPaid = { viewModel.markReminderPaid(reminder, null) }
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Column {
                SectionHeader(
                    title = "Recent Transactions",
                    actionLabel = "View All",
                    onActionClick = onNavigateToTransactions
                )

                if (recentTransactions.isEmpty()) {
                    FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No recent transactions", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentTransactions.take(5).forEach { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                currencySymbol = currencySymbol,
                                isMasked = isMasked
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    currencySymbol: String,
    onMarkPaid: () -> Unit
) {
    val now = System.currentTimeMillis()
    val diffDays = ((reminder.dueDate - now) / 86400000L).toInt()

    val (statusLabel, statusColor) = when {
        diffDays < 0 -> "Overdue by ${-diffDays}d" to RoseNeon
        diffDays == 0 -> "Due Today" to AmberNeon
        else -> "Due in $diffDays d" to CyanNeon
    }

    FuturisticCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = statusColor.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${reminder.personOrCompany} • $statusLabel",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currencySymbol${reminder.amount.toInt()}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onMarkPaid,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Mark Paid",
                        tint = EmeraldNeon
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    currencySymbol: String,
    isMasked: Boolean
) {
    val isCredit = transaction.type == TransactionType.CREDIT.name
    val isTransfer = transaction.type == TransactionType.TRANSFER.name
    val tint = when {
        isCredit -> EmeraldNeon
        isTransfer -> CyanNeon
        else -> RoseNeon
    }
    val prefix = when {
        isCredit -> "+"
        isTransfer -> "⇄"
        else -> "-"
    }

    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    FuturisticCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else if (isTransfer) Icons.Default.SwapHoriz else Icons.Default.ArrowUpward,
                        contentDescription = transaction.type,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.personOrMerchant,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${transaction.category} • ${sdf.format(Date(transaction.date))}",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = if (isMasked) "••••" else "$prefix$currencySymbol${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
                color = tint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
