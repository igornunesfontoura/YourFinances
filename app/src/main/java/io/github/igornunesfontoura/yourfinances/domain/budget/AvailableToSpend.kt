package io.github.igornunesfontoura.yourfinances.domain.budget

import io.github.igornunesfontoura.yourfinances.domain.model.DateBasis
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import java.time.YearMonth

/**
 * "Quanto ainda posso gastar":
 * income + confirmed withdrawals − fixed expenses (predicted + confirmed)
 * − the larger of the investment goal and the month's contributions − flexible expenses.
 * [availableCents] may be negative when the month is overspent.
 */
data class AvailableToSpend(
    val month: YearMonth,
    val basis: DateBasis,
    val incomeCents: Long,
    /** Resgates already confirmed: money back from savings is available to spend again. */
    val withdrawalsCents: Long,
    val fixedExpensesCents: Long,
    val investmentGoalCents: Long,
    /** Aportes of the month, predicted ones included (a planned contribution is already set aside). */
    val contributionsCents: Long,
    val flexibleExpensesCents: Long,
) {
    /** The goal reserves money even before it is contributed; contributing more reserves more. */
    val reservedForSavingsCents: Long get() = maxOf(investmentGoalCents, contributionsCents)

    val availableCents: Long
        get() = incomeCents + withdrawalsCents - fixedExpensesCents - reservedForSavingsCents - flexibleExpensesCents
}

/**
 * Entries of every status count, except withdrawals: a predicted withdrawal has not brought any
 * money back yet.
 */
fun availableToSpend(
    entries: List<Entry>,
    month: YearMonth,
    investmentGoalCents: Long,
    basis: DateBasis,
): AvailableToSpend {
    require(investmentGoalCents >= 0) { "investmentGoalCents must not be negative" }
    val inMonth = entries.filter { YearMonth.from(basis.dateOf(it)) == month }
    fun total(type: EntryType, confirmedOnly: Boolean = false) = inMonth
        .filter { it.type == type && (!confirmedOnly || it.status == EntryStatus.CONFIRMED) }
        .sumOf { it.amountCents }
    return AvailableToSpend(
        month = month,
        basis = basis,
        incomeCents = total(EntryType.INCOME),
        withdrawalsCents = total(EntryType.WITHDRAWAL, confirmedOnly = true),
        fixedExpensesCents = total(EntryType.FIXED_EXPENSE),
        investmentGoalCents = investmentGoalCents,
        contributionsCents = total(EntryType.INVESTMENT),
        flexibleExpensesCents = total(EntryType.FLEXIBLE_EXPENSE),
    )
}
