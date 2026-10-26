package io.github.igornunes.yourfinances.ui.installment

import io.github.igornunes.yourfinances.domain.installment.monthlyPaymentDates
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import io.github.igornunes.yourfinances.testing.FakeCreditCardRepository
import io.github.igornunes.yourfinances.testing.FakeInstallmentRepository
import io.github.igornunes.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class InstallmentViewModelsTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val today = LocalDate.of(2026, 3, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val repository = FakeInstallmentRepository()
    private val cards = FakeCreditCardRepository(listOf(CreditCard(id = 1, name = "Nubank", closingDay = 10, dueDay = 20)))

    @Test
    fun `form previews the split and saves monthly installments`() = runTest {
        val viewModel = InstallmentFormViewModel(repository, cards, clock)
        assertEquals(LocalDate.of(2026, 4, 15), viewModel.state.value.firstPaymentOn)

        viewModel.onTotalChange("100000")
        viewModel.onCountChange("3")
        assertEquals(listOf(33334L, 33333L, 33333L), viewModel.state.value.preview)
        viewModel.onDescriptionChange("Notebook")
        viewModel.save()

        val plan = repository.plans.value.single()
        assertEquals(EntryType.FIXED_EXPENSE, plan.purchase.type)
        assertEquals(100000L, plan.installments.sumOf { it.amountCents })
        assertEquals(
            listOf(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15)),
            plan.installments.map { it.paidOn },
        )
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `card purchase uses the due dates of consecutive invoices`() = runTest {
        val viewModel = InstallmentFormViewModel(repository, cards, clock)
        viewModel.onTotalChange("30000")
        viewModel.onCountChange("3")
        viewModel.onCardChange(1)
        // Today (15th) is after the closing day (10th): first invoice due April 20.
        assertEquals(
            listOf(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 20)),
            viewModel.state.value.paymentDates,
        )
        viewModel.save()

        val plan = repository.plans.value.single()
        assertEquals(1L, plan.purchase.creditCardId)
        assertEquals(viewModel.state.value.paymentDates, plan.installments.map { it.paidOn })
        assertTrue(plan.installments.all { it.creditCardId == 1L })
    }

    @Test
    fun `first payment follows the purchase date until edited`() {
        val viewModel = InstallmentFormViewModel(repository, cards, clock)
        viewModel.onPurchaseDateChange(LocalDate.of(2026, 1, 31))
        assertEquals(LocalDate.of(2026, 2, 28), viewModel.state.value.firstPaymentOn)

        viewModel.onFirstPaymentChange(LocalDate.of(2026, 2, 10))
        viewModel.onPurchaseDateChange(LocalDate.of(2026, 1, 20))
        assertEquals(LocalDate.of(2026, 2, 10), viewModel.state.value.firstPaymentOn)

        viewModel.onFirstPaymentChange(LocalDate.of(2026, 1, 10))
        assertTrue(viewModel.state.value.firstBeforePurchase)
    }

    @Test
    fun `count must be between 2 and the maximum`() {
        val viewModel = InstallmentFormViewModel(repository, cards, clock)
        viewModel.onTotalChange("1000")
        listOf("1", "0", "121", "").forEach { count ->
            viewModel.onCountChange(count)
            assertNull(viewModel.state.value.preview, "count '$count'")
            assertFalse(viewModel.state.value.canSave)
        }
        viewModel.onCountChange("120")
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `list shows commitments from the current month and splits active from finished`() = runTest {
        val old = InstallmentPurchase(description = "Antiga", totalCents = 2000, installmentCount = 2, purchasedOn = LocalDate.of(2025, 1, 5))
        repository.add(old, monthlyPaymentDates(LocalDate.of(2025, 2, 5), 2))
        val tv = InstallmentPurchase(description = "TV", totalCents = 3000, installmentCount = 3, purchasedOn = LocalDate.of(2026, 2, 20))
        repository.add(tv, monthlyPaymentDates(LocalDate.of(2026, 3, 10), 3))

        val viewModel = InstallmentListViewModel(repository, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        val state = viewModel.state.value

        assertEquals(listOf("TV"), state.active.map { it.purchase.description })
        assertEquals(listOf("Antiga"), state.finished.map { it.purchase.description })
        assertEquals(1, state.active.single().paidCount)
        // The March installment (10th) is already paid, so only April and May are still committed.
        assertEquals(listOf(YearMonth.of(2026, 4) to 1000L, YearMonth.of(2026, 5) to 1000L), state.committedByMonth)

        viewModel.delete(state.active.single().purchase.id)
        assertTrue(viewModel.state.value.active.isEmpty())
    }
}
