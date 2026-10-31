package io.github.igornunesfontoura.yourfinances.domain.installment

import io.github.igornunesfontoura.yourfinances.domain.model.InstallmentPurchase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstallmentPlanTest {
    private val purchase = InstallmentPurchase(
        id = 1,
        description = "Sofá",
        totalCents = 100000,
        installmentCount = 3,
        purchasedOn = LocalDate.of(2026, 1, 31),
    )
    private val installments = purchase.toEntries(monthlyPaymentDates(LocalDate.of(2026, 2, 28), 3))

    @Test
    fun `first payment defaults to one month after the purchase`() {
        assertEquals(LocalDate.of(2026, 2, 28), defaultFirstPayment(LocalDate.of(2026, 1, 31)))
        assertEquals(LocalDate.of(2027, 1, 15), defaultFirstPayment(LocalDate.of(2026, 12, 15)))
    }

    @Test
    fun `nothing paid before the first due date`() {
        val progress = InstallmentPlan(purchase, installments).progress(LocalDate.of(2026, 2, 27))
        assertEquals(0, progress.paidCount)
        assertEquals(100000L, progress.remainingCents)
        assertEquals(LocalDate.of(2026, 4, 28), progress.endsOn)
        assertFalse(progress.isFinished)
    }

    @Test
    fun `installment due today counts as paid`() {
        val progress = InstallmentPlan(purchase, installments).progress(LocalDate.of(2026, 2, 28))
        assertEquals(1, progress.paidCount)
        assertEquals(66666L, progress.remainingCents)
    }

    @Test
    fun `finished after the last due date`() {
        val progress = InstallmentPlan(purchase, installments).progress(LocalDate.of(2026, 4, 28))
        assertEquals(3, progress.paidCount)
        assertEquals(0L, progress.remainingCents)
        assertTrue(progress.isFinished)
    }

    @Test
    fun `purchase without installments has no end date`() {
        assertNull(InstallmentPlan(purchase, emptyList()).progress(LocalDate.of(2026, 3, 1)).endsOn)
    }

    @Test
    fun `paid share counts installments paid so far`() {
        assertEquals(0f, purchase.progress(installments, LocalDate.of(2026, 1, 1)).paidShare)
        assertEquals(1f / 3, purchase.progress(installments, installments[0].paidOn).paidShare)
        assertEquals(1f, purchase.progress(installments, LocalDate.of(2027, 1, 1)).paidShare)
    }
}
