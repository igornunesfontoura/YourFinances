package io.github.igornunes.yourfinances.domain.budget

import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import java.time.YearMonth

/**
 * "Quanto ainda posso gastar":
 * income − fixed expenses (predicted + confirmed) − investment goal − flexible expenses.
 * [availableCents] may be negative when the month is overspent.
 */
data class AvailableToSpend(
    val month: YearMonth,
    val basis: DateBasis,
    val incomeCents: Long,
    val fixedExpensesCents: Long,
    val investmentGoalCents: Long,
    val flexibleExpensesCents: Long,
) {
    val availableCents: Long
        get() = incomeCents - fixedExpensesCents - investmentGoalCents - flexibleExpensesCents
}

/**
 * Entries of every status count. [EntryType.INVESTMENT] entries are ignored: the month reserves
 * [investmentGoalCents] instead.
 */
fun availableToSpend(
    entries: List<Entry>,
    month: YearMonth,
    investmentGoalCents: Long,
    basis: DateBasis,
): AvailableToSpend {
    require(investmentGoalCents >= 0) { "investmentGoalCents must not be negative" }
    val inMonth = entries.filter { YearMonth.from(basis.dateOf(it)) == month }
    fun total(type: EntryType) = inMonth.filter { it.type == type }.sumOf { it.amountCents }
    return AvailableToSpend(
        month = month,
        basis = basis,
        incomeCents = total(EntryType.INCOME),
        fixedExpensesCents = total(EntryType.FIXED_EXPENSE),
        investmentGoalCents = investmentGoalCents,
        flexibleExpensesCents = total(EntryType.FLEXIBLE_EXPENSE),
    )
}
