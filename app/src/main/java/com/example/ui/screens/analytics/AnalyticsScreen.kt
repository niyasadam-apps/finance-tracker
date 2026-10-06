package com.example.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: FinanceViewModel) {
    val stats by viewModel.currentMonthStats.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.currentMonthCategoryBreakdown.collectAsStateWithLifecycle()
    val cashFlowData by viewModel.last6MonthsCashFlow.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.userProfile.collectAsStateWithLifecycle().value?.currencySymbol ?: "₹"

    // Largest expenses this month
    val largestExpenses = transactions
        .filter { it.type == TransactionType.DEBIT.name }
        .sortedByDescending { it.amount }
        .take(4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Financial Analytics",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "In-depth telemetry & algorithmic intelligence",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Savings Rate Ring & Month Totals Card
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth(), borderColor = CyanNeon) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SAVINGS METRIC", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMasked) "••••" else "${stats.savingsRate.toInt()}%",
                            color = CyanNeon,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${viewModel.formatCurrency(stats.savings)} saved this month",
                            color = EmeraldNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    ProgressRing(
                        progress = (stats.savingsRate / 100f).toFloat(),
                        modifier = Modifier.size(90.dp),
                        color = CyanNeon,
                        strokeWidth = 9.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Savings",
                            tint = CyanNeon,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("INCOME", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(stats.income),
                            color = EmeraldNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(DarkCardBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EXPENSES", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(stats.expenses),
                            color = RoseNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Cash Flow Trend (6 Months)
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "6-Month Cash Flow")
                Spacer(modifier = Modifier.height(8.dp))
                CashFlowChart(
                    data = cashFlowData,
                    currencySymbol = currencySymbol
                )
            }
        }

        // Category Expense Distribution
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Expense Allocation by Category")
                Spacer(modifier = Modifier.height(12.dp))
                SpendingBreakdownBar(
                    breakdown = categoryBreakdown,
                    currencySymbol = currencySymbol
                )
            }
        }

        // Largest Single Transactions
        item {
            Column {
                SectionHeader(title = "Largest Transactions")
                if (largestExpenses.isEmpty()) {
                    FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No debit transactions recorded yet", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        largestExpenses.forEach { tx ->
                            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = tx.personOrMerchant,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = tx.category,
                                            color = TextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = if (isMasked) "••••" else "-$currencySymbol${String.format(Locale.getDefault(), "%,.2f", tx.amount)}",
                                        color = RoseNeon,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
