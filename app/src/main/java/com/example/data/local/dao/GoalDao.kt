package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY targetDate ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Query("SELECT SUM(savedAmount) FROM goals")
    fun getTotalSavedInGoalsFlow(): Flow<Double?>

    @Query("SELECT SUM(targetAmount) FROM goals")
    fun getTotalTargetInGoalsFlow(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>): List<Long>

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET savedAmount = :savedAmount WHERE id = :id")
    suspend fun updateGoalSavings(id: Long, savedAmount: Double)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    @Query("DELETE FROM goals")
    suspend fun clearAll()
}
