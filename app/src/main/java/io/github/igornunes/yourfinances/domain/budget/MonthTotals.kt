package io.github.igornunes.yourfinances.domain.budget

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType

/** Totals of a list of entries; [balanceCents] is what is left after expenses and net savings. */
data class MonthTotals(
    val incomeCents: Long = 0,
    val fixedExpensesCents: Long = 0,
    val flexibleExpensesCents: Long = 0,
    val investmentCents: Long = 0,
    val withdrawalsCents: Long = 0,
) {
    val expensesCents: Long get() = fixedExpensesCents + flexibleExpensesCents
    val balanceCents: Long get() = incomeCents + withdrawalsCents - expensesCents - investmentCents
}

/** Sums every entry given (the caller picks the month and the date basis); all statuses count. */
fun monthTotals(entries: List<Entry>): MonthTotals {
    fun total(type: EntryType) = entries.filter { it.type == type }.sumOf { it.amountCents }
    return MonthTotals(
        incomeCents = total(EntryType.INCOME),
        fixedExpensesCents = total(EntryType.FIXED_EXPENSE),
        flexibleExpensesCents = total(EntryType.FLEXIBLE_EXPENSE),
        investmentCents = total(EntryType.INVESTMENT),
        withdrawalsCents = total(EntryType.WITHDRAWAL),
    )
}
