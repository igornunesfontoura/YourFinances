package io.github.igornunes.yourfinances.domain.report

import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyEvolutionTest {
    private fun entry(type: EntryType, amount: Long, on: LocalDate, paid: LocalDate = on) =
        Entry(type = type, amountCents = amount, description = "", occurredOn = on, paidOn = paid)

    @Test
    fun `months ending at a month, oldest first, across the year`() {
        assertEquals(
            listOf(YearMonth.of(2025, 11), YearMonth.of(2025, 12), YearMonth.of(2026, 1)),
            monthsEndingAt(YearMonth.of(2026, 1), 3),
        )
    }

    @Test
    fun `totals per month by the chosen basis, empty months included`() {
        val entries = listOf(
            entry(EntryType.INCOME, 500000, LocalDate.of(2026, 1, 5)),
            entry(EntryType.FLEXIBLE_EXPENSE, 20000, LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 5)),
        )
        val months = monthsEndingAt(YearMonth.of(2026, 1), 3)
        val accrual = monthlyEvolution(entries, months, DateBasis.ACCRUAL)
        assertEquals(listOf(0L, 20000L, 0L), accrual.map { it.totals.expensesCents })
        assertEquals(listOf(0L, 0L, 500000L), accrual.map { it.totals.incomeCents })
        val cash = monthlyEvolution(entries, months, DateBasis.CASH)
        assertEquals(listOf(0L, 0L, 20000L), cash.map { it.totals.expensesCents })
    }
}
