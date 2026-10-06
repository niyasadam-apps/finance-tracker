package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.DebtStatus
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAccountInsertionAndQuery() = runBlocking {
        val accountDao = db.accountDao()
        val account = AccountEntity(
            name = "Test Bank",
            type = "BANK",
            balance = 50000.0,
            accountNumberLast4 = "1234"
        )
        val id = accountDao.insertAccount(account)
        val retrieved = accountDao.getAccountById(id)

        assertNotNull(retrieved)
        assertEquals("Test Bank", retrieved?.name)
        assertEquals(50000.0, retrieved?.balance ?: 0.0, 0.001)
    }

    @Test
    fun testTransactionAndFlow() = runBlocking {
        val txDao = db.transactionDao()
        val tx = TransactionEntity(
            amount = 1500.0,
            type = TransactionType.DEBIT.name,
            category = "FOOD",
            accountId = 1L,
            personOrMerchant = "Bistro",
            date = System.currentTimeMillis()
        )
        txDao.insertTransaction(tx)
        val allTx = txDao.getAllTransactions().first()

        assertEquals(1, allTx.size)
        assertEquals("Bistro", allTx.first().personOrMerchant)
        assertEquals(1500.0, allTx.first().amount, 0.001)
    }

    @Test
    fun testDebtOperations() = runBlocking {
        val debtDao = db.debtDao()
        val debt = DebtEntity(
            personName = "Alex",
            type = DebtType.LENT.name,
            originalAmount = 3000.0,
            remainingAmount = 3000.0,
            dueDate = System.currentTimeMillis() + 86400000L,
            status = DebtStatus.ACTIVE.name
        )
        val id = debtDao.insertDebt(debt)
        debtDao.updateDebtRepayment(id, paid = 1000.0, remaining = 2000.0, status = DebtStatus.PARTIALLY_PAID.name)

        val updated = debtDao.getDebtById(id)
        assertEquals(1000.0, updated?.paidAmount ?: 0.0, 0.001)
        assertEquals(2000.0, updated?.remainingAmount ?: 0.0, 0.001)
        assertEquals(DebtStatus.PARTIALLY_PAID.name, updated?.status)
    }

    @Test
    fun testGoalOperations() = runBlocking {
        val goalDao = db.goalDao()
        val goal = GoalEntity(
            title = "Laptop Fund",
            targetAmount = 75000.0,
            savedAmount = 25000.0,
            targetDate = System.currentTimeMillis() + 7 * 86400000L
        )
        val id = goalDao.insertGoal(goal)
        val retrieved = goalDao.getGoalById(id)

        assertEquals("Laptop Fund", retrieved?.title)
        assertEquals(75000.0, retrieved?.targetAmount ?: 0.0, 0.001)
        assertEquals(25000.0, retrieved?.savedAmount ?: 0.0, 0.001)
    }
}
