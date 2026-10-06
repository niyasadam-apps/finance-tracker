package com.example.ui.screens.transactions

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
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.ui.components.FuturisticCard
import com.example.ui.components.outlinedFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class TxTypeFilter(val label: String) {
    ALL("All"),
    CREDIT("Credits / In"),
    DEBIT("Debits / Out"),
    TRANSFER("Transfers")
}

enum class TxSortOrder(val label: String) {
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    AMOUNT_DESC("Amount (Highest)"),
    AMOUNT_ASC("Amount (Lowest)")
}

@Composable
fun TransactionsScreen(viewModel: FinanceViewModel) {
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.userProfile.collectAsStateWithLifecycle().value?.currencySymbol ?: "₹"

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf(TxTypeFilter.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedAccountIdFilter by remember { mutableStateOf<Long?>(null) }
    var sortOrder by remember { mutableStateOf(TxSortOrder.DATE_DESC) }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Filter and Sort Pipeline
    val filteredTransactions = remember(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedAccountIdFilter,
        sortOrder
    ) {
        var list = allTransactions.asSequence()

        // 1. Search Query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase(Locale.getDefault())
            list = list.filter {
                it.personOrMerchant.lowercase(Locale.getDefault()).contains(q) ||
                it.description.lowercase(Locale.getDefault()).contains(q) ||
                it.category.lowercase(Locale.getDefault()).contains(q)
            }
        }

        // 2. Type Filter
        when (selectedTypeFilter) {
            TxTypeFilter.CREDIT -> list = list.filter { it.type == TransactionType.CREDIT.name }
            TxTypeFilter.DEBIT -> list = list.filter { it.type == TransactionType.DEBIT.name }
            TxTypeFilter.TRANSFER -> list = list.filter { it.type == TransactionType.TRANSFER.name }
            TxTypeFilter.ALL -> {}
        }

        // 3. Category Filter
        if (selectedCategoryFilter != null) {
            list = list.filter { it.category == selectedCategoryFilter }
        }

        // 4. Account Filter
        if (selectedAccountIdFilter != null) {
            list = list.filter { it.accountId == selectedAccountIdFilter || it.toAccountId == selectedAccountIdFilter }
        }

        // 5. Sorting
        val sorted = when (sortOrder) {
            TxSortOrder.DATE_DESC -> list.sortedByDescending { it.date }
            TxSortOrder.DATE_ASC -> list.sortedBy { it.date }
            TxSortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            TxSortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
        }

        sorted.toList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "Ledger & History",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredTransactions.size} transactions found",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Sort toggle button
                    IconButton(
                        onClick = {
                            sortOrder = when (sortOrder) {
                                TxSortOrder.DATE_DESC -> TxSortOrder.AMOUNT_DESC
                                TxSortOrder.AMOUNT_DESC -> TxSortOrder.DATE_ASC
                                TxSortOrder.DATE_ASC -> TxSortOrder.AMOUNT_ASC
                                TxSortOrder.AMOUNT_ASC -> TxSortOrder.DATE_DESC
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = CyanNeon
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by merchant, note, person...", color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_search_input"),
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Type Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TxTypeFilter.entries.forEach { filter ->
                        val isSel = selectedTypeFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) CyanNeon else DarkSurfaceElevated)
                                .clickable { selectedTypeFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = filter.label,
                                color = if (isSel) DarkBackground else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Account & Category Filter Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Account pills
                    accounts.forEach { acc ->
                        val isSel = selectedAccountIdFilter == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) DarkSurfaceElevated else DarkSurfaceCard)
                                .border(1.dp, if (isSel) EmeraldNeon else DarkCardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedAccountIdFilter = if (isSel) null else acc.id
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = acc.name,
                                color = if (isSel) EmeraldNeon else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Transaction Items
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "Empty",
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No transactions match your criteria",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val acc = accounts.find { it.id == tx.accountId }
                    val toAcc = if (tx.toAccountId != null) accounts.find { it.id == tx.toAccountId } else null
                    TransactionDetailCard(
                        transaction = tx,
                        accountName = acc?.name ?: "Account",
                        toAccountName = toAcc?.name,
                        currencySymbol = currencySymbol,
                        isMasked = isMasked,
                        onDeleteClick = { transactionToDelete = tx }
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                containerColor = DarkSurface,
                title = { Text("Delete Transaction?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to delete this ${transactionToDelete!!.type} of $currencySymbol${transactionToDelete!!.amount}? This will reverse the account balance adjustment.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(transactionToDelete!!)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseNeon)
                    ) {
                        Text("Delete", color = DarkBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionDetailCard(
    transaction: TransactionEntity,
    accountName: String,
    toAccountName: String?,
    currencySymbol: String,
    isMasked: Boolean,
    onDeleteClick: () -> Unit
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
        isTransfer -> "⇄ "
        else -> "-"
    }

    val sdf = remember { SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()) }

    FuturisticCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = DarkCardBorder
    ) {
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f))
                        .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else if (isTransfer) Icons.Default.SwapHoriz else Icons.Default.ArrowUpward,
                        contentDescription = transaction.type,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.personOrMerchant,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isTransfer && toAccountName != null) "$accountName → $toAccountName" else "$accountName • ${transaction.category}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = sdf.format(Date(transaction.date)),
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    if (transaction.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "“${transaction.description}”",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isMasked) "••••••••" else "$prefix$currencySymbol${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
                    color = tint,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
