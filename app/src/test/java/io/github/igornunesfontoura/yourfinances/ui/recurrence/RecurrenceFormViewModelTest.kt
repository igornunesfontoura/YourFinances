package io.github.igornunesfontoura.yourfinances.ui.recurrence

import io.github.igornunesfontoura.yourfinances.domain.model.Category
import io.github.igornunesfontoura.yourfinances.domain.model.CategoryKind
import io.github.igornunesfontoura.yourfinances.testing.FakeCategoryRepository
import io.github.igornunesfontoura.yourfinances.testing.FakeCreditCardRepository
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import io.github.igornunesfontoura.yourfinances.domain.recurrence.predictionWindow
import io.github.igornunesfontoura.yourfinances.testing.FakeRecurrenceRepository
import io.github.igornunesfontoura.yourfinances.testing.FakeSavingsRepository
import io.github.igornunesfontoura.yourfinances.domain.model.SavingsPocket
import io.github.igornunesfontoura.yourfinances.testing.MainDispatcherExtension
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
    private val categories = FakeCategoryRepository(
        listOf(
            Category(id = 11, name = "Mercado", kind = CategoryKind.EXPENSE),
            Category(id = 12, name = "Salário", kind = CategoryKind.INCOME),
        ),
    )

    @Test
    fun `defaults to a monthly fixed expense starting today on today's day`() {
        val state = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository()).state.value
        assertEquals(EntryType.FIXED_EXPENSE, state.type)
        assertEquals(Frequency.MONTHLY, state.frequency)
        assertEquals(15, state.dayOfMonth)
        assertEquals(today, state.startsOn)
        assertNull(state.endsOn)
        assertFalse(state.canSave)
    }

    @Test
    fun `creates the recurrence and predicts the current window`() = runTest {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
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
    fun `a twice monthly salary saves both payments`() = runTest {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
        viewModel.onTypeChange(EntryType.INCOME)
        viewModel.onFrequencyChange(Frequency.TWICE_MONTHLY)
        viewModel.onAmountChange("200000")
        viewModel.onDayChange("16")
        assertFalse(viewModel.state.value.canSave)
        viewModel.onSecondDayChange("31")
        assertFalse(viewModel.state.value.canSave)
        viewModel.onSecondAmountChange("300000")
        viewModel.save()

        val saved = repository.recurrences.value.single()
        assertEquals(Frequency.TWICE_MONTHLY, saved.frequency)
        assertEquals(16, saved.dayOfMonth)
        assertEquals(200000L, saved.amountCents)
        assertEquals(31, saved.secondDayOfMonth)
        assertEquals(300000L, saved.secondAmountCents)
    }

    @Test
    fun `second day not after the first blocks saving`() {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
        viewModel.onFrequencyChange(Frequency.TWICE_MONTHLY)
        viewModel.onAmountChange("100")
        viewModel.onSecondAmountChange("100")
        viewModel.onDayChange("16")
        viewModel.onSecondDayChange("10")
        assertTrue(viewModel.state.value.secondDayInvalid)
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `second day out of range shows even without a first day`() {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
        viewModel.onFrequencyChange(Frequency.TWICE_MONTHLY)
        viewModel.onDayChange("")
        viewModel.onSecondDayChange("45")
        assertTrue(viewModel.state.value.secondDayInvalid)
        viewModel.onSecondDayChange("20")
        assertFalse(viewModel.state.value.secondDayInvalid)
    }

    @Test
    fun `switching back to monthly drops the second payment`() = runTest {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
        viewModel.onFrequencyChange(Frequency.TWICE_MONTHLY)
        viewModel.onSecondDayChange("31")
        viewModel.onSecondAmountChange("300000")
        viewModel.onFrequencyChange(Frequency.MONTHLY)
        viewModel.onAmountChange("4990")
        viewModel.onDayChange("10")
        viewModel.save()

        val saved = repository.recurrences.value.single()
        assertEquals(null, saved.secondDayOfMonth)
        assertEquals(null, saved.secondAmountCents)
    }

    @Test
    fun `invalid day or end before start blocks saving`() {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
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
        val viewModel = RecurrenceFormViewModel(3, repository, clock, savings, categories, FakeCreditCardRepository())
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
        val viewModel = RecurrenceFormViewModel(3, repository, clock, savings, categories, FakeCreditCardRepository())
        viewModel.delete()
        assertTrue(repository.recurrences.value.isEmpty())
        assertEquals(predictionWindow(today), repository.windows.single())
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `failed save keeps the form open`() = runTest {
        repository.failWrites = true
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
        viewModel.onAmountChange("100")
        viewModel.save()
        assertTrue(viewModel.state.value.status.failed)
        assertFalse(viewModel.state.value.status.finished)
    }

    @Test
    fun `a recurring contribution keeps its pocket, other types drop it`() = runTest {
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, FakeCreditCardRepository())
        assertFalse(viewModel.state.value.showsPocket)
        viewModel.onTypeChange(EntryType.INVESTMENT)
        assertTrue(viewModel.state.value.showsPocket)
        viewModel.onPocketChange(2)
        viewModel.onAmountChange("50000")
        viewModel.save()
        assertEquals(2L, repository.recurrences.value.single().savingsPocketId)
    }

    @Test
    fun `a recurring expense can be charged to a card, with a category`() = runTest {
        val cardRepository = FakeCreditCardRepository(listOf(io.github.igornunesfontoura.yourfinances.domain.model.CreditCard(id = 5, name = "Nubank", closingDay = 25, dueDay = 5)))
        val viewModel = RecurrenceFormViewModel(null, repository, clock, savings, categories, cardRepository)
        assertTrue(viewModel.state.value.showsPayment)
        viewModel.onCardChange(5)
        viewModel.onCategoryChange(11)
        viewModel.onAmountChange("9990")
        viewModel.save()
        val saved = repository.recurrences.value.single()
        assertEquals(5L, saved.creditCardId)
        assertEquals(11L, saved.categoryId)

        val income = RecurrenceFormViewModel(null, repository, clock, savings, categories, cardRepository)
        income.onCardChange(5)
        income.onTypeChange(EntryType.INCOME)
        assertEquals(null, income.state.value.cardId)
        assertFalse(income.state.value.showsPayment)
    }
}
