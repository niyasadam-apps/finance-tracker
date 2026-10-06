package com.example.ui.screens.debts

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DebtEntity
import com.example.data.model.DebtStatus
import com.example.data.model.DebtType
import com.example.ui.components.FuturisticCard
import com.example.ui.components.MetricCard
import com.example.ui.components.StatBadge
import com.example.ui.components.outlinedFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtsScreen(viewModel: FinanceViewModel) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val totalReceivable by viewModel.totalReceivable.collectAsStateWithLifecycle()
    val totalPayable by viewModel.totalPayable.collectAsStateWithLifecycle()
    val netDebtPosition by viewModel.netDebtPosition.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.userProfile.collectAsStateWithLifecycle().value?.currencySymbol ?: "₹"

    var selectedDebtTypeTab by remember { mutableStateOf<DebtType?>(null) } // null = All
    var showRecordPaymentDialogFor by remember { mutableStateOf<DebtEntity?>(null) }
    var showAddDebtDialog by remember { mutableStateOf(false) }

    val filteredDebts = remember(debts, selectedDebtTypeTab) {
        if (selectedDebtTypeTab == null) debts else debts.filter { it.type == selectedDebtTypeTab!!.name }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "People & Debts",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lent, borrowed & partial settlements",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showAddDebtDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_debt_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Debt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Metric Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Receivable",
                        value = viewModel.formatCurrency(totalReceivable),
                        subtitle = "Money others owe you",
                        icon = Icons.Default.CallReceived,
                        iconTint = EmeraldNeon,
                        accentColor = EmeraldNeon,
                        isMasked = isMasked,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Payable",
                        value = viewModel.formatCurrency(totalPayable),
                        subtitle = "Money you owe others",
                        icon = Icons.Default.CallMade,
                        iconTint = RoseNeon,
                        accentColor = RoseNeon,
                        isMasked = isMasked,
                        modifier = Modifier.weight(1f)
                    )
                }

                FuturisticCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (netDebtPosition >= 0) EmeraldNeon else RoseNeon
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NET DEBT POSITION", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isMasked) "••••••••" else viewModel.formatCurrency(netDebtPosition),
                                color = if (netDebtPosition >= 0) EmeraldNeon else RoseNeon,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        StatBadge(
                            text = if (netDebtPosition >= 0) "Net Positive" else "Net Owed",
                            color = if (netDebtPosition >= 0) EmeraldNeon else RoseNeon
                        )
                    }
                }
            }
        }

        // Type Filter Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf<Pair<String, DebtType?>>(
                    "All (${debts.size})" to null,
                    "Money I Lent" to DebtType.LENT,
                    "Money I Borrowed" to DebtType.BORROWED
                )

                tabs.forEach { (label, type) ->
                    val isSel = selectedDebtTypeTab == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) CyanNeon else DarkSurfaceElevated)
                            .clickable { selectedDebtTypeTab = type }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) DarkBackground else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Debts List
        if (filteredDebts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records in this category", color = TextTertiary, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredDebts, key = { it.id }) { debt ->
                DebtItemCard(
                    debt = debt,
                    currencySymbol = currencySymbol,
                    isMasked = isMasked,
                    onRecordPayment = { showRecordPaymentDialogFor = debt },
                    onDelete = { viewModel.deleteDebt(debt) }
                )
            }
        }
    }

    // Add Debt Dialog
    if (showAddDebtDialog) {
        var personName by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(DebtType.LENT) }
        var amountText by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var selectedAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            containerColor = DarkSurface,
            title = { Text("Record Lent or Borrowed Money", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { type = DebtType.LENT },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == DebtType.LENT) EmeraldNeon else DarkSurfaceElevated,
                                contentColor = if (type == DebtType.LENT) DarkBackground else TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I Lent Money", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { type = DebtType.BORROWED },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == DebtType.BORROWED) RoseNeon else DarkSurfaceElevated,
                                contentColor = if (type == DebtType.BORROWED) DarkBackground else TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I Borrowed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Person Name (e.g. Rahul, John)") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("debt_person_input")
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Total Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("debt_amount_input")
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Reason / Description") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (personName.isNotBlank() && amt > 0.0) {
                            viewModel.addDebt(
                                personName = personName,
                                type = type,
                                amount = amt,
                                dueDate = System.currentTimeMillis() + 14 * 86400000L,
                                description = desc,
                                accountId = selectedAccountId
                            )
                            showAddDebtDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                ) {
                    Text("Save Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Record Partial/Full Repayment Dialog
    if (showRecordPaymentDialogFor != null) {
        val targetDebt = showRecordPaymentDialogFor!!
        var paymentAmtText by remember { mutableStateOf(targetDebt.remainingAmount.toString()) }
        var selectedAccId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { showRecordPaymentDialogFor = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Record Repayment for ${targetDebt.personName}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Original: $currencySymbol${targetDebt.originalAmount.toInt()} | Already Paid: $currencySymbol${targetDebt.paidAmount.toInt()} | Remaining: $currencySymbol${targetDebt.remainingAmount.toInt()}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = paymentAmtText,
                        onValueChange = { paymentAmtText = it },
                        label = { Text("Payment Installment ($currencySymbol)") },
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
                        val pAmt = paymentAmtText.toDoubleOrNull() ?: 0.0
                        if (pAmt > 0.0) {
                            viewModel.recordDebtRepayment(
                                debtId = targetDebt.id,
                                amount = pAmt,
                                accountId = selectedAccId
                            )
                            showRecordPaymentDialogFor = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = DarkBackground)
                ) {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPaymentDialogFor = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun DebtItemCard(
    debt: DebtEntity,
    currencySymbol: String,
    isMasked: Boolean,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val isLent = debt.type == DebtType.LENT.name
    val accentColor = if (isLent) EmeraldNeon else RoseNeon
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    FuturisticCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accentColor.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLent) Icons.Default.CallReceived else Icons.Default.CallMade,
                        contentDescription = debt.type,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = debt.personName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isLent) "Lent to them" else "Borrowed from them",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (debt.description.isNotBlank()) {
                        Text(
                            text = debt.description,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isMasked) "••••" else "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", debt.remainingAmount)}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Remaining of $currencySymbol${debt.originalAmount.toInt()}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar of repayment
        val progress = if (debt.originalAmount > 0) (debt.paidAmount / debt.originalAmount).toFloat() else 0f
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = DarkSurfaceElevated
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Due: ${sdf.format(Date(debt.dueDate))} • ${debt.status}",
                color = TextTertiary,
                fontSize = 11.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (debt.remainingAmount > 0) {
                    Button(
                        onClick = onRecordPayment,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = CyanNeon
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Pay / Settle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
