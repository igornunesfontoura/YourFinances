package io.github.igornunes.yourfinances.ui.entry

import app.cash.turbine.test
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.testing.FakeEntryRepository
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

    private val today = LocalDate.of(2026, 3, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Test
    fun `quick add saves a confirmed flexible expense for today`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, clock)

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
        assertTrue(viewModel.state.value.finished)
    }

    @Test
    fun `zero amount cannot be saved`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, clock)
        viewModel.onAmountChange("000")
        viewModel.save()
        assertTrue(repository.entries.value.isEmpty())
        assertFalse(viewModel.state.value.finished)
    }

    @Test
    fun `type, date and description are applied`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, clock)
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
        val viewModel = EntryFormViewModel(7, repository, clock)

        viewModel.state.test {
            val loaded = awaitItem()
            assertTrue(loaded.isEditing)
            assertFalse(loaded.loading)
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
        val viewModel = EntryFormViewModel(7, repository, clock)
        viewModel.delete()
        assertTrue(repository.entries.value.isEmpty())
        assertTrue(viewModel.state.value.finished)
    }

    @Test
    fun `missing entry closes the form`() = runTest {
        val viewModel = EntryFormViewModel(99, FakeEntryRepository(), clock)
        assertTrue(viewModel.state.value.finished)
    }

    @Test
    fun `saving twice creates a single entry`() = runTest {
        val repository = FakeEntryRepository()
        val viewModel = EntryFormViewModel(null, repository, clock)
        viewModel.onAmountChange("1500")
        viewModel.save()
        viewModel.save()
        assertEquals(1, repository.entries.value.size)
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `failed save keeps the form open and allows retrying`() = runTest {
        val repository = FakeEntryRepository().apply { failWrites = true }
        val viewModel = EntryFormViewModel(null, repository, clock)
        viewModel.onAmountChange("1500")
        viewModel.save()
        assertTrue(viewModel.state.value.failed)
        assertFalse(viewModel.state.value.finished)
        assertTrue(viewModel.state.value.canSave)

        repository.failWrites = false
        viewModel.save()
        assertEquals(1, repository.entries.value.size)
        assertTrue(viewModel.state.value.finished)
    }

    @Test
    fun `installment cannot be deleted or have its values changed`() = runTest {
        val installment = Entry(
            id = 7,
            type = EntryType.FIXED_EXPENSE,
            amountCents = 1000,
            description = "TV",
            occurredOn = today,
            paidOn = today.plusMonths(2),
            installmentPurchaseId = 3,
            installmentNumber = 3,
            installmentCount = 5,
        )
        val repository = FakeEntryRepository(listOf(installment))
        val viewModel = EntryFormViewModel(7, repository, clock)
        assertFalse(viewModel.state.value.canEditValues)
        assertFalse(viewModel.state.value.canDelete)

        viewModel.delete()
        assertEquals(1, repository.entries.value.size)

        viewModel.onAmountChange("999999")
        viewModel.onDescriptionChange("TV da sala")
        viewModel.save()
        assertEquals(installment.copy(description = "TV da sala"), repository.entries.value.single())
    }
}
