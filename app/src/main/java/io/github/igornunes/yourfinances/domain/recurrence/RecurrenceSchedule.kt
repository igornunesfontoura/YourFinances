package io.github.igornunes.yourfinances.domain.recurrence

import io.github.igornunes.yourfinances.domain.card.paymentDateFor
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import io.github.igornunes.yourfinances.domain.time.atDayClamped
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
        if (frequency == Frequency.MONTHLY || month.month == startsOn.month) {
            val date = month.atDayClamped(dayOfMonth)
            if (!date.isBefore(start) && !date.isAfter(end)) dates += date
        }
        month = month.plusMonths(1)
    }
    return dates
}

/**
 * Predicted entries for occurrences within [from]..[to].
 *
 * [alreadyScheduled] holds the slots this recurrence already used: those of generated entries and
 * those the user skipped. An occurrence is dropped when its period (the month, or the year for
 * yearly recurrences) already has a slot, so changing [Recurrence.dayOfMonth] never generates a
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
        .filter { frequency == Frequency.MONTHLY || it.month == startsOn.month }
        .map { periodOf(it) }
        .toSet()
    return occurrencesBetween(from, to)
        .filterNot { periodOf(it) in usedPeriods }
        .map { date ->
            Entry(
                type = type,
                amountCents = amountCents,
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
    Frequency.YEARLY -> date.year
}
