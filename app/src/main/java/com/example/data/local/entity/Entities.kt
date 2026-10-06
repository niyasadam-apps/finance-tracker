package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["name"]),
        Index(value = ["type"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // AccountType enum name: BANK, CASH, SAVINGS, CREDIT_CARD, WALLET, INVESTMENT, OTHER
    val balance: Double,
    val initialBalance: Double = 0.0,
    val accountNumberLast4: String = "",
    val colorHex: String = "#00E5FF",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["toAccountId"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["debtId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // CREDIT, DEBIT, TRANSFER
    val category: String, // ExpenseCategory enum name
    val accountId: Long,
    val toAccountId: Long? = null,
    val personOrMerchant: String,
    val description: String = "",
    val paymentMethod: String = "UPI_ONLINE",
    val date: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val receiptNote: String = "",
    val debtId: Long? = null
)

@Entity(
    tableName = "debts",
    indices = [
        Index(value = ["type"]),
        Index(value = ["dueDate"]),
        Index(value = ["status"]),
        Index(value = ["personName"])
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val type: String, // LENT, BORROWED
    val originalAmount: Double,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double,
    val dateGivenOrBorrowed: Long = System.currentTimeMillis(),
    val dueDate: Long,
    val description: String = "",
    val status: String = "ACTIVE", // ACTIVE, PARTIALLY_PAID, SETTLED
    val accountId: Long? = null
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["isPaid"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val personOrCompany: String,
    val amount: Double,
    val dueDate: Long,
    val noticeOption: String = "ON_DUE_DATE",
    val frequency: String = "MONTHLY",
    val isPaid: Boolean = false,
    val paidDate: Long? = null,
    val accountId: Long? = null,
    val category: String = "BILLS"
)

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["targetDate"])
    ]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDate: Long,
    val category: String = "TECH",
    val iconName: String = "LAPTOP",
    val colorHex: String = "#00E5FF",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "assets_liabilities",
    indices = [
        Index(value = ["type"]),
        Index(value = ["category"])
    ]
)
data class AssetLiabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // ASSET, LIABILITY
    val category: String,
    val value: Double,
    val interestRate: Double = 0.0,
    val monthlyPayment: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Niyas Adam",
    val email: String = "niyasadam4@gmail.com",
    val phoneNumber: String = "+91 98765 43210",
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val monthlyIncome: Double = 65000.0,
    val financialGoal: String = "Build 6-Month Emergency Fund & Tech Portfolio",
    val pinLockEnabled: Boolean = false,
    val pinCode: String = "",
    val biometricEnabled: Boolean = false,
    val hideSensitiveAmounts: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val themeMode: String = "FUTURISTIC_DARK"
)
