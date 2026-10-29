package io.github.igornunes.yourfinances.domain.report

import io.github.igornunes.yourfinances.domain.budget.MonthTotals
import io.github.igornunes.yourfinances.domain.budget.monthTotals
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import java.time.YearMonth

data class MonthEvolution(val month: YearMonth, val totals: MonthTotals)

/** The [count] months ending at [last], oldest first: what charts plot left to right. */
fun monthsEndingAt(last: YearMonth, count: Int): List<YearMonth> {
    require(count >= 0) { "count must not be negative" }
    return (count - 1 downTo 0).map { last.minusMonths(it.toLong()) }
}

/** Totals of each of [months], by [basis]; all statuses count, as in the month list. */
fun monthlyEvolution(entries: List<Entry>, months: List<YearMonth>, basis: DateBasis): List<MonthEvolution> {
    val byMonth = entries.groupBy { YearMonth.from(basis.dateOf(it)) }
    return months.map { MonthEvolution(it, monthTotals(byMonth[it].orEmpty())) }
}
