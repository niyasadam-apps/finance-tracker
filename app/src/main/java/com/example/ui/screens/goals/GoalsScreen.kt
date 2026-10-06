package com.example.ui.screens.goals

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GoalEntity
import com.example.ui.components.FuturisticCard
import com.example.ui.components.ProgressRing
import com.example.ui.components.outlinedFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GoalsScreen(viewModel: FinanceViewModel) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.userProfile.collectAsStateWithLifecycle().value?.currencySymbol ?: "₹"

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showDepositDialogFor by remember { mutableStateOf<GoalEntity?>(null) }

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
                        text = "Savings Goals",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target tracking & progress milestones",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showCreateGoalDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Goals List
        if (goals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No goals created yet. Set a financial target!", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalItemCard(
                    goal = goal,
                    currencySymbol = currencySymbol,
                    isMasked = isMasked,
                    onDeposit = { showDepositDialogFor = goal },
                    onDelete = { viewModel.deleteGoal(goal) }
                )
            }
        }
    }

    // Create Goal Dialog
    if (showCreateGoalDialog) {
        var title by remember { mutableStateOf("") }
        var targetAmountText by remember { mutableStateOf("") }
        var targetDaysAhead by remember { mutableStateOf("90") }

        AlertDialog(
            onDismissRequest = { showCreateGoalDialog = false },
            containerColor = DarkSurface,
            title = { Text("Create Savings Goal", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Goal Title (e.g., MacBook, Car, House)") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { targetAmountText = it },
                        label = { Text("Target Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetDaysAhead,
                        onValueChange = { targetDaysAhead = it },
                        label = { Text("Target Deadline (Days from today)") },
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
                        val tAmt = targetAmountText.toDoubleOrNull() ?: 0.0
                        val days = targetDaysAhead.toLongOrNull() ?: 90L
                        if (title.isNotBlank() && tAmt > 0.0) {
                            viewModel.addGoal(
                                title = title,
                                targetAmount = tAmt,
                                targetDate = System.currentTimeMillis() + days * 86400000L
                            )
                            showCreateGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                ) {
                    Text("Create Goal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGoalDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Deposit to Goal Dialog
    if (showDepositDialogFor != null) {
        val targetGoal = showDepositDialogFor!!
        var depositAmountText by remember { mutableStateOf("") }
        var selectedAccId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { showDepositDialogFor = null },
            containerColor = DarkSurface,
            title = { Text("Add Funds to ${targetGoal.title}", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Target: $currencySymbol${targetGoal.targetAmount.toInt()} | Already Saved: $currencySymbol${targetGoal.savedAmount.toInt()}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = depositAmountText,
                        onValueChange = { depositAmountText = it },
                        label = { Text("Deposit Amount ($currencySymbol)") },
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
                        val amt = depositAmountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            viewModel.addFundsToGoal(targetGoal, amt, selectedAccId)
                            showDepositDialogFor = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = DarkBackground)
                ) {
                    Text("Deposit Funds", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialogFor = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun GoalItemCard(
    goal: GoalEntity,
    currencySymbol: String,
    isMasked: Boolean,
    onDeposit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat() else 0f
    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    FuturisticCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CyanNeon.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMasked) "••••" else "Saved: $currencySymbol${goal.savedAmount.toInt()} of $currencySymbol${goal.targetAmount.toInt()}",
                    color = CyanNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Remaining: $currencySymbol${remaining.toInt()} • Target: ${sdf.format(Date(goal.targetDate))}",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            ProgressRing(
                progress = progress,
                modifier = Modifier.size(68.dp),
                color = CyanNeon,
                strokeWidth = 6.dp
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDeposit,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Funds", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Funds", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
