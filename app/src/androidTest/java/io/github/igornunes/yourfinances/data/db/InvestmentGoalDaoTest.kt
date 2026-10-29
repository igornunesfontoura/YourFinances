package io.github.igornunes.yourfinances.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.igornunes.yourfinances.data.repository.RoomInvestmentGoalRepository
import io.github.igornunes.yourfinances.domain.model.InvestmentGoal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class InvestmentGoalDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var goals: RoomInvestmentGoalRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        goals = RoomInvestmentGoalRepository(database.investmentGoalDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun goalsComeOldestFirstAndTheSameMonthIsReplaced() = runTest {
        // December before January checks the stored months sort across the year.
        goals.set(InvestmentGoal(YearMonth.of(2027, 1), 30000))
        goals.set(InvestmentGoal(YearMonth.of(2026, 12), 10000))
        goals.set(InvestmentGoal(YearMonth.of(2026, 12), 20000))

        assertEquals(
            listOf(InvestmentGoal(YearMonth.of(2026, 12), 20000), InvestmentGoal(YearMonth.of(2027, 1), 30000)),
            goals.observeAll().first(),
        )
    }
}
