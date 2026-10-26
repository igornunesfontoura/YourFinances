package io.github.igornunes.yourfinances.domain.installment

import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class InstallmentsTest {
    private val purchase = InstallmentPurchase(
        id = 42,
        description = "Notebook",
        totalCents = 100000,
        installmentCount = 3,
        purchasedOn = LocalDate.of(2026, 1, 20),
    )

    @Test
    fun `installments add up to the total with the remainder on the first`() {
        val entries = purchase.toEntries(monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))
        assertEquals(listOf(33334L, 33333L, 33333L), entries.map { it.amountCents })
        assertEquals(100000L, entries.sumOf { it.amountCents })
    }

    @Test
    fun `installments are numbered and linked to the purchase`() {
        val entries = purchase.toEntries(monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))
        assertEquals(listOf(1, 2, 3), entries.map { it.installmentNumber })
        entries.forEach {
            assertEquals(42L, it.installmentPurchaseId)
            assertEquals(3, it.installmentCount)
            // Each installment accrues in the month it is due.
            assertEquals(it.paidOn, it.occurredOn)
        }
        assertEquals(
            listOf(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 10)),
            entries.map { it.paidOn },
        )
    }

    @Test
    fun `monthly dates from the 31st keep the 31st when the month allows`() {
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
            monthlyPaymentDates(LocalDate.of(2026, 1, 31), 3),
        )
    }

    @Test
    fun `card installments follow consecutive invoice due dates`() {
        val card = CreditCard(id = 9, name = "Card", closingDay = 25, dueDay = 5)
        val onCard = purchase.copy(creditCardId = 9, purchasedOn = LocalDate.of(2026, 11, 30))
        assertEquals(
            listOf(LocalDate.of(2027, 1, 5), LocalDate.of(2027, 2, 5), LocalDate.of(2027, 3, 5)),
            onCard.cardPaymentDates(card),
        )
    }

    @Test
    fun `payment dates must match the installment count`() {
        assertThrows<IllegalArgumentException> {
            purchase.toEntries(monthlyPaymentDates(LocalDate.of(2026, 2, 10), 2))
        }
    }

    @Test
    fun `end date is the last installment of each purchase`() {
        val first = purchase.toEntries(monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))
        val second = purchase.copy(id = 43, installmentCount = 2)
            .toEntries(monthlyPaymentDates(LocalDate.of(2026, 5, 1), 2))
        assertEquals(
            mapOf(42L to LocalDate.of(2026, 4, 10), 43L to LocalDate.of(2026, 6, 1)),
            installmentEndDates(first + second),
        )
    }

    @Test
    fun `committed amount per month sums installments from the given month on`() {
        val first = purchase.toEntries(monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))
        val second = purchase.copy(id = 43, totalCents = 5000, installmentCount = 2)
            .toEntries(monthlyPaymentDates(LocalDate.of(2026, 3, 1), 2))
        val notInstallment = Entry(
            type = purchase.type,
            amountCents = 999,
            description = "Bill",
            occurredOn = LocalDate.of(2026, 3, 1),
            paidOn = LocalDate.of(2026, 3, 1),
        )
        assertEquals(
            mapOf(
                YearMonth.of(2026, 3) to 33333L + 2500L,
                YearMonth.of(2026, 4) to 33333L + 2500L,
            ),
            committedInstallmentsByMonth(first + second + notInstallment, YearMonth.of(2026, 3)),
        )
    }

    @Test
    fun `pending amount per month leaves out what is already paid`() {
        val entries = purchase.toEntries(monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))
        assertEquals(
            mapOf(YearMonth.of(2026, 3) to 33333L, YearMonth.of(2026, 4) to 33333L),
            pendingInstallmentsByMonth(entries, LocalDate.of(2026, 2, 10)),
        )
        assertEquals(
            mapOf(YearMonth.of(2026, 2) to 33334L, YearMonth.of(2026, 3) to 33333L, YearMonth.of(2026, 4) to 33333L),
            pendingInstallmentsByMonth(entries, LocalDate.of(2026, 2, 9)),
        )
    }
}
