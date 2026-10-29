package io.github.igornunesfontoura.yourfinances.domain.subscription

import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import io.github.igornunesfontoura.yourfinances.domain.recurrence.occurrencesBetween
import java.time.LocalDate

/**
 * A recurring fixed expense (subscription, bill) with its charges in the next 12 months: one ending
 * soon costs only what is left.
 */
data class Subscription(val recurrence: Recurrence, val upcomingCharges: List<LocalDate>) {
    val nextChargeOn: LocalDate? get() = upcomingCharges.firstOrNull()

    val yearlyCents: Long get() = recurrence.amountCents * upcomingCharges.size
}

data class SubscriptionsSummary(
    /** Soonest next charge first. */
    val subscriptions: List<Subscription>,
    val yearlyCents: Long,
) {
    /** Average per month, rounded half up to the cent: yearly charges spread over 12 months. */
    val monthlyAverageCents: Long get() = (yearlyCents + 6) / 12
}

/**
 * Fixed-expense recurrences with a charge in the next 12 months (from [today] on); the yearly total
 * is exactly what those charges add up to. Skipped months are not known here and still count.
 */
fun subscriptionsSummary(recurrences: List<Recurrence>, today: LocalDate): SubscriptionsSummary {
    val subscriptions = recurrences
        .filter { it.type == EntryType.FIXED_EXPENSE }
        .map { Subscription(it, it.occurrencesBetween(today, today.plusYears(1).minusDays(1))) }
        .filter { it.upcomingCharges.isNotEmpty() }
        .sortedBy { it.nextChargeOn }
    return SubscriptionsSummary(subscriptions, subscriptions.sumOf { it.yearlyCents })
}
