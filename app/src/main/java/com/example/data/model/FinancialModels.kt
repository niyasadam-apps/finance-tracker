package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

enum class TransactionType(val label: String) {
    CREDIT("Credit / Income"),
    DEBIT("Debit / Expense"),
    TRANSFER("Transfer")
}

enum class DebtType(val label: String) {
    LENT("Money I Lent (Receivable)"),
    BORROWED("Money I Borrowed (Payable)")
}

enum class DebtStatus(val label: String) {
    ACTIVE("Active"),
    PARTIALLY_PAID("Partially Paid"),
    SETTLED("Settled")
}

enum class AccountType(val label: String) {
    BANK("Bank Account"),
    CASH("Cash"),
    SAVINGS("Savings Account"),
    CREDIT_CARD("Credit Card"),
    WALLET("Digital Wallet"),
    INVESTMENT("Investment"),
    OTHER("Other")
}

enum class AssetType(val label: String) {
    ASSET("Asset"),
    LIABILITY("Liability")
}

enum class AssetCategory(val label: String) {
    REAL_ESTATE("Real Estate / Property"),
    VEHICLE("Vehicle"),
    INVESTMENT("Stock / Mutual Funds"),
    CRYPTO("Crypto / Digital"),
    GOLD("Gold & Precious Metals"),
    VALUABLE("Valuables & Gadgets"),
    OTHER("Other Asset"),
    
    // Liabilities
    HOME_LOAN("Home Loan"),
    CAR_LOAN("Car Loan"),
    PERSONAL_LOAN("Personal Loan"),
    EDUCATION_LOAN("Education Loan"),
    CREDIT_CARD_DEBT("Credit Card Balance"),
    OTHER_DEBT("Other Liability")
}

enum class ExpenseCategory(val label: String, val color: Color) {
    FOOD("Food & Dining", CatFood),
    SHOPPING("Shopping", CatShopping),
    TRAVEL("Travel", CatTravel),
    BILLS("Bills & Utilities", CatBills),
    RENT("Rent & Housing", CatRent),
    ENTERTAINMENT("Entertainment", CatEntertainment),
    EDUCATION("Education", CatEducation),
    HEALTHCARE("Healthcare", CatHealthcare),
    TRANSPORTATION("Transportation", CatTransportation),
    SUBSCRIPTIONS("Subscriptions", CatSubscriptions),
    SALARY("Salary", EmeraldNeon),
    FREELANCE("Freelance Income", CyanNeon),
    INVESTMENT_RETURN("Investment Return", AmberNeon),
    OTHER("Other", CatOther);

    companion object {
        fun fromString(name: String?): ExpenseCategory {
            return entries.find { it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true) }
                ?: OTHER
        }
    }
}

enum class PaymentMethod(val label: String) {
    UPI_ONLINE("UPI / Online"),
    CARD("Credit / Debit Card"),
    NET_BANKING("Net Banking"),
    CASH("Cash"),
    OTHER("Other")
}

enum class ReminderFrequency(val label: String) {
    ONCE("Once"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class ReminderNotice(val label: String, val daysBefore: Int) {
    ON_DUE_DATE("On due date", 0),
    ONE_DAY_BEFORE("1 day before", 1),
    THREE_DAYS_BEFORE("3 days before", 3),
    SEVEN_DAYS_BEFORE("7 days before", 7)
}

enum class ReminderStatus(val label: String) {
    UPCOMING("Upcoming"),
    DUE_TODAY("Due Today"),
    OVERDUE("Overdue"),
    PAID("Paid")
}

data class FinancialInsight(
    val id: String,
    val title: String,
    val message: String,
    val type: InsightType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class InsightType {
    INFO,
    POSITIVE,
    WARNING,
    ALERT
}
