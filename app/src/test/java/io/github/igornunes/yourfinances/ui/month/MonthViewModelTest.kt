package io.github.igornunes.yourfinances.ui.month

import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.testing.FakeEntryRepository
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
class MonthViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = Clock.fixed(LocalDate.of(2026, 3, 15).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun entry(id: Long, type: EntryType, amount: Long, occurredOn: LocalDate, paidOn: LocalDate = occurredOn) =
        Entry(id = id, type = type, amountCents = amount, description = "", occurredOn = occurredOn, paidOn = paidOn)

    private val repository = FakeEntryRepository(
        listOf(
            entry(1, EntryType.INCOME, 500000, LocalDate.of(2026, 3, 5)),
            entry(2, EntryType.FLEXIBLE_EXPENSE, 4000, LocalDate.of(2026, 3, 10)),
            entry(3, EntryType.FLEXIBLE_EXPENSE, 6000, LocalDate.of(2026, 3, 10)),
            // Card dinner: happened in March, paid in April.
            entry(4, EntryType.FLEXIBLE_EXPENSE, 20000, LocalDate.of(2026, 3, 28), LocalDate.of(2026, 4, 5)),
            entry(5, EntryType.FIXED_EXPENSE, 100000, LocalDate.of(2026, 4, 1)),
        ),
    )

    /** A subscriber keeps the WhileSubscribed state flowing. */
    private fun TestScope.subscribed(): MonthViewModel {
        val viewModel = MonthViewModel(repository, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun `starts on the current month by accrual date, newest day first`() = runTest {
        val state = subscribed().state.value
        assertFalse(state.loading)
        assertEquals(YearMonth.of(2026, 3), state.month)
        assertEquals(DateBasis.ACCRUAL, state.basis)
        assertEquals(
            listOf(LocalDate.of(2026, 3, 28), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 5)),
            state.days.map { it.date },
        )
        assertEquals(2, state.days[1].entries.size)
        assertEquals(500000L, state.totals.incomeCents)
        assertEquals(30000L, state.totals.expensesCents)
    }

    @Test
    fun `cash basis moves the card purchase to the payment month`() = runTest {
        val viewModel = subscribed()
        viewModel.setBasis(DateBasis.CASH)
        assertEquals(DateBasis.CASH, viewModel.state.value.basis)
        assertEquals(10000L, viewModel.state.value.totals.expensesCents)

        viewModel.nextMonth()
        assertEquals(YearMonth.of(2026, 4), viewModel.state.value.month)
        assertEquals(DateBasis.CASH, viewModel.state.value.basis)
        assertEquals(120000L, viewModel.state.value.totals.expensesCents)
    }

    @Test
    fun `navigates across the year and reacts to new entries`() = runTest {
        val viewModel = subscribed()
        repeat(3) { viewModel.previousMonth() }
        assertEquals(YearMonth.of(2025, 12), viewModel.state.value.month)
        assertEquals(0, viewModel.state.value.days.size)

        repository.add(entry(0, EntryType.INCOME, 1000, LocalDate.of(2025, 12, 24)))
        assertEquals(1000L, viewModel.state.value.totals.incomeCents)
    }

    @Test
    fun `confirming a predicted entry keeps its values`() = runTest {
        val predicted = entry(9, EntryType.FIXED_EXPENSE, 4990, LocalDate.of(2026, 3, 20))
            .copy(status = EntryStatus.PREDICTED, recurrenceId = 1, scheduledFor = LocalDate.of(2026, 3, 20))
        repository.add(predicted)
        val viewModel = subscribed()
        val stored = repository.entries.value.last()

        viewModel.confirm(stored)

        assertEquals(stored.copy(status = EntryStatus.CONFIRMED), repository.entries.value.last())
        assertFalse(viewModel.state.value.actionFailed)
    }

    @Test
    fun `failed confirmation is reported and can be dismissed`() = runTest {
        val viewModel = subscribed()
        repository.failWrites = true
        viewModel.confirm(repository.entries.value.first())
        assertTrue(viewModel.state.value.actionFailed)
        viewModel.dismissError()
        assertFalse(viewModel.state.value.actionFailed)
    }
}
