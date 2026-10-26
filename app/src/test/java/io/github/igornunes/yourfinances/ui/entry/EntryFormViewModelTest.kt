package io.github.igornunes.yourfinances.ui.entry

import app.cash.turbine.test
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.installment.monthlyPaymentDates
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import io.github.igornunes.yourfinances.domain.recurrence.predictionWindow
import io.github.igornunes.yourfinances.testing.FakeEntryRepository
import io.github.igornunes.yourfinances.testing.FakeInstallmentRepository
import io.github.igornunes.yourfinances.testing.FakeRecurrenceRepository
import io.github.igornunes.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class EntryFormViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val recurrences = FakeRecurrenceRepository()
    private val installments = FakeInstallmentRepository()

    private val today = LocalDate.of(2026, 3, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Test
    fun `quick add saves a confirmed flexible expense for today`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, recurrences, installments, clock)

        assertFalse(viewModel.state.value.canSave)
        viewModel.onAmountChange("4590")
        assertEquals(4590L, viewModel.state.value.amountCents)
        viewModel.save()

        val saved = repository.entries.value.single()
        assertEquals(EntryType.FLEXIBLE_EXPENSE, saved.type)
        assertEquals(4590L, saved.amountCents)
        assertEquals(today, saved.occurredOn)
        assertEquals(today, saved.paidOn)
        assertEquals(EntryStatus.CONFIRMED, saved.status)
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `zero amount cannot be saved`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, recurrences, installments, clock)
        viewModel.onAmountChange("000")
        viewModel.save()
        assertTrue(repository.entries.value.isEmpty())
        assertFalse(viewModel.state.value.status.finished)
    }

    @Test
    fun `type, date and description are applied`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, recurrences, installments, clock)
        viewModel.onAmountChange("500000")
        viewModel.onTypeChange(EntryType.INCOME)
        viewModel.onDateChange(LocalDate.of(2026, 3, 5))
        viewModel.onDescriptionChange(" Salário ")
        viewModel.save()

        val saved = repository.entries.value.single()
        assertEquals(EntryType.INCOME, saved.type)
        assertEquals(LocalDate.of(2026, 3, 5), saved.occurredOn)
        assertEquals("Salário", saved.description)
    }

    @Test
    fun `editing loads the entry and updates it in place`() = runTest {
        val existing = Entry(
            id = 7,
            type = EntryType.FIXED_EXPENSE,
            amountCents = 12990,
            description = "Internet",
            occurredOn = LocalDate.of(2026, 3, 10),
            paidOn = LocalDate.of(2026, 3, 10),
        )
        val repository = FakeEntryRepository(listOf(existing))
        val viewModel = EntryFormViewModel(7, repository, recurrences, installments, clock)

        viewModel.state.test {
            val loaded = awaitItem()
            assertTrue(loaded.isEditing)
            assertFalse(loaded.status.loading)
            assertEquals("12990", loaded.amountDigits)
            assertEquals(EntryType.FIXED_EXPENSE, loaded.type)
            assertEquals("Internet", loaded.description)
        }

        viewModel.onAmountChange("13990")
        viewModel.onDateChange(LocalDate.of(2026, 3, 12))
        viewModel.save()

        val updated = repository.entries.value.single()
        assertEquals(7L, updated.id)
        assertEquals(13990L, updated.amountCents)
        assertEquals(LocalDate.of(2026, 3, 12), updated.paidOn)
    }

    @Test
    fun `deleting removes the entry`() = runTest {
        val existing = Entry(
            id = 7,
            type = EntryType.FLEXIBLE_EXPENSE,
            amountCents = 100,
            description = "",
            occurredOn = today,
            paidOn = today,
        )
        val repository = FakeEntryRepository(listOf(existing))
        val viewModel = EntryFormViewModel(7, repository, recurrences, installments, clock)
        viewModel.delete()
        assertTrue(repository.entries.value.isEmpty())
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `missing entry closes the form`() = runTest {
        val viewModel = EntryFormViewModel(99, FakeEntryRepository(), recurrences, installments, clock)
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `saving twice creates a single entry`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, recurrences, installments, clock)
        viewModel.onAmountChange("1500")
        viewModel.save()
        viewModel.save()
        assertEquals(1, repository.entries.value.size)
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `failed save keeps the form open and allows retrying`() = runTest {
        val repository = FakeEntryRepository().apply { failWrites = true }
        val viewModel = EntryFormViewModel(null, repository, recurrences, installments, clock)
        viewModel.onAmountChange("1500")
        viewModel.save()
        assertTrue(viewModel.state.value.status.failed)
        assertFalse(viewModel.state.value.status.finished)
        assertTrue(viewModel.state.value.canSave)

        repository.failWrites = false
        viewModel.save()
        assertEquals(1, repository.entries.value.size)
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `installment keeps its values and deleting it deletes the whole purchase`() = runTest {
        val purchase = InstallmentPurchase(id = 3, description = "TV", totalCents = 3000, installmentCount = 3, purchasedOn = today)
        installments.add(purchase, monthlyPaymentDates(today, 3))
        val installment = installments.plans.value.single().installments[1].copy(id = 7)
        val repository = FakeEntryRepository(listOf(installment))
        val viewModel = EntryFormViewModel(7, repository, recurrences, installments, clock)
        assertFalse(viewModel.state.value.canEditValues)
        assertTrue(viewModel.state.value.deletesWholePurchase)

        viewModel.onAmountChange("999999")
        viewModel.onDescriptionChange("TV da sala")
        viewModel.save()
        assertEquals(installment.copy(description = "TV da sala"), repository.entries.value.single())

        val reopened = EntryFormViewModel(7, repository, recurrences, installments, clock)
        reopened.delete()
        assertTrue(installments.plans.value.isEmpty())
    }

    private val predicted = Entry(
        id = 9,
        type = EntryType.FIXED_EXPENSE,
        amountCents = 15000,
        description = "Luz",
        occurredOn = LocalDate.of(2026, 3, 10),
        paidOn = LocalDate.of(2026, 3, 10),
        status = EntryStatus.PREDICTED,
        recurrenceId = 4,
        scheduledFor = LocalDate.of(2026, 3, 10),
    )

    @Test
    fun `adjusting a predicted entry confirms only that occurrence by default`() = runTest {
        val repository = FakeEntryRepository(listOf(predicted))
        val viewModel = EntryFormViewModel(9, repository, recurrences, installments, clock)
        assertTrue(viewModel.state.value.isRecurring)

        viewModel.onAmountChange("17890")
        viewModel.save()

        val saved = repository.entries.value.single()
        assertEquals(17890L, saved.amountCents)
        assertEquals(EntryStatus.CONFIRMED, saved.status)
        assertTrue(recurrences.appliedEntries.isEmpty())
    }

    @Test
    fun `apply to upcoming hands the edited occurrence to the recurrence`() = runTest {
        val repository = FakeEntryRepository(listOf(predicted))
        val viewModel = EntryFormViewModel(9, repository, recurrences, installments, clock)
        viewModel.onAmountChange("17890")
        viewModel.onApplyToUpcomingChange(true)
        viewModel.save()

        val applied = recurrences.appliedEntries.single()
        assertEquals(17890L, applied.amountCents)
        assertEquals(EntryStatus.CONFIRMED, applied.status)
        assertEquals(predictionWindow(today), recurrences.windows.single())
        assertTrue(viewModel.state.value.status.finished)
    }
}
