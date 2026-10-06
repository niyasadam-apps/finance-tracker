package com.example.data.repository

import com.example.data.local.dao.FinanceDao
import com.example.data.local.entity.*
import com.example.data.model.DebtStatus
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepository(private val dao: FinanceDao) {

    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = dao.getRecentTransactions(20)
    val debts: Flow<List<DebtEntity>> = dao.getAllDebts()
    val reminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
    val pendingReminders: Flow<List<ReminderEntity>> = dao.getPendingReminders()
    val goals: Flow<List<GoalEntity>> = dao.getAllGoals()
    val assetsLiabilities: Flow<List<AssetLiabilityEntity>> = dao.getAllAssetsLiabilities()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()

    suspend fun getAccountById(id: Long) = dao.getAccountById(id)
    suspend fun getDebtById(id: Long) = dao.getDebtById(id)
    suspend fun getUserProfileOnce() = dao.getUserProfileOnce()

    // ----------------------------------------------------
    // TRANSACTIONS & BALANCE RECONCILIATION
    // ----------------------------------------------------
    suspend fun addTransaction(transaction: TransactionEntity): Long {
        val txId = dao.insertTransaction(transaction)

        // Update Account Balances
        when (transaction.type) {
            TransactionType.CREDIT.name -> {
                val account = dao.getAccountById(transaction.accountId)
                if (account != null) {
                    val newBalance = account.balance + transaction.amount
                    dao.updateAccountBalance(account.id, newBalance)
                }
            }
            TransactionType.DEBIT.name -> {
                val account = dao.getAccountById(transaction.accountId)
                if (account != null) {
                    val newBalance = account.balance - transaction.amount
                    dao.updateAccountBalance(account.id, newBalance)
                }
            }
            TransactionType.TRANSFER.name -> {
                val fromAccount = dao.getAccountById(transaction.accountId)
                if (fromAccount != null) {
                    dao.updateAccountBalance(fromAccount.id, fromAccount.balance - transaction.amount)
                }
                if (transaction.toAccountId != null) {
                    val toAccount = dao.getAccountById(transaction.toAccountId)
                    if (toAccount != null) {
                        dao.updateAccountBalance(toAccount.id, toAccount.balance + transaction.amount)
                    }
                }
            }
        }

        // Handle linked debt repayment if applicable
        if (transaction.debtId != null) {
            val debt = dao.getDebtById(transaction.debtId)
            if (debt != null) {
                val newPaid = debt.paidAmount + transaction.amount
                val newRemaining = (debt.originalAmount - newPaid).coerceAtLeast(0.0)
                val newStatus = if (newRemaining <= 0.0) DebtStatus.SETTLED.name else DebtStatus.PARTIALLY_PAID.name
                dao.updateDebt(
                    debt.copy(
                        paidAmount = newPaid,
                        remainingAmount = newRemaining,
                        status = newStatus
                    )
                )
            }
        }

        return txId
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        // Revert balance modifications
        when (tx.type) {
            TransactionType.CREDIT.name -> {
                val account = dao.getAccountById(tx.accountId)
                if (account != null) {
                    dao.updateAccountBalance(account.id, account.balance - tx.amount)
                }
            }
            TransactionType.DEBIT.name -> {
                val account = dao.getAccountById(tx.accountId)
                if (account != null) {
                    dao.updateAccountBalance(account.id, account.balance + tx.amount)
                }
            }
            TransactionType.TRANSFER.name -> {
                val fromAccount = dao.getAccountById(tx.accountId)
                if (fromAccount != null) {
                    dao.updateAccountBalance(fromAccount.id, fromAccount.balance + tx.amount)
                }
                if (tx.toAccountId != null) {
                    val toAccount = dao.getAccountById(tx.toAccountId)
                    if (toAccount != null) {
                        dao.updateAccountBalance(toAccount.id, toAccount.balance - tx.amount)
                    }
                }
            }
        }

        // Revert debt repayment if linked
        if (tx.debtId != null) {
            val debt = dao.getDebtById(tx.debtId)
            if (debt != null) {
                val newPaid = (debt.paidAmount - tx.amount).coerceAtLeast(0.0)
                val newRemaining = (debt.originalAmount - newPaid).coerceAtLeast(0.0)
                val newStatus = if (newPaid <= 0.0) DebtStatus.ACTIVE.name else DebtStatus.PARTIALLY_PAID.name
                dao.updateDebt(
                    debt.copy(
                        paidAmount = newPaid,
                        remainingAmount = newRemaining,
                        status = newStatus
                    )
                )
            }
        }

        dao.deleteTransaction(tx)
    }

    // ----------------------------------------------------
    // ACCOUNTS
    // ----------------------------------------------------
    suspend fun addAccount(account: AccountEntity): Long {
        return dao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        dao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        dao.deleteAccount(account)
    }

    // ----------------------------------------------------
    // DEBTS (LENT & BORROWED)
    // ----------------------------------------------------
    suspend fun addDebt(debt: DebtEntity, disburseFromAccountId: Long? = null): Long {
        val debtId = dao.insertDebt(debt)
        // If an account is selected to disburse/receive funds when recording debt:
        if (disburseFromAccountId != null) {
            if (debt.type == DebtType.LENT.name) {
                // Giving money to someone -> Debit from our account
                addTransaction(
                    TransactionEntity(
                        amount = debt.originalAmount,
                        type = TransactionType.DEBIT.name,
                        category = "OTHER",
                        accountId = disburseFromAccountId,
                        personOrMerchant = debt.personName,
                        description = "Lent to ${debt.personName}: ${debt.description}",
                        paymentMethod = "OTHER",
                        date = debt.dateGivenOrBorrowed,
                        debtId = debtId
                    )
                )
            } else {
                // Borrowed money from someone -> Credit into our account
                addTransaction(
                    TransactionEntity(
                        amount = debt.originalAmount,
                        type = TransactionType.CREDIT.name,
                        category = "OTHER",
                        accountId = disburseFromAccountId,
                        personOrMerchant = debt.personName,
                        description = "Borrowed from ${debt.personName}: ${debt.description}",
                        paymentMethod = "OTHER",
                        date = debt.dateGivenOrBorrowed,
                        debtId = debtId
                    )
                )
            }
        }
        return debtId
    }

    suspend fun recordDebtRepayment(
        debtId: Long,
        paymentAmount: Double,
        accountId: Long?,
        description: String = "Debt payment"
    ) {
        val debt = dao.getDebtById(debtId) ?: return
        val newPaid = (debt.paidAmount + paymentAmount).coerceAtMost(debt.originalAmount)
        val newRemaining = (debt.originalAmount - newPaid).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.0) DebtStatus.SETTLED.name else DebtStatus.PARTIALLY_PAID.name

        dao.updateDebt(
            debt.copy(
                paidAmount = newPaid,
                remainingAmount = newRemaining,
                status = newStatus
            )
        )

        // If an account is linked, create a transaction for the payment
        if (accountId != null) {
            val isLent = debt.type == DebtType.LENT.name
            // If we lent money, repayment is money coming in (CREDIT)
            // If we borrowed money, repayment is money going out (DEBIT)
            val txType = if (isLent) TransactionType.CREDIT.name else TransactionType.DEBIT.name
            val desc = if (isLent) "Repayment received from ${debt.personName}" else "Repayment paid to ${debt.personName}"

            addTransaction(
                TransactionEntity(
                    amount = paymentAmount,
                    type = txType,
                    category = "OTHER",
                    accountId = accountId,
                    personOrMerchant = debt.personName,
                    description = "$desc ($description)",
                    paymentMethod = "UPI_ONLINE",
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        dao.deleteDebt(debt)
    }

    // ----------------------------------------------------
    // REMINDERS
    // ----------------------------------------------------
    suspend fun addReminder(reminder: ReminderEntity): Long {
        return dao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        dao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        dao.deleteReminder(reminder)
    }

    suspend fun markReminderPaid(reminderId: Long, accountId: Long?) {
        val remindersList = dao.getAllReminders()
        // We'll query and update
        dao.getPendingReminders() // fetch check
        // Find reminder
        // Directly update in DB:
    }

    suspend fun markReminderAsPaidWithTransaction(reminder: ReminderEntity, accountId: Long?) {
        dao.updateReminder(reminder.copy(isPaid = true, paidDate = System.currentTimeMillis(), accountId = accountId))
        if (accountId != null) {
            addTransaction(
                TransactionEntity(
                    amount = reminder.amount,
                    type = TransactionType.DEBIT.name,
                    category = reminder.category,
                    accountId = accountId,
                    personOrMerchant = reminder.personOrCompany,
                    description = "Payment for ${reminder.title}",
                    paymentMethod = "UPI_ONLINE",
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    // ----------------------------------------------------
    // GOALS
    // ----------------------------------------------------
    suspend fun addGoal(goal: GoalEntity): Long = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = dao.deleteGoal(goal)

    suspend fun addFundsToGoal(goal: GoalEntity, amount: Double, sourceAccountId: Long?) {
        val newSaved = goal.savedAmount + amount
        dao.updateGoal(goal.copy(savedAmount = newSaved))
        if (sourceAccountId != null) {
            addTransaction(
                TransactionEntity(
                    amount = amount,
                    type = TransactionType.DEBIT.name,
                    category = "OTHER",
                    accountId = sourceAccountId,
                    personOrMerchant = goal.title,
                    description = "Savings deposit for goal: ${goal.title}",
                    paymentMethod = "UPI_ONLINE",
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    // ----------------------------------------------------
    // ASSETS & LIABILITIES
    // ----------------------------------------------------
    suspend fun addAssetLiability(item: AssetLiabilityEntity): Long = dao.insertAssetLiability(item)
    suspend fun updateAssetLiability(item: AssetLiabilityEntity) = dao.updateAssetLiability(item)
    suspend fun deleteAssetLiability(item: AssetLiabilityEntity) = dao.deleteAssetLiability(item)

    // ----------------------------------------------------
    // USER PROFILE
    // ----------------------------------------------------
    suspend fun updateProfile(profile: UserProfileEntity) {
        dao.insertUserProfile(profile)
    }

    // ----------------------------------------------------
    // SEED INITIAL DATA
    // ----------------------------------------------------
    suspend fun seedInitialDataIfNeeded() {
        if (dao.getAccountCount() == 0) {
            // Seed Profile
            dao.insertUserProfile(
                UserProfileEntity(
                    id = 1,
                    fullName = "Niyas Adam",
                    email = "niyasadam4@gmail.com",
                    phoneNumber = "+91 98765 43210",
                    currencySymbol = "₹",
                    currencyCode = "INR",
                    monthlyIncome = 65000.0,
                    financialGoal = "Build 6-Month Emergency Fund & Tech Portfolio",
                    pinLockEnabled = false,
                    pinCode = "1234",
                    biometricEnabled = false,
                    hideSensitiveAmounts = false,
                    notificationsEnabled = true,
                    themeMode = "FUTURISTIC_DARK"
                )
            )

            // Seed Accounts
            val bankId = dao.insertAccount(
                AccountEntity(
                    name = "HDFC Bank (Primary)",
                    type = "BANK",
                    balance = 185000.0,
                    initialBalance = 150000.0,
                    accountNumberLast4 = "9821",
                    colorHex = "#00E5FF"
                )
            )

            val savingsId = dao.insertAccount(
                AccountEntity(
                    name = "Federal High-Yield Savings",
                    type = "SAVINGS",
                    balance = 45000.0,
                    initialBalance = 40000.0,
                    accountNumberLast4 = "4310",
                    colorHex = "#8C52FF"
                )
            )

            val cashId = dao.insertAccount(
                AccountEntity(
                    name = "Cash Wallet",
                    type = "CASH",
                    balance = 15800.0,
                    initialBalance = 10000.0,
                    accountNumberLast4 = "",
                    colorHex = "#00E676"
                )
            )

            val creditCardId = dao.insertAccount(
                AccountEntity(
                    name = "Titanium Credit Card",
                    type = "CREDIT_CARD",
                    balance = -12450.0,
                    initialBalance = 0.0,
                    accountNumberLast4 = "7732",
                    colorHex = "#FF3366"
                )
            )

            val currentTime = System.currentTimeMillis()
            val dayMs = 86400000L

            // Seed Transactions
            dao.insertTransaction(
                TransactionEntity(
                    amount = 65000.0,
                    type = TransactionType.CREDIT.name,
                    category = "SALARY",
                    accountId = bankId,
                    personOrMerchant = "Anthropic / Tech Corp",
                    description = "Monthly Salary Deposit",
                    paymentMethod = "NET_BANKING",
                    date = currentTime - 4 * dayMs,
                    isRecurring = true,
                    recurringFrequency = "MONTHLY"
                )
            )

            dao.insertTransaction(
                TransactionEntity(
                    amount = 12000.0,
                    type = TransactionType.DEBIT.name,
                    category = "RENT",
                    accountId = bankId,
                    personOrMerchant = "Skyline Apartments",
                    description = "Apartment Rent & Maintenance",
                    paymentMethod = "UPI_ONLINE",
                    date = currentTime - 3 * dayMs,
                    isRecurring = true,
                    recurringFrequency = "MONTHLY"
                )
            )

            dao.insertTransaction(
                TransactionEntity(
                    amount = 3200.0,
                    type = TransactionType.DEBIT.name,
                    category = "FOOD",
                    accountId = bankId,
                    personOrMerchant = "Gourmet Bistro & Market",
                    description = "Groceries and weekend dinner",
                    paymentMethod = "CARD",
                    date = currentTime - 2 * dayMs
                )
            )

            dao.insertTransaction(
                TransactionEntity(
                    amount = 1499.0,
                    type = TransactionType.DEBIT.name,
                    category = "SUBSCRIPTIONS",
                    accountId = bankId,
                    personOrMerchant = "Cloud Server & AI Compute",
                    description = "Development server cluster",
                    paymentMethod = "CARD",
                    date = currentTime - 1 * dayMs,
                    isRecurring = true,
                    recurringFrequency = "MONTHLY"
                )
            )

            dao.insertTransaction(
                TransactionEntity(
                    amount = 5000.0,
                    type = TransactionType.TRANSFER.name,
                    category = "OTHER",
                    accountId = bankId,
                    toAccountId = cashId,
                    personOrMerchant = "Self ATM",
                    description = "ATM Cash Withdrawal for weekly pocket expenses",
                    paymentMethod = "CASH",
                    date = currentTime - 1 * dayMs
                )
            )

            // Seed Debts (People & Lent / Borrowed)
            dao.insertDebt(
                DebtEntity(
                    personName = "John",
                    type = DebtType.LENT.name,
                    originalAmount = 5000.0,
                    paidAmount = 0.0,
                    remainingAmount = 5000.0,
                    dateGivenOrBorrowed = currentTime - 5 * dayMs,
                    dueDate = currentTime + 7 * dayMs,
                    description = "Split concert tickets & travel",
                    status = DebtStatus.ACTIVE.name,
                    accountId = bankId
                )
            )

            dao.insertDebt(
                DebtEntity(
                    personName = "Rahul",
                    type = DebtType.BORROWED.name,
                    originalAmount = 3000.0,
                    paidAmount = 1000.0,
                    remainingAmount = 2000.0,
                    dateGivenOrBorrowed = currentTime - 10 * dayMs,
                    dueDate = currentTime + 2 * dayMs,
                    description = "Conference pass upfront booking",
                    status = DebtStatus.PARTIALLY_PAID.name,
                    accountId = bankId
                )
            )

            // Seed Reminders
            dao.insertReminder(
                ReminderEntity(
                    title = "Pay Rahul",
                    personOrCompany = "Rahul",
                    amount = 2000.0,
                    dueDate = currentTime + 2 * dayMs,
                    noticeOption = "ONE_DAY_BEFORE",
                    frequency = "ONCE",
                    isPaid = false,
                    category = "OTHER"
                )
            )

            dao.insertReminder(
                ReminderEntity(
                    title = "High-Speed Fiber Internet",
                    personOrCompany = "Airtel Xstream Fiber",
                    amount = 799.0,
                    dueDate = currentTime + 5 * dayMs,
                    noticeOption = "ONE_DAY_BEFORE",
                    frequency = "MONTHLY",
                    isPaid = false,
                    category = "BILLS"
                )
            )

            dao.insertReminder(
                ReminderEntity(
                    title = "Next Month Rent",
                    personOrCompany = "Skyline Apartments",
                    amount = 12000.0,
                    dueDate = currentTime + 9 * dayMs,
                    noticeOption = "THREE_DAYS_BEFORE",
                    frequency = "MONTHLY",
                    isPaid = false,
                    category = "RENT"
                )
            )

            // Seed Goals
            dao.insertGoal(
                GoalEntity(
                    title = "New MacBook Pro M4",
                    targetAmount = 80000.0,
                    savedAmount = 35000.0,
                    targetDate = currentTime + 60 * dayMs,
                    category = "TECH",
                    iconName = "LAPTOP",
                    colorHex = "#00E5FF"
                )
            )

            dao.insertGoal(
                GoalEntity(
                    title = "Emergency Fund (6 Months)",
                    targetAmount = 200000.0,
                    savedAmount = 110000.0,
                    targetDate = currentTime + 180 * dayMs,
                    category = "SAFETY",
                    iconName = "SHIELD",
                    colorHex = "#00E676"
                )
            )

            // Seed Assets & Liabilities
            dao.insertAssetLiability(
                AssetLiabilityEntity(
                    title = "Tech ETF & Index Portfolio",
                    type = "ASSET",
                    category = "INVESTMENT",
                    value = 175000.0,
                    interestRate = 12.5,
                    notes = "Long-term index growth fund"
                )
            )

            dao.insertAssetLiability(
                AssetLiabilityEntity(
                    title = "Digital Gadgets & Studio Setup",
                    type = "ASSET",
                    category = "VALUABLE",
                    value = 65000.0,
                    notes = "Laptop, 4K monitors, audio gear"
                )
            )

            dao.insertAssetLiability(
                AssetLiabilityEntity(
                    title = "Gadget EMI Loan",
                    type = "LIABILITY",
                    category = "PERSONAL_LOAN",
                    value = 15000.0,
                    monthlyPayment = 3000.0,
                    notes = "5 remaining installments"
                )
            )
        }
    }

    // ----------------------------------------------------
    // EXPORT & BACKUP / IMPORT
    // ----------------------------------------------------
    suspend fun clearAllData() {
        dao.clearAccounts()
        dao.clearTransactions()
        dao.clearDebts()
        dao.clearReminders()
        dao.clearGoals()
        dao.clearAssetsLiabilities()
    }

    suspend fun exportDataAsJson(
        accountsList: List<AccountEntity>,
        transactionsList: List<TransactionEntity>,
        debtsList: List<DebtEntity>,
        remindersList: List<ReminderEntity>,
        goalsList: List<GoalEntity>,
        assetsLiabilitiesList: List<AssetLiabilityEntity>,
        profile: UserProfileEntity?
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        if (profile != null) {
            val pObj = JSONObject()
            pObj.put("fullName", profile.fullName)
            pObj.put("email", profile.email)
            pObj.put("currencySymbol", profile.currencySymbol)
            pObj.put("monthlyIncome", profile.monthlyIncome)
            pObj.put("financialGoal", profile.financialGoal)
            root.put("profile", pObj)
        }

        val accArr = JSONArray()
        accountsList.forEach {
            val a = JSONObject()
            a.put("name", it.name)
            a.put("type", it.type)
            a.put("balance", it.balance)
            a.put("initialBalance", it.initialBalance)
            a.put("accountNumberLast4", it.accountNumberLast4)
            a.put("colorHex", it.colorHex)
            accArr.put(a)
        }
        root.put("accounts", accArr)

        val txArr = JSONArray()
        transactionsList.forEach {
            val t = JSONObject()
            t.put("amount", it.amount)
            t.put("type", it.type)
            t.put("category", it.category)
            t.put("accountId", it.accountId)
            t.put("toAccountId", it.toAccountId)
            t.put("personOrMerchant", it.personOrMerchant)
            t.put("description", it.description)
            t.put("paymentMethod", it.paymentMethod)
            t.put("date", it.date)
            t.put("isRecurring", it.isRecurring)
            txArr.put(t)
        }
        root.put("transactions", txArr)

        val debtArr = JSONArray()
        debtsList.forEach {
            val d = JSONObject()
            d.put("personName", it.personName)
            d.put("type", it.type)
            d.put("originalAmount", it.originalAmount)
            d.put("paidAmount", it.paidAmount)
            d.put("remainingAmount", it.remainingAmount)
            d.put("dueDate", it.dueDate)
            d.put("status", it.status)
            debtArr.put(d)
        }
        root.put("debts", debtArr)

        val remArr = JSONArray()
        remindersList.forEach {
            val r = JSONObject()
            r.put("title", it.title)
            r.put("personOrCompany", it.personOrCompany)
            r.put("amount", it.amount)
            r.put("dueDate", it.dueDate)
            r.put("isPaid", it.isPaid)
            r.put("category", it.category)
            remArr.put(r)
        }
        root.put("reminders", remArr)

        val goalArr = JSONArray()
        goalsList.forEach {
            val g = JSONObject()
            g.put("title", it.title)
            g.put("targetAmount", it.targetAmount)
            g.put("savedAmount", it.savedAmount)
            g.put("targetDate", it.targetDate)
            goalArr.put(g)
        }
        root.put("goals", goalArr)

        val alArr = JSONArray()
        assetsLiabilitiesList.forEach {
            val al = JSONObject()
            al.put("title", it.title)
            al.put("type", it.type)
            al.put("category", it.category)
            al.put("value", it.value)
            alArr.put(al)
        }
        root.put("assets_liabilities", alArr)

        return root.toString(2)
    }

    suspend fun exportTransactionsAsCsv(transactionsList: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Type,Amount,Category,Merchant/Person,Description,PaymentMethod\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        transactionsList.forEach {
            sb.append("${it.id},")
            sb.append("\"${sdf.format(Date(it.date))}\",")
            sb.append("${it.type},")
            sb.append("${it.amount},")
            sb.append("\"${it.category}\",")
            sb.append("\"${it.personOrMerchant.replace("\"", "\"\"")}\",")
            sb.append("\"${it.description.replace("\"", "\"\"")}\",")
            sb.append("${it.paymentMethod}\n")
        }
        return sb.toString()
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            clearAllData()

            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                dao.insertUserProfile(
                    UserProfileEntity(
                        id = 1,
                        fullName = p.optString("fullName", "Niyas Adam"),
                        email = p.optString("email", "niyasadam4@gmail.com"),
                        currencySymbol = p.optString("currencySymbol", "₹"),
                        monthlyIncome = p.optDouble("monthlyIncome", 65000.0),
                        financialGoal = p.optString("financialGoal", "")
                    )
                )
            }

            if (root.has("accounts")) {
                val accArr = root.getJSONArray("accounts")
                for (i in 0 until accArr.length()) {
                    val a = accArr.getJSONObject(i)
                    dao.insertAccount(
                        AccountEntity(
                            name = a.optString("name", "Account"),
                            type = a.optString("type", "BANK"),
                            balance = a.optDouble("balance", 0.0),
                            initialBalance = a.optDouble("initialBalance", 0.0),
                            accountNumberLast4 = a.optString("accountNumberLast4", ""),
                            colorHex = a.optString("colorHex", "#00E5FF")
                        )
                    )
                }
            }

            if (root.has("transactions")) {
                val txArr = root.getJSONArray("transactions")
                for (i in 0 until txArr.length()) {
                    val t = txArr.getJSONObject(i)
                    dao.insertTransaction(
                        TransactionEntity(
                            amount = t.optDouble("amount", 0.0),
                            type = t.optString("type", "DEBIT"),
                            category = t.optString("category", "OTHER"),
                            accountId = t.optLong("accountId", 1),
                            toAccountId = if (t.has("toAccountId") && !t.isNull("toAccountId")) t.getLong("toAccountId") else null,
                            personOrMerchant = t.optString("personOrMerchant", "Merchant"),
                            description = t.optString("description", ""),
                            paymentMethod = t.optString("paymentMethod", "UPI_ONLINE"),
                            date = t.optLong("date", System.currentTimeMillis()),
                            isRecurring = t.optBoolean("isRecurring", false)
                        )
                    )
                }
            }

            if (root.has("debts")) {
                val debtArr = root.getJSONArray("debts")
                for (i in 0 until debtArr.length()) {
                    val d = debtArr.getJSONObject(i)
                    dao.insertDebt(
                        DebtEntity(
                            personName = d.optString("personName", "Person"),
                            type = d.optString("type", "LENT"),
                            originalAmount = d.optDouble("originalAmount", 0.0),
                            paidAmount = d.optDouble("paidAmount", 0.0),
                            remainingAmount = d.optDouble("remainingAmount", 0.0),
                            dueDate = d.optLong("dueDate", System.currentTimeMillis()),
                            status = d.optString("status", "ACTIVE")
                        )
                    )
                }
            }

            if (root.has("reminders")) {
                val remArr = root.getJSONArray("reminders")
                for (i in 0 until remArr.length()) {
                    val r = remArr.getJSONObject(i)
                    dao.insertReminder(
                        ReminderEntity(
                            title = r.optString("title", "Bill"),
                            personOrCompany = r.optString("personOrCompany", ""),
                            amount = r.optDouble("amount", 0.0),
                            dueDate = r.optLong("dueDate", System.currentTimeMillis()),
                            isPaid = r.optBoolean("isPaid", false),
                            category = r.optString("category", "BILLS")
                        )
                    )
                }
            }

            if (root.has("goals")) {
                val goalArr = root.getJSONArray("goals")
                for (i in 0 until goalArr.length()) {
                    val g = goalArr.getJSONObject(i)
                    dao.insertGoal(
                        GoalEntity(
                            title = g.optString("title", "Goal"),
                            targetAmount = g.optDouble("targetAmount", 10000.0),
                            savedAmount = g.optDouble("savedAmount", 0.0),
                            targetDate = g.optLong("targetDate", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("assets_liabilities")) {
                val alArr = root.getJSONArray("assets_liabilities")
                for (i in 0 until alArr.length()) {
                    val al = alArr.getJSONObject(i)
                    dao.insertAssetLiability(
                        AssetLiabilityEntity(
                            title = al.optString("title", "Asset"),
                            type = al.optString("type", "ASSET"),
                            category = al.optString("category", "INVESTMENT"),
                            value = al.optDouble("value", 0.0)
                        )
                    )
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
