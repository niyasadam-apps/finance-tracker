package com.example.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.ui.components.FuturisticCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.outlinedFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun AccountsScreen(viewModel: FinanceViewModel) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.userProfile.collectAsStateWithLifecycle().value?.currencySymbol ?: "₹"

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedAccountForDetails by remember { mutableStateOf<AccountEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accounts & Vaults",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${accounts.size} active vaults",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showTransferDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = CyanNeon
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showAddAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_account_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Account Cards
        items(accounts, key = { it.id }) { account ->
            val accountTransactions = transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
            val moneyIn = accountTransactions
                .filter { (it.type == TransactionType.CREDIT.name && it.accountId == account.id) || (it.type == TransactionType.TRANSFER.name && it.toAccountId == account.id) }
                .sumOf { it.amount }
            val moneyOut = accountTransactions
                .filter { (it.type == TransactionType.DEBIT.name && it.accountId == account.id) || (it.type == TransactionType.TRANSFER.name && it.accountId == account.id) }
                .sumOf { it.amount }

            val parsedColor = remember(account.colorHex) {
                try {
                    Color(android.graphics.Color.parseColor(account.colorHex))
                } catch (e: Exception) {
                    CyanNeon
                }
            }

            AccountCardItem(
                account = account,
                accentColor = parsedColor,
                moneyIn = moneyIn,
                moneyOut = moneyOut,
                currencySymbol = currencySymbol,
                isMasked = isMasked,
                onAccountClick = {
                    selectedAccountForDetails = if (selectedAccountForDetails?.id == account.id) null else account
                },
                onDelete = { viewModel.deleteAccount(account) }
            )
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(AccountType.BANK) }
        var balanceText by remember { mutableStateOf("") }
        var last4 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            containerColor = DarkSurface,
            title = { Text("Create New Vault / Account", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g. Chase Bank, Cash)") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                    )

                    // Type selection chips
                    Text("VAULT TYPE", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountType.entries.forEach { accType ->
                            val isSel = type == accType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) CyanNeon else DarkSurfaceElevated)
                                    .clickable { type = accType }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = accType.label,
                                    color = if (isSel) DarkBackground else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Initial Balance ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                    )

                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4) last4 = it },
                        label = { Text("Last 4 digits (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val initBal = balanceText.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            viewModel.addAccount(
                                name = name,
                                type = type,
                                balance = initBal,
                                last4 = last4,
                                colorHex = when (type) {
                                    AccountType.BANK -> "#00E5FF"
                                    AccountType.CASH -> "#00E676"
                                    AccountType.SAVINGS -> "#8C52FF"
                                    AccountType.CREDIT_CARD -> "#FF3366"
                                    else -> "#2979FF"
                                }
                            )
                            showAddAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                    modifier = Modifier.testTag("save_account_confirm")
                ) {
                    Text("Create Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Inter-Account Transfer Dialog
    if (showTransferDialog && accounts.size >= 2) {
        var fromId by remember { mutableStateOf(accounts.first().id) }
        var toId by remember { mutableStateOf(accounts[1].id) }
        var transferAmount by remember { mutableStateOf("") }
        var transferNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            containerColor = DarkSurface,
            title = { Text("Inter-Vault Transfer", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Move funds between your personal accounts. This will not affect income or expenses.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Transfer Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                    )

                    OutlinedTextField(
                        value = transferNote,
                        onValueChange = { transferNote = it },
                        label = { Text("Transfer Note (e.g. ATM withdrawal)") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = transferAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0 && fromId != toId) {
                            viewModel.transferMoney(
                                fromAccountId = fromId,
                                toAccountId = toId,
                                amount = amt,
                                note = transferNote.ifBlank { "Account transfer" }
                            )
                            showTransferDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                ) {
                    Text("Complete Transfer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun AccountCardItem(
    account: AccountEntity,
    accentColor: Color,
    moneyIn: Double,
    moneyOut: Double,
    currencySymbol: String,
    isMasked: Boolean,
    onAccountClick: () -> Unit,
    onDelete: () -> Unit
) {
    FuturisticCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accentColor.copy(alpha = 0.5f),
        onClick = onAccountClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (account.type) {
                            "BANK" -> Icons.Default.AccountBalance
                            "CASH" -> Icons.Default.Payments
                            "CREDIT_CARD" -> Icons.Default.CreditCard
                            "SAVINGS" -> Icons.Default.Savings
                            else -> Icons.Default.AccountBalanceWallet
                        },
                        contentDescription = account.type,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = account.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${account.type} ${if (account.accountNumberLast4.isNotBlank()) "•••• ${account.accountNumberLast4}" else ""}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isMasked) "••••••••" else "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", account.balance)}",
                    color = if (account.balance < 0) RoseNeon else TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Current Balance",
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // In / Out telemetry row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Money In: ", color = TextTertiary, fontSize = 11.sp)
                Text(
                    text = if (isMasked) "••••" else "+$currencySymbol${String.format(Locale.getDefault(), "%,.0f", moneyIn)}",
                    color = EmeraldNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Money Out: ", color = TextTertiary, fontSize = 11.sp)
                Text(
                    text = if (isMasked) "••••" else "-$currencySymbol${String.format(Locale.getDefault(), "%,.0f", moneyOut)}",
                    color = RoseNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
