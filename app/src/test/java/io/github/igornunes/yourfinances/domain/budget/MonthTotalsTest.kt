package io.github.igornunes.yourfinances.domain.budget

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MonthTotalsTest {
    private fun entry(type: EntryType, amount: Long, status: EntryStatus = EntryStatus.CONFIRMED) = Entry(
        type = type,
        amountCents = amount,
        description = "",
        occurredOn = LocalDate.of(2026, 3, 1),
        paidOn = LocalDate.of(2026, 3, 1),
        status = status,
    )

    @Test
    fun `sums each type and computes the balance`() {
        val totals = monthTotals(
            listOf(
                entry(EntryType.INCOME, 500000),
                entry(EntryType.FIXED_EXPENSE, 100000),
                entry(EntryType.FIXED_EXPENSE, 20000, EntryStatus.PREDICTED),
                entry(EntryType.FLEXIBLE_EXPENSE, 3050),
                entry(EntryType.INVESTMENT, 50000),
            ),
        )
        assertEquals(500000L, totals.incomeCents)
        assertEquals(120000L, totals.fixedExpensesCents)
        assertEquals(3050L, totals.flexibleExpensesCents)
        assertEquals(123050L, totals.expensesCents)
        assertEquals(50000L, totals.investmentCents)
        assertEquals(500000L - 123050L - 50000L, totals.balanceCents)
    }

    @Test
    fun `no entries means zero`() {
        assertEquals(MonthTotals(), monthTotals(emptyList()))
        assertEquals(0L, MonthTotals().balanceCents)
    }
}
