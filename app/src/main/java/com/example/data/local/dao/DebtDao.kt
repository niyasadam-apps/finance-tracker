package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts ORDER BY dueDate ASC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE type = :type ORDER BY dueDate ASC")
    fun getDebtsByType(type: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE status = :status ORDER BY dueDate ASC")
    fun getDebtsByStatus(status: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Query("SELECT SUM(remainingAmount) FROM debts WHERE type = 'LENT' AND status != 'SETTLED'")
    fun getTotalReceivableFlow(): Flow<Double?>

    @Query("SELECT SUM(remainingAmount) FROM debts WHERE type = 'BORROWED' AND status != 'SETTLED'")
    fun getTotalPayableFlow(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>): List<Long>

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("UPDATE debts SET paidAmount = :paid, remainingAmount = :remaining, status = :status WHERE id = :id")
    suspend fun updateDebtRepayment(id: Long, paid: Double, remaining: Double, status: String)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Query("DELETE FROM debts")
    suspend fun clearAll()
}
