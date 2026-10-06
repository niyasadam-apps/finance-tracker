package com.example.ui.screens.networth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AssetLiabilityEntity
import com.example.data.model.AssetType
import com.example.data.model.DebtType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

enum class NetWorthTimeframe(val label: String) {
    D7("7D"),
    D30("30D"),
    M6("6M"),
    Y1("1Y"),
    ALL("ALL")
}

@Composable
fun NetWorthScreen(viewModel: FinanceViewModel) {
    val totalAssets by viewModel.totalAssets.collectAsStateWithLifecycle()
    val totalLiabilities by viewModel.totalLiabilities.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val customItems by viewModel.assetsLiabilities.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.userProfile.collectAsStateWithLifecycle().value?.currencySymbol ?: "₹"

    var selectedTimeframe by remember { mutableStateOf(NetWorthTimeframe.M6) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    val simulatedPoints = remember(selectedTimeframe, netWorth) {
        val base = netWorth.toFloat().coerceAtLeast(1000f)
        when (selectedTimeframe) {
            NetWorthTimeframe.D7 -> listOf(base * 0.98f, base * 0.985f, base * 0.99f, base * 0.993f, base * 0.996f, base * 0.998f, base)
            NetWorthTimeframe.D30 -> listOf(base * 0.94f, base * 0.95f, base * 0.965f, base * 0.97f, base * 0.985f, base * 0.99f, base)
            NetWorthTimeframe.M6 -> listOf(base * 0.85f, base * 0.88f, base * 0.91f, base * 0.94f, base * 0.97f, base)
            NetWorthTimeframe.Y1 -> listOf(base * 0.72f, base * 0.78f, base * 0.83f, base * 0.90f, base * 0.95f, base)
            NetWorthTimeframe.ALL -> listOf(base * 0.50f, base * 0.65f, base * 0.78f, base * 0.88f, base)
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
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Net Worth Engine",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Assets − Liabilities balance sheet",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showAddItemDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Net Worth Chart & Master Display
        item {
            FuturisticCard(
                borderColor = CyanNeon,
                glowColor = CyanGlow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("TOTAL NET WORTH", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMasked) "••••••••" else viewModel.formatCurrency(netWorth),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    StatBadge(text = "+12.4% Trajectory", color = EmeraldNeon)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeframe Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NetWorthTimeframe.entries.forEach { tf ->
                        val isSel = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) CyanNeon else DarkSurfaceElevated)
                                .clickable { selectedTimeframe = tf }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tf.label,
                                color = if (isSel) DarkBackground else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NetWorthSplineChart(
                    points = simulatedPoints,
                    lineColor = CyanNeon,
                    height = 100.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Accounting Equation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL ASSETS (+)", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(totalAssets),
                            color = EmeraldNeon,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("−", color = TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Column {
                        Text("LIABILITIES (−)", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(totalLiabilities),
                            color = RoseNeon,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("=", color = TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Column {
                        Text("NET WORTH", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMasked) "••••" else viewModel.formatCurrency(netWorth),
                            color = CyanNeon,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Assets Breakdown Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Assets Portfolio")

                // Accounts with positive balance
                accounts.filter { it.balance > 0 }.forEach { acc ->
                    AssetLiabilityRow(
                        title = acc.name,
                        category = "Account Vault (${acc.type})",
                        amount = acc.balance,
                        isAsset = true,
                        currencySymbol = currencySymbol,
                        isMasked = isMasked
                    )
                }

                // Money Lent (Receivables)
                debts.filter { it.type == DebtType.LENT.name && it.remainingAmount > 0 }.forEach { debt ->
                    AssetLiabilityRow(
                        title = "Receivable from ${debt.personName}",
                        category = "Money Lent",
                        amount = debt.remainingAmount,
                        isAsset = true,
                        currencySymbol = currencySymbol,
                        isMasked = isMasked
                    )
                }

                // Custom Assets
                customItems.filter { it.type == AssetType.ASSET.name }.forEach { asset ->
                    AssetLiabilityRow(
                        title = asset.title,
                        category = asset.category,
                        amount = asset.value,
                        isAsset = true,
                        currencySymbol = currencySymbol,
                        isMasked = isMasked,
                        onDelete = { viewModel.deleteAssetLiability(asset) }
                    )
                }
            }
        }

        // Liabilities Breakdown Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Liabilities & Debts")

                // Accounts with negative balance (Credit cards)
                accounts.filter { it.balance < 0 }.forEach { acc ->
                    AssetLiabilityRow(
                        title = acc.name,
                        category = "Credit Card Debt",
                        amount = kotlin.math.abs(acc.balance),
                        isAsset = false,
                        currencySymbol = currencySymbol,
                        isMasked = isMasked
                    )
                }

                // Money Borrowed (Payables)
                debts.filter { it.type == DebtType.BORROWED.name && it.remainingAmount > 0 }.forEach { debt ->
                    AssetLiabilityRow(
                        title = "Payable to ${debt.personName}",
                        category = "Money Borrowed",
                        amount = debt.remainingAmount,
                        isAsset = false,
                        currencySymbol = currencySymbol,
                        isMasked = isMasked
                    )
                }

                // Custom Liabilities
                customItems.filter { it.type == AssetType.LIABILITY.name }.forEach { liability ->
                    AssetLiabilityRow(
                        title = liability.title,
                        category = liability.category,
                        amount = liability.value,
                        isAsset = false,
                        currencySymbol = currencySymbol,
                        isMasked = isMasked,
                        onDelete = { viewModel.deleteAssetLiability(liability) }
                    )
                }
            }
        }
    }

    // Add Custom Asset / Liability Dialog
    if (showAddItemDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(AssetType.ASSET) }
        var category by remember { mutableStateOf("Investment") }
        var valueText by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            containerColor = DarkSurface,
            title = { Text("Add Asset or Liability", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { type = AssetType.ASSET },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == AssetType.ASSET) EmeraldNeon else DarkSurfaceElevated,
                                contentColor = if (type == AssetType.ASSET) DarkBackground else TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Asset", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { type = AssetType.LIABILITY },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == AssetType.LIABILITY) RoseNeon else DarkSurfaceElevated,
                                contentColor = if (type == AssetType.LIABILITY) DarkBackground else TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("- Liability", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g. Stocks, Vehicle, Home Loan)") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g. Property, Tech, Loan)") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = { Text("Estimated Valuation ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val v = valueText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && v > 0.0) {
                            viewModel.addAssetLiability(
                                title = title,
                                type = type,
                                category = category,
                                value = v,
                                notes = notes
                            )
                            showAddItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                ) {
                    Text("Save Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun AssetLiabilityRow(
    title: String,
    category: String,
    amount: Double,
    isAsset: Boolean,
    currencySymbol: String,
    isMasked: Boolean,
    onDelete: (() -> Unit)? = null
) {
    val tint = if (isAsset) EmeraldNeon else RoseNeon

    FuturisticCard(modifier = Modifier.fillMaxWidth()) {
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
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAsset) Icons.Default.TrendingUp else Icons.Default.CreditCard,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = category, color = TextTertiary, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMasked) "••••" else "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", amount)}",
                    color = tint,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
