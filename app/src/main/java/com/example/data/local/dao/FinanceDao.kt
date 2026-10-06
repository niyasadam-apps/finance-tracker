package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- ACCOUNTS ---
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :id")
    suspend fun updateAccountBalance(id: Long, newBalance: Double)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int


    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    fun getTransactionsBetweenDates(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM transactions WHERE isRecurring = 1")
    fun getRecurringTransactions(): Flow<List<TransactionEntity>>


    // --- DEBTS (PEOPLE & LENT/BORROWED) ---
    @Query("SELECT * FROM debts ORDER BY dueDate ASC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE type = :type ORDER BY dueDate ASC")
    fun getDebtsByType(type: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)


    // --- PAYMENT REMINDERS ---
    @Query("SELECT * FROM reminders ORDER BY isPaid ASC, dueDate ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getPendingReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)


    // --- SAVINGS GOALS ---
    @Query("SELECT * FROM goals ORDER BY targetDate ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)


    // --- ASSETS & LIABILITIES ---
    @Query("SELECT * FROM assets_liabilities ORDER BY createdAt DESC")
    fun getAllAssetsLiabilities(): Flow<List<AssetLiabilityEntity>>

    @Query("SELECT * FROM assets_liabilities WHERE type = :type ORDER BY createdAt DESC")
    fun getAssetsLiabilitiesByType(type: String): Flow<List<AssetLiabilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetLiability(item: AssetLiabilityEntity): Long

    @Update
    suspend fun updateAssetLiability(item: AssetLiabilityEntity)

    @Delete
    suspend fun deleteAssetLiability(item: AssetLiabilityEntity)

    @Query("DELETE FROM assets_liabilities WHERE id = :id")
    suspend fun deleteAssetLiabilityById(id: Long)


    // --- USER PROFILE ---
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    // --- RESET / PURGE ALL DATA ---
    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM debts")
    suspend fun clearDebts()

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM assets_liabilities")
    suspend fun clearAssetsLiabilities()
}
