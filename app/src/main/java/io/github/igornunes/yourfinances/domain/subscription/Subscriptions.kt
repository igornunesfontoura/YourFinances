package io.github.igornunes.yourfinances.domain.subscription

import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import io.github.igornunes.yourfinances.domain.recurrence.occurrencesBetween
import java.time.LocalDate

/** A recurring fixed expense (subscription, bill) and its next charge, if any is left. */
data class Subscription(val recurrence: Recurrence, val nextChargeOn: LocalDate?) {
    /** Exact cost per year: twelve monthly charges or one yearly charge. */
    val yearlyCents: Long
        get() = when (recurrence.frequency) {
            Frequency.MONTHLY -> recurrence.amountCents * 12
            Frequency.YEARLY -> recurrence.amountCents
        }
}

data class SubscriptionsSummary(
    /** Soonest next charge first; those with no charge left come last. */
    val subscriptions: List<Subscription>,
    val yearlyCents: Long,
) {
    /** Average per month, rounded half up to the cent: yearly charges spread over 12 months. */
    val monthlyAverageCents: Long get() = (yearlyCents + 6) / 12
}

/**
 * Fixed-expense recurrences still active on [today] (no end, or ending today or later). The next
 * charge is the first occurrence from today on.
 */
fun subscriptionsSummary(recurrences: List<Recurrence>, today: LocalDate): SubscriptionsSummary {
    val active = recurrences.filter {
        it.type == EntryType.FIXED_EXPENSE && (it.endsOn == null || !it.endsOn.isBefore(today))
    }
    val subscriptions = active
        .map { Subscription(it, it.occurrencesBetween(today, today.plusYears(1)).firstOrNull()) }
        .sortedWith(compareBy(nullsLast()) { it.nextChargeOn })
    return SubscriptionsSummary(subscriptions, subscriptions.sumOf { it.yearlyCents })
}
