package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db.financeDao())
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentTransactions = repository.recentTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts = repository.debts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assetsLiabilities = repository.assetsLiabilities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val userProfile = repository.userProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Security & Privacy UI States
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _maskAmounts = MutableStateFlow(false)
    val maskAmounts: StateFlow<Boolean> = _maskAmounts.asStateFlow()

    fun toggleMaskAmounts() {
        _maskAmounts.value = !_maskAmounts.value
    }

    fun unlockWithPin(pin: String): Boolean {
        val correctPin = userProfile.value?.pinCode ?: "1234"
        if (pin == correctPin || correctPin.isEmpty()) {
            _isLocked.value = false
            return true
        }
        return false
    }

    fun lockApp() {
        if (userProfile.value?.pinLockEnabled == true) {
            _isLocked.value = true
        }
    }

    // ------------------------------------------------------------------------
    // CALCULATED FINANCIAL METRICS
    // ------------------------------------------------------------------------

    // Total Balance = sum of all account balances
    val totalBalance: StateFlow<Double> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total Assets = Positive Account balances + Money Lent to others + Custom Assets
    val totalAssets: StateFlow<Double> = combine(accounts, debts, assetsLiabilities) { accs, dbts, als ->
        val positiveAccountBalances = accs.filter { it.balance > 0 }.sumOf { it.balance }
        val moneyLent = dbts.filter { it.type == DebtType.LENT.name }.sumOf { it.remainingAmount }
        val customAssets = als.filter { it.type == AssetType.ASSET.name }.sumOf { it.value }
        positiveAccountBalances + moneyLent + customAssets
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total Liabilities = Negative Account balances + Money Borrowed + Custom Liabilities
    val totalLiabilities: StateFlow<Double> = combine(accounts, debts, assetsLiabilities) { accs, dbts, als ->
        val negativeAccountBalances = accs.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
        val moneyBorrowed = dbts.filter { it.type == DebtType.BORROWED.name }.sumOf { it.remainingAmount }
        val customLiabilities = als.filter { it.type == AssetType.LIABILITY.name }.sumOf { it.value }
        negativeAccountBalances + moneyBorrowed + customLiabilities
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Net Worth = Total Assets - Total Liabilities
    val netWorth: StateFlow<Double> = combine(totalAssets, totalLiabilities) { assets, liabilities ->
        assets - liabilities
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Money owed to me (Receivables)
    val totalReceivable: StateFlow<Double> = debts.map { list ->
        list.filter { it.type == DebtType.LENT.name }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Money I owe others (Payables)
    val totalPayable: StateFlow<Double> = debts.map { list ->
        list.filter { it.type == DebtType.BORROWED.name }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Net Debt Position = Receivable - Payable
    val netDebtPosition: StateFlow<Double> = combine(totalReceivable, totalPayable) { rec, pay ->
        rec - pay
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Month Metrics
    data class MonthlyStats(
        val income: Double,
        val expenses: Double,
        val savings: Double,
        val savingsRate: Double
    )

    val currentMonthStats: StateFlow<MonthlyStats> = transactions.map { txList ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var income = 0.0
        var expenses = 0.0

        val txCal = Calendar.getInstance()
        txList.forEach { tx ->
            txCal.timeInMillis = tx.date
            if (txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear) {
                when (tx.type) {
                    TransactionType.CREDIT.name -> income += tx.amount
                    TransactionType.DEBIT.name -> expenses += tx.amount
                }
            }
        }

        val savings = income - expenses
        val rate = if (income > 0) (savings / income * 100.0).coerceIn(0.0, 100.0) else 0.0

        MonthlyStats(income, expenses, savings, rate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyStats(0.0, 0.0, 0.0, 0.0))

    // Spending breakdown by Category
    val currentMonthCategoryBreakdown: StateFlow<Map<ExpenseCategory, Double>> = transactions.map { txList ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val map = mutableMapOf<ExpenseCategory, Double>()
        val txCal = Calendar.getInstance()

        txList.forEach { tx ->
            if (tx.type == TransactionType.DEBIT.name) {
                txCal.timeInMillis = tx.date
                if (txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear) {
                    val cat = ExpenseCategory.fromString(tx.category)
                    map[cat] = (map[cat] ?: 0.0) + tx.amount
                }
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Cash flow data: last 6 months (Month Label, Income, Expenses)
    data class MonthCashFlow(val monthLabel: String, val income: Double, val expense: Double)

    val last6MonthsCashFlow: StateFlow<List<MonthCashFlow>> = transactions.map { txList ->
        val result = mutableListOf<MonthCashFlow>()
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 5 downTo 0) {
            val targetCal = Calendar.getInstance()
            targetCal.add(Calendar.MONTH, -i)
            val targetMonth = targetCal.get(Calendar.MONTH)
            val targetYear = targetCal.get(Calendar.YEAR)
            val label = monthFormat.format(targetCal.time)

            var inc = 0.0
            var exp = 0.0
            val itemCal = Calendar.getInstance()

            txList.forEach { tx ->
                itemCal.timeInMillis = tx.date
                if (itemCal.get(Calendar.MONTH) == targetMonth && itemCal.get(Calendar.YEAR) == targetYear) {
                    when (tx.type) {
                        TransactionType.CREDIT.name -> inc += tx.amount
                        TransactionType.DEBIT.name -> exp += tx.amount
                    }
                }
            }

            result.add(MonthCashFlow(label, inc, exp))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Financial Insights (derived strictly from user data)
    val smartInsights: StateFlow<List<FinancialInsight>> = combine(
        transactions,
        debts,
        reminders,
        currentMonthStats,
        currentMonthCategoryBreakdown
    ) { txList, debtList, remList, stats, catMap ->
        val insights = mutableListOf<FinancialInsight>()

        // 1. Top spending category
        val topCategory = catMap.maxByOrNull { it.value }
        if (topCategory != null && topCategory.value > 0) {
            insights.add(
                FinancialInsight(
                    id = "top_cat",
                    title = "Highest Spending Area",
                    message = "${topCategory.key.label} is your highest expense this month at ${formatCurrency(topCategory.value)}.",
                    type = InsightType.INFO
                )
            )
        }

        // 2. Savings rate
        if (stats.savingsRate >= 30.0) {
            insights.add(
                FinancialInsight(
                    id = "savings_high",
                    title = "Excellent Savings Pace",
                    message = "You are currently saving ${stats.savingsRate.toInt()}% of your income this month (${formatCurrency(stats.savings)} saved).",
                    type = InsightType.POSITIVE
                )
            )
        } else if (stats.expenses > stats.income && stats.income > 0) {
            insights.add(
                FinancialInsight(
                    id = "expenses_exceed",
                    title = "Deficit Alert",
                    message = "Your expenses (${formatCurrency(stats.expenses)}) exceed this month's recorded income (${formatCurrency(stats.income)}).",
                    type = InsightType.WARNING
                )
            )
        }

        // 3. Upcoming / Overdue payment reminders
        val now = System.currentTimeMillis()
        val sevenDaysAhead = now + 7 * 86400000L
        val upcomingRemindersCount = remList.count { !it.isPaid && it.dueDate in now..sevenDaysAhead }
        val overdueRemindersCount = remList.count { !it.isPaid && it.dueDate < now }

        if (overdueRemindersCount > 0) {
            insights.add(
                FinancialInsight(
                    id = "overdue_rem",
                    title = "Overdue Payment Attention",
                    message = "You have $overdueRemindersCount overdue bill/payment pending settlement.",
                    type = InsightType.ALERT
                )
            )
        } else if (upcomingRemindersCount > 0) {
            insights.add(
                FinancialInsight(
                    id = "upcoming_rem",
                    title = "Upcoming Commitments",
                    message = "You have $upcomingRemindersCount payments due within the next 7 days.",
                    type = InsightType.INFO
                )
            )
        }

        // 4. Debt position
        val receivable = debtList.filter { it.type == DebtType.LENT.name }.sumOf { it.remainingAmount }
        val payable = debtList.filter { it.type == DebtType.BORROWED.name }.sumOf { it.remainingAmount }
        if (receivable > 0) {
            insights.add(
                FinancialInsight(
                    id = "debts_rec",
                    title = "Receivables Pending",
                    message = "Friends and colleagues owe you a total of ${formatCurrency(receivable)}.",
                    type = InsightType.POSITIVE
                )
            )
        }

        if (insights.isEmpty()) {
            insights.add(
                FinancialInsight(
                    id = "default_ready",
                    title = "Financial OS Active",
                    message = "All offline records are synchronized and ready. Track expenses to see detailed telemetry.",
                    type = InsightType.INFO
                )
            )
        }

        insights
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ------------------------------------------------------------------------
    // ACTIONS & REPOSITORY DISPATCHES
    // ------------------------------------------------------------------------

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        accountId: Long,
        toAccountId: Long? = null,
        personOrMerchant: String,
        description: String = "",
        paymentMethod: PaymentMethod = PaymentMethod.UPI_ONLINE,
        date: Long = System.currentTimeMillis(),
        isRecurring: Boolean = false,
        recurringFrequency: ReminderFrequency? = null,
        receiptNote: String = ""
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    amount = amount,
                    type = type.name,
                    category = category.name,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    personOrMerchant = personOrMerchant.ifBlank { "Personal" },
                    description = description,
                    paymentMethod = paymentMethod.name,
                    date = date,
                    isRecurring = isRecurring,
                    recurringFrequency = recurringFrequency?.name,
                    receiptNote = receiptNote
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        balance: Double,
        last4: String = "",
        colorHex: String = "#00E5FF"
    ) {
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    name = name,
                    type = type.name,
                    balance = balance,
                    initialBalance = balance,
                    accountNumberLast4 = last4,
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun transferMoney(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String = "Inter-account transfer"
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    amount = amount,
                    type = TransactionType.TRANSFER.name,
                    category = "OTHER",
                    accountId = fromAccountId,
                    toAccountId = toAccountId,
                    personOrMerchant = "Self Transfer",
                    description = note,
                    paymentMethod = "OTHER",
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    fun addDebt(
        personName: String,
        type: DebtType,
        amount: Double,
        dueDate: Long,
        description: String = "",
        accountId: Long? = null
    ) {
        viewModelScope.launch {
            repository.addDebt(
                DebtEntity(
                    personName = personName,
                    type = type.name,
                    originalAmount = amount,
                    paidAmount = 0.0,
                    remainingAmount = amount,
                    dueDate = dueDate,
                    description = description,
                    accountId = accountId
                ),
                disburseFromAccountId = accountId
            )
        }
    }

    fun recordDebtRepayment(
        debtId: Long,
        amount: Double,
        accountId: Long?,
        note: String = "Repayment installment"
    ) {
        viewModelScope.launch {
            repository.recordDebtRepayment(debtId, amount, accountId, note)
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun addReminder(
        title: String,
        personOrCompany: String,
        amount: Double,
        dueDate: Long,
        noticeOption: ReminderNotice = ReminderNotice.ON_DUE_DATE,
        frequency: ReminderFrequency = ReminderFrequency.MONTHLY,
        category: ExpenseCategory = ExpenseCategory.BILLS
    ) {
        viewModelScope.launch {
            repository.addReminder(
                ReminderEntity(
                    title = title,
                    personOrCompany = personOrCompany,
                    amount = amount,
                    dueDate = dueDate,
                    noticeOption = noticeOption.name,
                    frequency = frequency.name,
                    category = category.name
                )
            )
        }
    }

    fun markReminderPaid(reminder: ReminderEntity, accountId: Long?) {
        viewModelScope.launch {
            repository.markReminderAsPaidWithTransaction(reminder, accountId)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun addGoal(
        title: String,
        targetAmount: Double,
        targetDate: Long,
        category: String = "GENERAL",
        iconName: String = "LAPTOP",
        colorHex: String = "#00E5FF"
    ) {
        viewModelScope.launch {
            repository.addGoal(
                GoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    savedAmount = 0.0,
                    targetDate = targetDate,
                    category = category,
                    iconName = iconName,
                    colorHex = colorHex
                )
            )
        }
    }

    fun addFundsToGoal(goal: GoalEntity, amount: Double, sourceAccountId: Long?) {
        viewModelScope.launch {
            repository.addFundsToGoal(goal, amount, sourceAccountId)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addAssetLiability(
        title: String,
        type: AssetType,
        category: String,
        value: Double,
        interestRate: Double = 0.0,
        monthlyPayment: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addAssetLiability(
                AssetLiabilityEntity(
                    title = title,
                    type = type.name,
                    category = category,
                    value = value,
                    interestRate = interestRate,
                    monthlyPayment = monthlyPayment,
                    notes = notes
                )
            )
        }
    }

    fun deleteAssetLiability(item: AssetLiabilityEntity) {
        viewModelScope.launch {
            repository.deleteAssetLiability(item)
        }
    }

    fun updateProfile(
        fullName: String,
        email: String,
        phone: String,
        currencySymbol: String,
        currencyCode: String,
        monthlyIncome: Double,
        financialGoal: String,
        pinLockEnabled: Boolean,
        pinCode: String,
        biometricEnabled: Boolean
    ) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(
                current.copy(
                    fullName = fullName,
                    email = email,
                    phoneNumber = phone,
                    currencySymbol = currencySymbol,
                    currencyCode = currencyCode,
                    monthlyIncome = monthlyIncome,
                    financialGoal = financialGoal,
                    pinLockEnabled = pinLockEnabled,
                    pinCode = pinCode,
                    biometricEnabled = biometricEnabled
                )
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    suspend fun exportJson(): String {
        return repository.exportDataAsJson(
            accounts.value,
            transactions.value,
            debts.value,
            reminders.value,
            goals.value,
            assetsLiabilities.value,
            userProfile.value
        )
    }

    suspend fun exportCsv(): String {
        return repository.exportTransactionsAsCsv(transactions.value)
    }

    suspend fun importJson(json: String): Boolean {
        return repository.importDataFromJson(json)
    }

    fun formatCurrency(amount: Double): String {
        val sym = userProfile.value?.currencySymbol ?: "₹"
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val formattedNumber = String.format(Locale.getDefault(), "%,.2f", absAmount)
        return if (isNegative) "-$sym$formattedNumber" else "$sym$formattedNumber"
    }
}
