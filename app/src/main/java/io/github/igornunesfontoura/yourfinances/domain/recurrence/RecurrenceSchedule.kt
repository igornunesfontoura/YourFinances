package io.github.igornunesfontoura.yourfinances.domain.recurrence

import io.github.igornunesfontoura.yourfinances.domain.card.paymentDateFor
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import io.github.igornunesfontoura.yourfinances.domain.time.atDayClamped
import java.time.LocalDate
import java.time.YearMonth

/** Occurrence dates of the recurrence within [from]..[to] (inclusive). */
fun Recurrence.occurrencesBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
    val start = maxOf(from, startsOn)
    val end = endsOn?.let { minOf(to, it) } ?: to
    if (start.isAfter(end)) return emptyList()

    val dates = mutableListOf<LocalDate>()
    var month = YearMonth.from(start)
    val lastMonth = YearMonth.from(end)
    while (!month.isAfter(lastMonth)) {
        if (frequency != Frequency.YEARLY || month.month == startsOn.month) {
            datesIn(month).filterTo(dates) { !it.isBefore(start) && !it.isAfter(end) }
        }
        month = month.plusMonths(1)
    }
    return dates
}

/**
 * The payment dates of [month]. When both twice-monthly days clamp to the same last day, the first
 * payment moves one day earlier so each keeps its own slot.
 */
private fun Recurrence.datesIn(month: YearMonth): List<LocalDate> {
    val first = month.atDayClamped(dayOfMonth)
    val secondDay = secondDayOfMonth ?: return listOf(first)
    val second = month.atDayClamped(secondDay)
    return listOf(if (first == second) first.minusDays(1) else first, second)
}

/**
 * Which payment of the month [date] is: 0 for the first, 1 for the second (twice monthly only).
 * Slots sit exactly on a payment date, since [carrySlotsFrom] moves them whenever the days change;
 * any other date belongs to the nearer payment.
 */
fun Recurrence.paymentOf(date: LocalDate): Int {
    if (frequency != Frequency.TWICE_MONTHLY) return 0
    val (first, second) = datesIn(YearMonth.from(date))
    return if (date.dayOfMonth * 2 <= first.dayOfMonth + second.dayOfMonth) 0 else 1
}

/**
 * Where the slots used under [previous] go when the schedule becomes this one: [moved] maps old
 * slots to new ones and [skipped] holds extra slots to mark as skipped.
 */
data class SlotMoves(val moved: Map<LocalDate, LocalDate>, val skipped: Set<LocalDate>)

/**
 * Keeps each used slot naming the payment it was made for when a twice-monthly schedule changes
 * its days, or a recurrence becomes twice monthly. Within a month the slots keep their order (the
 * earlier is the first payment); a lone slot keeps the payment it had under [previous]. A month
 * paid once under a monthly or yearly schedule counts as fully paid, so its second payment is
 * skipped. Other changes move nothing: monthly and yearly slots count for their whole period.
 */
fun Recurrence.carrySlotsFrom(previous: Recurrence, slots: Collection<LocalDate>): SlotMoves {
    val sameSchedule = previous.frequency == frequency && previous.dayOfMonth == dayOfMonth &&
        previous.secondDayOfMonth == secondDayOfMonth
    if (frequency != Frequency.TWICE_MONTHLY || sameSchedule) return SlotMoves(emptyMap(), emptySet())
    val moved = mutableMapOf<LocalDate, LocalDate>()
    val skipped = mutableSetOf<LocalDate>()
    slots.distinct().groupBy { YearMonth.from(it) }.forEach { (month, inMonth) ->
        val targets = datesIn(month)
        val sorted = inMonth.sorted()
        when {
            sorted.size >= 2 -> {
                moved[sorted[0]] = targets[0]
                moved[sorted[1]] = targets[1]
            }
            previous.frequency == Frequency.TWICE_MONTHLY -> moved[sorted[0]] = targets[previous.paymentOf(sorted[0])]
            else -> {
                moved[sorted[0]] = targets[0]
                skipped += targets[1]
            }
        }
    }
    return SlotMoves(moved.filter { (from, to) -> from != to }, skipped)
}

/** The amount of the payment due on [date]. */
fun Recurrence.amountOn(date: LocalDate): Long =
    if (paymentOf(date) == 1) requireNotNull(secondAmountCents) else amountCents

/**
 * Predicted entries for occurrences within [from]..[to].
 *
 * [alreadyScheduled] holds the slots this recurrence already used: those of generated entries and
 * those the user skipped. An occurrence is dropped when its period (the month, each half of it for
 * twice-monthly recurrences, or the year for yearly ones) already has a slot, so changing [Recurrence.dayOfMonth] never generates a
 * second entry for the same period. For yearly recurrences only slots in the month of
 * [Recurrence.startsOn] count: slots left by a former monthly schedule do not use up the year.
 *
 * [card] must be the recurrence's card when it has one: each entry is then paid on the due date of
 * the invoice its occurrence falls into.
 */
fun Recurrence.predictedEntriesBetween(
    from: LocalDate,
    to: LocalDate,
    alreadyScheduled: Set<LocalDate> = emptySet(),
    card: CreditCard? = null,
): List<Entry> {
    require(card?.id == creditCardId) { "card does not match the recurrence" }
    val usedPeriods = alreadyScheduled
        .filter { frequency != Frequency.YEARLY || it.month == startsOn.month }
        .map { periodOf(it) }
        .toSet()
    return occurrencesBetween(from, to)
        .filterNot { periodOf(it) in usedPeriods }
        .map { date ->
            Entry(
                type = type,
                amountCents = amountOn(date),
                description = description,
                occurredOn = date,
                paidOn = card?.paymentDateFor(date) ?: date,
                status = EntryStatus.PREDICTED,
                categoryId = categoryId,
                recurrenceId = id,
                scheduledFor = date,
                savingsPocketId = savingsPocketId,
                creditCardId = creditCardId,
            )
        }
}

private fun Recurrence.periodOf(date: LocalDate): Int = when (frequency) {
    Frequency.MONTHLY -> date.year * 12 + date.monthValue
    Frequency.TWICE_MONTHLY -> (date.year * 12 + date.monthValue) * 2 + paymentOf(date)
    Frequency.YEARLY -> date.year
}
