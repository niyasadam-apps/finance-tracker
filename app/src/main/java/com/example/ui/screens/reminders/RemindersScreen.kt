package com.example.ui.screens.reminders

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ReminderEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ReminderFrequency
import com.example.data.model.ReminderNotice
import com.example.notification.NotificationHelper
import com.example.ui.components.FuturisticCard
import com.example.ui.components.outlinedFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersScreen(viewModel: FinanceViewModel) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isMasked by viewModel.maskAmounts.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.userProfile.collectAsStateWithLifecycle().value?.currencySymbol ?: "₹"
    val context = LocalContext.current

    var showAddReminderDialog by remember { mutableStateOf(false) }
    var filterOnlyPending by remember { mutableStateOf(true) }

    val displayedReminders = remember(reminders, filterOnlyPending) {
        if (filterOnlyPending) reminders.filter { !it.isPaid } else reminders
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
                        text = "Payment Reminders",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Due dates, recurring bills & local notifications",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showAddReminderDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (filterOnlyPending) CyanNeon else DarkSurfaceElevated)
                        .clickable { filterOnlyPending = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Pending (${reminders.count { !it.isPaid }})",
                        color = if (filterOnlyPending) DarkBackground else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!filterOnlyPending) CyanNeon else DarkSurfaceElevated)
                        .clickable { filterOnlyPending = false }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "All Reminders (${reminders.size})",
                        color = if (!filterOnlyPending) DarkBackground else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Reminders List
        if (displayedReminders.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text("No payment reminders found", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(displayedReminders, key = { it.id }) { reminder ->
                ReminderDetailCard(
                    reminder = reminder,
                    currencySymbol = currencySymbol,
                    isMasked = isMasked,
                    onMarkPaid = {
                        val accId = accounts.firstOrNull()?.id
                        viewModel.markReminderPaid(reminder, accId)
                        Toast.makeText(context, "Marked as paid & recorded in ledger", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { viewModel.deleteReminder(reminder) },
                    onTestNotification = {
                        NotificationHelper.showPaymentReminderNotification(
                            context = context,
                            notificationId = reminder.id.toInt(),
                            title = "Payment Alert: ${reminder.title}",
                            message = "Reminder: $currencySymbol${reminder.amount.toInt()} due for ${reminder.personOrCompany}"
                        )
                        Toast.makeText(context, "Local alert notification triggered", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Add Reminder Dialog
    if (showAddReminderDialog) {
        var title by remember { mutableStateOf("") }
        var payee by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var daysAhead by remember { mutableStateOf("5") }
        var freq by remember { mutableStateOf(ReminderFrequency.MONTHLY) }

        AlertDialog(
            onDismissRequest = { showAddReminderDialog = false },
            containerColor = DarkSurface,
            title = { Text("Schedule Payment Reminder", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Bill / Payment Title (e.g., Rent, Wifi)") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = payee,
                        onValueChange = { payee = it },
                        label = { Text("Payee / Service Company") },
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = daysAhead,
                        onValueChange = { daysAhead = it },
                        label = { Text("Due in (Days from now)") },
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
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val d = daysAhead.toLongOrNull() ?: 5L
                        if (title.isNotBlank() && amt > 0.0) {
                            viewModel.addReminder(
                                title = title,
                                personOrCompany = payee.ifBlank { "Service" },
                                amount = amt,
                                dueDate = System.currentTimeMillis() + d * 86400000L,
                                frequency = freq
                            )
                            showAddReminderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                ) {
                    Text("Save Reminder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReminderDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ReminderDetailCard(
    reminder: ReminderEntity,
    currencySymbol: String,
    isMasked: Boolean,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit,
    onTestNotification: () -> Unit
) {
    val now = System.currentTimeMillis()
    val diffDays = ((reminder.dueDate - now) / 86400000L).toInt()
    val sdf = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    val (statusLabel, statusColor) = when {
        reminder.isPaid -> "Paid & Settled" to EmeraldNeon
        diffDays < 0 -> "Overdue by ${-diffDays} days" to RoseNeon
        diffDays == 0 -> "Due Today!" to AmberNeon
        else -> "Due in $diffDays days" to CyanNeon
    }

    FuturisticCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = statusColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${reminder.personOrCompany} • ${reminder.frequency}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${sdf.format(Date(reminder.dueDate))} ($statusLabel)",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isMasked) "••••" else "$currencySymbol${reminder.amount.toInt()}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!reminder.isPaid) {
                    Button(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = DarkBackground),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Pay", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onTestNotification,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Test Alert", tint = CyanNeon, modifier = Modifier.size(18.dp))
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
