package io.github.igornunesfontoura.yourfinances.domain.subscription

import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SubscriptionsTest {
    private val today = LocalDate.of(2026, 3, 15)

    private fun recurrence(
        id: Long,
        amount: Long,
        day: Int,
        frequency: Frequency = Frequency.MONTHLY,
        type: EntryType = EntryType.FIXED_EXPENSE,
        startsOn: LocalDate = LocalDate.of(2025, 1, 1),
        endsOn: LocalDate? = null,
    ) = Recurrence(
        id = id, type = type, amountCents = amount, description = "r$id", frequency = frequency, dayOfMonth = day,
        startsOn = startsOn, endsOn = endsOn,
    )

    @Test
    fun `active fixed expenses, soonest charge first, yearly spread over the months`() {
        val streaming = recurrence(1, 5590, 20)
        val gym = recurrence(2, 9990, 10)
        val domain = recurrence(3, 4000, 5, Frequency.YEARLY, startsOn = LocalDate.of(2025, 6, 1))
        val summary = subscriptionsSummary(
            listOf(
                streaming,
                gym,
                domain,
                recurrence(4, 500000, 5, type = EntryType.INCOME),
                recurrence(5, 1000, 5, endsOn = LocalDate.of(2026, 3, 14)),
            ),
            today,
        )
        assertEquals(listOf(streaming, gym, domain), summary.subscriptions.map { it.recurrence })
        assertEquals(
            listOf(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 4, 10), LocalDate.of(2026, 6, 5)),
            summary.subscriptions.map { it.nextChargeOn },
        )
        assertEquals(5590L * 12 + 9990L * 12 + 4000L, summary.yearlyCents)
        // 190960 / 12 = 15913.33…, rounded to the cent.
        assertEquals(15913L, summary.monthlyAverageCents)
    }

    @Test
    fun `charges today count, ended or ending recurrences cost only what is left`() {
        val endingSoon = recurrence(1, 1000, 10, endsOn = LocalDate.of(2026, 5, 20))
        val today15 = recurrence(2, 1000, 15)
        val ended = recurrence(3, 1000, 25, endsOn = LocalDate.of(2026, 3, 20))
        val summary = subscriptionsSummary(listOf(endingSoon, today15, ended), today)
        assertEquals(listOf(today15, endingSoon), summary.subscriptions.map { it.recurrence })
        assertEquals(listOf(today, LocalDate.of(2026, 4, 10)), summary.subscriptions.map { it.nextChargeOn })
        // April and May for the one ending in May; twelve charges for the other.
        assertEquals(listOf(12000L, 2000L), summary.subscriptions.map { it.yearlyCents })
        assertEquals(14000L, summary.yearlyCents)
    }

    @Test
    fun `average rounds half up`() {
        assertEquals(1L, SubscriptionsSummary(emptyList(), 6).monthlyAverageCents)
        assertEquals(0L, SubscriptionsSummary(emptyList(), 5).monthlyAverageCents)
    }
}
