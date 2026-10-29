package io.github.igornunes.yourfinances.ui.recurrence

import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import io.github.igornunes.yourfinances.domain.recurrence.predictionWindow
import io.github.igornunes.yourfinances.testing.FakeRecurrenceRepository
import io.github.igornunes.yourfinances.testing.FakeSavingsRepository
import io.github.igornunes.yourfinances.domain.model.SavingsPocket
import io.github.igornunes.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class RecurrenceFormViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val today = LocalDate.of(2026, 3, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val repository = FakeRecurrenceRepository()
    private val savings = FakeSavingsRepository(listOf(SavingsPocket(id = 2, name = "Reserva")))

    @Test
    fun `defaults to a monthly fixed expense starting today on today's day`() {
        val state = RecurrenceFormViewModel(null, repository, clock, savings).state.value
        assertEquals(EntryType.FIXED_EXPENSE, state.type)
        assertEquals(Frequency.MONTHLY, state.frequency)
        assertEquals(15, state.dayOfMonth)
        assertEquals(today, state.startsOn)
        assertNull(state.endsOn)
        assertFalse(state.canSave)
    }

    @Test
    fun `creates the recurrence and predicts the current window`() = runTest {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings)
        viewModel.onAmountChange("4990")
        viewModel.onDescriptionChange(" Streaming ")
        viewModel.onDayChange("31")
        viewModel.save()

        val saved = repository.recurrences.value.single()
        assertEquals(4990L, saved.amountCents)
        assertEquals("Streaming", saved.description)
        assertEquals(31, saved.dayOfMonth)
        assertEquals(predictionWindow(today), repository.windows.single())
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `invalid day or end before start blocks saving`() {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings)
        viewModel.onAmountChange("100")
        listOf("0", "32", "").forEach { day ->
            viewModel.onDayChange(day)
            assertFalse(viewModel.state.value.canSave, "day '$day'")
        }
        viewModel.onDayChange("5")
        assertTrue(viewModel.state.value.canSave)

        viewModel.onEndChange(today.minusDays(1))
        assertTrue(viewModel.state.value.endsBeforeStart)
        assertFalse(viewModel.state.value.canSave)
        viewModel.onEndChange(null)
        assertTrue(viewModel.state.value.canSave)
    }

    private val stored = Recurrence(
        id = 3,
        type = EntryType.INCOME,
        amountCents = 500000,
        description = "Salário",
        frequency = Frequency.MONTHLY,
        dayOfMonth = 5,
        startsOn = LocalDate.of(2025, 1, 1),
        categoryId = 8,
    )

    @Test
    fun `editing updates the template and keeps its category`() = runTest {
        val repository = FakeRecurrenceRepository(listOf(stored))
        val viewModel = RecurrenceFormViewModel(3, repository, clock, savings)
        assertEquals("500000", viewModel.state.value.amountDigits)
        assertEquals("5", viewModel.state.value.dayText)

        viewModel.onAmountChange("550000")
        viewModel.onEndChange(LocalDate.of(2026, 12, 31))
        viewModel.save()

        assertEquals(
            stored.copy(amountCents = 550000, endsOn = LocalDate.of(2026, 12, 31)),
            repository.recurrences.value.single(),
        )
        assertEquals(predictionWindow(today), repository.windows.single())
    }

    @Test
    fun `deleting removes the template`() = runTest {
        val repository = FakeRecurrenceRepository(listOf(stored))
        val viewModel = RecurrenceFormViewModel(3, repository, clock, savings)
        viewModel.delete()
        assertTrue(repository.recurrences.value.isEmpty())
        assertEquals(predictionWindow(today), repository.windows.single())
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `failed save keeps the form open`() = runTest {
        repository.failWrites = true
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings)
        viewModel.onAmountChange("100")
        viewModel.save()
        assertTrue(viewModel.state.value.status.failed)
        assertFalse(viewModel.state.value.status.finished)
    }

    @Test
    fun `a recurring contribution keeps its pocket, other types drop it`() = runTest {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings)
        assertFalse(viewModel.state.value.showsPocket)
        viewModel.onTypeChange(EntryType.INVESTMENT)
        assertTrue(viewModel.state.value.showsPocket)
        viewModel.onPocketChange(2)
        viewModel.onAmountChange("50000")
        viewModel.save()
        assertEquals(2L, repository.recurrences.value.single().savingsPocketId)
    }
}
