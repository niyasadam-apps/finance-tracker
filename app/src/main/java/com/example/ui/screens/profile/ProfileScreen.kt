package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FuturisticCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.outlinedFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: FinanceViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: "Niyas Adam") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "niyasadam4@gmail.com") }
    var phone by remember(profile) { mutableStateOf(profile?.phoneNumber ?: "+91 98765 43210") }
    var monthlyIncomeText by remember(profile) { mutableStateOf((profile?.monthlyIncome ?: 65000.0).toString()) }
    var financialGoal by remember(profile) { mutableStateOf(profile?.financialGoal ?: "") }
    var selectedCurrency by remember(profile) { mutableStateOf(profile?.currencySymbol ?: "₹") }
    var pinLockEnabled by remember(profile) { mutableStateOf(profile?.pinLockEnabled ?: false) }
    var pinCode by remember(profile) { mutableStateOf(profile?.pinCode ?: "1234") }
    var biometricEnabled by remember(profile) { mutableStateOf(profile?.biometricEnabled ?: false) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showExportResultDialog by remember { mutableStateOf<String?>(null) }

    val currencyOptions = listOf("₹" to "INR", "$" to "USD", "€" to "EUR", "£" to "GBP", "¥" to "JPY", "AED" to "AED")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Avatar Badge
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CyanNeon.copy(alpha = 0.15f))
                        .border(2.dp, CyanNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fullName.take(1).uppercase(),
                        color = CyanNeon,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = fullName,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = email,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Personal Information
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Personal Credentials")

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = monthlyIncomeText,
                    onValueChange = { monthlyIncomeText = it },
                    label = { Text("Monthly Baseline Income ($selectedCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = financialGoal,
                    onValueChange = { financialGoal = it },
                    label = { Text("Long-Term Financial Objective") },
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Currency Configuration
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Operating Currency")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currencyOptions.forEach { (sym, code) ->
                        val isSel = selectedCurrency == sym
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) CyanNeon else DarkSurfaceElevated)
                                .clickable { selectedCurrency = sym }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "$sym $code",
                                color = if (isSel) DarkBackground else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Security & Privacy Settings
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Security & App Lock")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable App PIN Lock", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Prompts for security PIN on launch", color = TextTertiary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = pinLockEnabled,
                        onCheckedChange = { pinLockEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = DarkSurfaceElevated)
                    )
                }

                if (pinLockEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { if (it.length <= 4) pinCode = it },
                        label = { Text("4-Digit PIN Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Biometric Authentication", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Fingerprint or Face Unlock simulation", color = TextTertiary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = DarkSurfaceElevated)
                    )
                }
            }
        }

        // Save Profile Button
        item {
            Button(
                onClick = {
                    val code = currencyOptions.find { it.first == selectedCurrency }?.second ?: "INR"
                    val inc = monthlyIncomeText.toDoubleOrNull() ?: 65000.0
                    viewModel.updateProfile(
                        fullName = fullName,
                        email = email,
                        phone = phone,
                        currencySymbol = selectedCurrency,
                        currencyCode = code,
                        monthlyIncome = inc,
                        financialGoal = financialGoal,
                        pinLockEnabled = pinLockEnabled,
                        pinCode = pinCode,
                        biometricEnabled = biometricEnabled
                    )
                    Toast.makeText(context, "Profile credentials saved successfully", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Profile Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Backup, Export & Import
        item {
            FuturisticCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Data Backup & Local Portability")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.exportJson()
                                showExportResultDialog = json
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = CyanNeon),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "JSON", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val csv = viewModel.exportCsv()
                                showExportResultDialog = csv
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = EmeraldNeon),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "CSV", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Import", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Backup Data (JSON)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Danger Zone: Delete All Data
        item {
            FuturisticCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = RoseNeon.copy(alpha = 0.5f)
            ) {
                SectionHeader(title = "Danger Zone")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Permanently wipes all accounts, transactions, debts, and goals stored locally on this device.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseNeon, contentColor = DarkBackground),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Purge & Delete All Data", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Export Dialog (with Copy to Clipboard)
    if (showExportResultDialog != null) {
        val exportText = showExportResultDialog!!
        AlertDialog(
            onDismissRequest = { showExportResultDialog = null },
            containerColor = DarkSurface,
            title = { Text("Backup Generated", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your financial telemetry data has been compiled locally:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = exportText.take(500) + if (exportText.length > 500) "\n... [truncated]" else "",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Nova Finance Backup", exportText))
                        Toast.makeText(context, "Copied backup to clipboard", Toast.LENGTH_SHORT).show()
                        showExportResultDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                ) {
                    Text("Copy to Clipboard", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportResultDialog = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = DarkSurface,
            title = { Text("Restore From JSON Backup", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste previously exported JSON backup below:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("{\"version\":1, \"accounts\":[...]}") },
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            coroutineScope.launch {
                                val success = viewModel.importJson(importJsonText)
                                if (success) {
                                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                    showImportDialog = false
                                } else {
                                    Toast.makeText(context, "Invalid JSON structure. Import failed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Wipe Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = DarkSurface,
            title = { Text("Confirm Complete Data Wipe?", color = RoseNeon, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This action cannot be undone. All your accounts, ledgers, debts, and goals will be eradicated from device storage.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "All data wiped clean", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseNeon, contentColor = DarkBackground)
                ) {
                    Text("Wipe Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
