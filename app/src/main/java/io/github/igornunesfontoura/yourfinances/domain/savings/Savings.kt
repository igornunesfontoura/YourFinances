package io.github.igornunesfontoura.yourfinances.domain.savings

import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.SavingsPocket
import java.time.LocalDate
import java.time.YearMonth

/**
 * What is set aside: contributions minus withdrawals. Only confirmed entries count, since a
 * predicted contribution has not happened yet. Other entry types are ignored. May be negative
 * when more was withdrawn than recorded as contributed.
 */
fun netSavedCents(entries: List<Entry>): Long = entries
    .filter { it.status == EntryStatus.CONFIRMED }
    .sumOf {
        when (it.type) {
            EntryType.INVESTMENT -> it.amountCents
            EntryType.WITHDRAWAL -> -it.amountCents
            else -> 0
        }
    }

/**
 * Net saved in [month] by accrual date, with the same rule as [savingsSummary]: confirmed entries
 * dated up to [today]. A month after today has saved nothing yet.
 */
fun savedInMonthCents(entries: List<Entry>, month: YearMonth, today: LocalDate): Long =
    netSavedCents(entries.filter { YearMonth.from(it.occurredOn) == month && !it.occurredOn.isAfter(today) })

/** [pocket] is null for the money set aside without a pocket. */
data class PocketBalance(val pocket: SavingsPocket?, val savedCents: Long) {
    /** Share of the pocket's target already reached, from 0 to 1; null without a target. */
    val progress: Float?
        get() = pocket?.targetCents?.let { target -> (savedCents.coerceAtLeast(0).toDouble() / target).coerceAtMost(1.0).toFloat() }
}

data class MonthSaved(val month: YearMonth, val savedCents: Long)

data class SavingsSummary(
    val totalCents: Long,
    /** Every pocket in the given order, then "sem caixinha" when anything is there. */
    val pockets: List<PocketBalance>,
    /** Newest month first. */
    val months: List<MonthSaved>,
)

/**
 * Balances up to [today] (entries dated later have not happened yet) and the net saved in each
 * of the [monthCount] months ending at today's month, by accrual date.
 */
fun savingsSummary(entries: List<Entry>, pockets: List<SavingsPocket>, today: LocalDate, monthCount: Int = 12): SavingsSummary {
    require(monthCount >= 0) { "monthCount must not be negative" }
    val upToToday = entries.filter { it.type.movesSavings && !it.occurredOn.isAfter(today) }
    val byPocket = upToToday.groupBy { it.savingsPocketId }
    val balances = pockets.map { PocketBalance(it, netSavedCents(byPocket[it.id].orEmpty())) }
    val known = pockets.map { it.id }.toSet()
    val unassigned = netSavedCents(upToToday.filter { it.savingsPocketId == null || it.savingsPocketId !in known })
    val current = YearMonth.from(today)
    val months = (0 until monthCount).map { back ->
        val month = current.minusMonths(back.toLong())
        MonthSaved(month, netSavedCents(upToToday.filter { YearMonth.from(it.occurredOn) == month }))
    }
    return SavingsSummary(
        totalCents = netSavedCents(upToToday),
        pockets = balances + listOfNotNull(PocketBalance(null, unassigned).takeIf { unassigned != 0L }),
        months = months,
    )
}
