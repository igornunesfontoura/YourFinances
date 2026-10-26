package io.github.igornunes.yourfinances.ui.card

import io.github.igornunes.yourfinances.domain.card.InvoiceStatus
import io.github.igornunes.yourfinances.domain.card.paymentDateFor
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.testing.FakeCreditCardRepository
import io.github.igornunes.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class CardViewModelsTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val today = LocalDate.of(2026, 3, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val nubank = CreditCard(id = 1, name = "Nubank", closingDay = 10, dueDay = 20)

    private fun purchase(id: Long, amount: Long, on: LocalDate) = Entry(
        id = id,
        type = EntryType.FLEXIBLE_EXPENSE,
        amountCents = amount,
        description = "x",
        occurredOn = on,
        paidOn = nubank.paymentDateFor(on),
        creditCardId = 1,
    )

    private fun TestScope.collect(flow: kotlinx.coroutines.flow.Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    @Test
    fun `form validates days and creates the card`() = runTest {
        val repository = FakeCreditCardRepository()
        val viewModel = CardFormViewModel(null, repository, clock)
        viewModel.onNameChange(" Nubank ")
        viewModel.onClosingChange("32")
        viewModel.onDueChange("20")
        assertFalse(viewModel.state.value.canSave)
        viewModel.onClosingChange("10")
        assertTrue(viewModel.state.value.canSave)
        viewModel.save()

        assertEquals(listOf(nubank), repository.cards.value)
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `editing passes today so pending entries can be rescheduled`() = runTest {
        val repository = FakeCreditCardRepository(listOf(nubank))
        val viewModel = CardFormViewModel(1, repository, clock)
        assertEquals("10", viewModel.state.value.closingText)
        viewModel.onDueChange("25")
        viewModel.save()
        assertEquals(nubank.copy(dueDay = 25), repository.cards.value.single())
        assertEquals(listOf(today), repository.updateDays)
    }

    @Test
    fun `card with entries is not deleted and the form says why`() = runTest {
        val repository = FakeCreditCardRepository(listOf(nubank))
        repository.entries.value = listOf(purchase(1, 1000, LocalDate.of(2026, 3, 1)))
        val viewModel = CardFormViewModel(1, repository, clock)
        viewModel.delete()
        assertTrue(viewModel.state.value.deleteBlocked)
        assertFalse(viewModel.state.value.status.finished)
        assertEquals(1, repository.cards.value.size)

        repository.entries.value = emptyList()
        viewModel.delete()
        assertTrue(viewModel.state.value.status.finished)
        assertTrue(repository.cards.value.isEmpty())
    }

    @Test
    fun `list shows the current invoice and the one waiting for payment`() = runTest {
        val repository = FakeCreditCardRepository(listOf(nubank))
        repository.entries.value = listOf(
            purchase(1, 1000, LocalDate.of(2026, 3, 5)), // closed Mar 10, due Mar 20
            purchase(2, 2500, LocalDate.of(2026, 3, 12)), // current invoice, due Apr 20
        )
        val viewModel = CardListViewModel(repository, clock)
        collect(viewModel.state)

        val overview = viewModel.state.value.cards.single()
        assertEquals(2500L, overview.current.totalCents)
        assertEquals(LocalDate.of(2026, 4, 20), overview.current.dueDate)
        assertEquals(1000L, overview.toPay!!.totalCents)
        assertEquals(InvoiceStatus.CLOSED, overview.toPay!!.status)
    }

    @Test
    fun `detail marks and unmarks an invoice as paid`() = runTest {
        val repository = FakeCreditCardRepository(listOf(nubank))
        repository.entries.value = listOf(purchase(1, 1000, LocalDate.of(2026, 3, 5)))
        val viewModel = CardDetailViewModel(1, repository, clock)
        collect(viewModel.state)

        val closed = viewModel.state.value.invoices.first { it.status == InvoiceStatus.CLOSED }
        viewModel.markPaid(closed)
        val paid = viewModel.state.value.invoices.first { it.dueDate == closed.dueDate }
        assertEquals(InvoiceStatus.PAID, paid.status)
        assertEquals(today, paid.paidOn)

        viewModel.unmarkPaid(paid)
        assertNull(viewModel.state.value.invoices.first { it.dueDate == closed.dueDate }.paidOn)
    }

    @Test
    fun `detail reports a deleted card as gone`() = runTest {
        val viewModel = CardDetailViewModel(1, FakeCreditCardRepository(), clock)
        collect(viewModel.state)
        assertTrue(viewModel.state.value.gone)
    }
}
