package io.github.igornunes.yourfinances.ui.home

import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.InvestmentGoal
import io.github.igornunes.yourfinances.testing.FakeEntryRepository
import io.github.igornunes.yourfinances.testing.FakeInvestmentGoalRepository
import io.github.igornunes.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = Clock.fixed(LocalDate.of(2026, 3, 15).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun entry(
        id: Long,
        type: EntryType,
        amount: Long,
        occurredOn: LocalDate,
        paidOn: LocalDate = occurredOn,
        status: EntryStatus = EntryStatus.CONFIRMED,
    ) = Entry(id = id, type = type, amountCents = amount, description = "", occurredOn = occurredOn, paidOn = paidOn, status = status)

    private val entries = FakeEntryRepository(
        listOf(
            entry(1, EntryType.INCOME, 500000, LocalDate.of(2026, 3, 5)),
            entry(2, EntryType.FIXED_EXPENSE, 150000, LocalDate.of(2026, 3, 10), status = EntryStatus.PREDICTED),
            entry(3, EntryType.FLEXIBLE_EXPENSE, 4000, LocalDate.of(2026, 3, 12)),
            // Card dinner: happened in March, paid in the April invoice.
            entry(4, EntryType.FLEXIBLE_EXPENSE, 20000, LocalDate.of(2026, 3, 28), LocalDate.of(2026, 4, 5)),
            entry(5, EntryType.INVESTMENT, 70000, LocalDate.of(2026, 3, 6)),
        ),
    )
    private val goals = FakeInvestmentGoalRepository(listOf(InvestmentGoal(YearMonth.of(2026, 3), 100000)))

    private fun TestScope.subscribed(): HomeViewModel {
        val viewModel = HomeViewModel(entries, goals, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun `current month by accrual, card purchases in the month they were made`() = runTest {
        val state = subscribed().state.value
        assertFalse(state.loading)
        assertEquals(YearMonth.of(2026, 3), state.month)
        assertEquals(DateBasis.ACCRUAL, state.available.basis)
        assertTrue(state.hasGoal)
        assertEquals(24000L, state.available.flexibleExpensesCents)
        assertEquals(500000L - 150000L - 100000L - 24000L, state.available.availableCents)
        // The contribution is compared with the goal, not subtracted again.
        assertEquals(70000L, state.savedCents)
    }

    @Test
    fun `goal in force carries into later months, none before it`() = runTest {
        val viewModel = subscribed()
        viewModel.nextMonth()
        assertEquals(YearMonth.of(2026, 4), viewModel.state.value.month)
        assertEquals(100000L, viewModel.state.value.available.investmentGoalCents)
        assertEquals(-100000L, viewModel.state.value.available.availableCents)

        repeat(2) { viewModel.previousMonth() }
        assertFalse(viewModel.state.value.hasGoal)
        assertEquals(0L, viewModel.state.value.available.investmentGoalCents)
    }

    @Test
    fun `reacts to new entries and goal changes`() = runTest {
        val viewModel = subscribed()
        entries.add(entry(0, EntryType.FLEXIBLE_EXPENSE, 1000, LocalDate.of(2026, 3, 20)))
        assertEquals(225000L, viewModel.state.value.available.availableCents)

        // A goal below what was already contributed (70000) reserves the contributions.
        goals.set(InvestmentGoal(YearMonth.of(2026, 3), 50000))
        assertEquals(500000L - 150000L - 70000L - 25000L, viewModel.state.value.available.availableCents)
    }

    @Test
    fun `without goals only the entries count`() = runTest {
        val viewModel = HomeViewModel(entries, FakeInvestmentGoalRepository(), clock)
        assertTrue(viewModel.state.value.loading)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertFalse(viewModel.state.value.hasGoal)
        // Without a goal, the contribution itself is what the month set aside.
        assertEquals(500000L - 150000L - 70000L - 24000L, viewModel.state.value.available.availableCents)
    }

    @Test
    fun `flags months after the prediction window, across the year`() = runTest {
        val viewModel = subscribed()
        assertFalse(viewModel.state.value.beyondPredictions)
        repeat(2) { viewModel.nextMonth() }
        assertFalse(viewModel.state.value.beyondPredictions)
        viewModel.nextMonth()
        assertTrue(viewModel.state.value.beyondPredictions)

        repeat(10) { viewModel.nextMonth() }
        assertEquals(YearMonth.of(2027, 4), viewModel.selectedMonth)
        assertEquals(YearMonth.of(2027, 4), viewModel.state.value.month)
        assertEquals(100000L, viewModel.state.value.available.investmentGoalCents)
    }

    @Test
    fun `saved this month only counts what already happened`() = runTest {
        val viewModel = subscribed()
        entries.add(entry(0, EntryType.INVESTMENT, 30000, LocalDate.of(2026, 3, 20)))
        assertEquals(70000L, viewModel.state.value.savedCents)
        // Still reserved in the month's budget, though: 70000 + 30000 = the 100000 goal.
        assertEquals(500000L - 150000L - 100000L - 24000L, viewModel.state.value.available.availableCents)
    }
}
