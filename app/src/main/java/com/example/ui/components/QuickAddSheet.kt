package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.entity.AccountEntity
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

enum class QuickAddTab(val label: String) {
    DEBIT("Debit / Spent"),
    CREDIT("Credit / Income"),
    TRANSFER("Transfer"),
    LENT("Money Lent"),
    BORROWED("Borrowed"),
    REMINDER("Reminder"),
    ASSET_LIABILITY("Asset / Debt")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    viewModel: FinanceViewModel,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(QuickAddTab.DEBIT) }

    // Common fields
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var personOrMerchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var targetAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: 0L) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI_ONLINE) }
    var selectedFrequency by remember { mutableStateOf(ReminderFrequency.MONTHLY) }
    var selectedAssetType by remember { mutableStateOf(AssetType.ASSET) }

    val currencySymbol = viewModel.userProfile.collectAsState().value?.currencySymbol ?: "₹"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkCardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Record",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Tab Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAddTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
                            .clickable {
                                selectedTab = tab
                                if (tab == QuickAddTab.CREDIT) {
                                    selectedCategory = ExpenseCategory.SALARY
                                } else if (tab == QuickAddTab.DEBIT) {
                                    selectedCategory = ExpenseCategory.FOOD
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab.label,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Input Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("AMOUNT", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currencySymbol,
                            color = CyanNeon,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            placeholder = { Text("0.00", color = TextTertiary, fontSize = 28.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_add_amount_input"),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Form based on Tab
            when (selectedTab) {
                QuickAddTab.DEBIT, QuickAddTab.CREDIT -> {
                    // Merchant / Person
                    OutlinedTextField(
                        value = personOrMerchantText,
                        onValueChange = { personOrMerchantText = it },
                        label = { Text(if (selectedTab == QuickAddTab.CREDIT) "Source / Sender" else "Merchant / Payee") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Account selector
                    AccountPicker(
                        label = "Account",
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onSelect = { selectedAccountId = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category chips
                    Text("CATEGORY", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val availableCategories = if (selectedTab == QuickAddTab.CREDIT) {
                            listOf(ExpenseCategory.SALARY, ExpenseCategory.FREELANCE, ExpenseCategory.INVESTMENT_RETURN, ExpenseCategory.OTHER)
                        } else {
                            ExpenseCategory.entries.filter { it != ExpenseCategory.SALARY && it != ExpenseCategory.FREELANCE && it != ExpenseCategory.INVESTMENT_RETURN }
                        }

                        availableCategories.forEach { cat ->
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) cat.color.copy(alpha = 0.25f) else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) cat.color else DarkCardBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat.label,
                                    color = if (isSel) cat.color else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description / Note
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                QuickAddTab.TRANSFER -> {
                    // From and To Accounts
                    AccountPicker(
                        label = "From Account",
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onSelect = { selectedAccountId = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AccountPicker(
                        label = "To Account",
                        accounts = accounts.filter { it.id != selectedAccountId },
                        selectedAccountId = targetAccountId,
                        onSelect = { targetAccountId = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Transfer Reason (e.g., ATM withdrawal, Savings allocation)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                QuickAddTab.LENT, QuickAddTab.BORROWED -> {
                    val isLent = selectedTab == QuickAddTab.LENT
                    OutlinedTextField(
                        value = personOrMerchantText,
                        onValueChange = { personOrMerchantText = it },
                        label = { Text(if (isLent) "Person who borrowed from you" else "Person who lent to you") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AccountPicker(
                        label = if (isLent) "Disburse from Account" else "Receive into Account",
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onSelect = { selectedAccountId = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Purpose / Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                QuickAddTab.REMINDER -> {
                    OutlinedTextField(
                        value = personOrMerchantText,
                        onValueChange = { personOrMerchantText = it },
                        label = { Text("Reminder Title / Service (e.g. WiFi Bill, Loan EMI)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Payee / Company Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                QuickAddTab.ASSET_LIABILITY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedAssetType = AssetType.ASSET },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAssetType == AssetType.ASSET) EmeraldNeon else DarkSurfaceElevated,
                                contentColor = if (selectedAssetType == AssetType.ASSET) DarkBackground else TextSecondary
                            )
                        ) {
                            Text("+ Asset")
                        }
                        Button(
                            onClick = { selectedAssetType = AssetType.LIABILITY },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAssetType == AssetType.LIABILITY) RoseNeon else DarkSurfaceElevated,
                                contentColor = if (selectedAssetType == AssetType.LIABILITY) DarkBackground else TextSecondary
                            )
                        ) {
                            Text("- Liability")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = personOrMerchantText,
                        onValueChange = { personOrMerchantText = it },
                        label = { Text("Item Title (e.g., Stocks Portfolio, Car, Home Loan)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Category / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) return@Button

                    when (selectedTab) {
                        QuickAddTab.CREDIT -> {
                            viewModel.addTransaction(
                                amount = amt,
                                type = TransactionType.CREDIT,
                                category = selectedCategory,
                                accountId = selectedAccountId,
                                personOrMerchant = personOrMerchantText.ifBlank { "Income" },
                                description = noteText,
                                paymentMethod = selectedPaymentMethod
                            )
                        }
                        QuickAddTab.DEBIT -> {
                            viewModel.addTransaction(
                                amount = amt,
                                type = TransactionType.DEBIT,
                                category = selectedCategory,
                                accountId = selectedAccountId,
                                personOrMerchant = personOrMerchantText.ifBlank { "Expense" },
                                description = noteText,
                                paymentMethod = selectedPaymentMethod
                            )
                        }
                        QuickAddTab.TRANSFER -> {
                            viewModel.transferMoney(
                                fromAccountId = selectedAccountId,
                                toAccountId = targetAccountId,
                                amount = amt,
                                note = noteText.ifBlank { "Inter-account transfer" }
                            )
                        }
                        QuickAddTab.LENT -> {
                            viewModel.addDebt(
                                personName = personOrMerchantText.ifBlank { "Friend" },
                                type = DebtType.LENT,
                                amount = amt,
                                dueDate = System.currentTimeMillis() + 14 * 86400000L,
                                description = noteText,
                                accountId = selectedAccountId
                            )
                        }
                        QuickAddTab.BORROWED -> {
                            viewModel.addDebt(
                                personName = personOrMerchantText.ifBlank { "Creditor" },
                                type = DebtType.BORROWED,
                                amount = amt,
                                dueDate = System.currentTimeMillis() + 14 * 86400000L,
                                description = noteText,
                                accountId = selectedAccountId
                            )
                        }
                        QuickAddTab.REMINDER -> {
                            viewModel.addReminder(
                                title = personOrMerchantText.ifBlank { "Bill Payment" },
                                personOrCompany = noteText.ifBlank { "Payee" },
                                amount = amt,
                                dueDate = System.currentTimeMillis() + 7 * 86400000L
                            )
                        }
                        QuickAddTab.ASSET_LIABILITY -> {
                            viewModel.addAssetLiability(
                                title = personOrMerchantText.ifBlank { "Asset Item" },
                                type = selectedAssetType,
                                category = "GENERAL",
                                value = amt,
                                notes = noteText
                            )
                        }
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("quick_add_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Confirm & Record", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun AccountPicker(
    label: String,
    accounts: List<AccountEntity>,
    selectedAccountId: Long,
    onSelect: (Long) -> Unit
) {
    Column {
        Text(label.uppercase(), color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { acc ->
                val isSelected = acc.id == selectedAccountId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceElevated else DarkSurfaceCard)
                        .border(1.dp, if (isSelected) CyanNeon else DarkCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelect(acc.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = acc.name,
                        color = if (isSelected) CyanNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanNeon,
    unfocusedBorderColor = DarkCardBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = CyanNeon,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = DarkSurfaceElevated,
    unfocusedContainerColor = DarkSurfaceElevated
)
