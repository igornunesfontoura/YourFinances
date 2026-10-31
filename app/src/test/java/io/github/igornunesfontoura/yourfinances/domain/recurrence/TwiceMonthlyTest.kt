package io.github.igornunesfontoura.yourfinances.domain.recurrence

import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import io.github.igornunesfontoura.yourfinances.domain.subscription.subscriptionsSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

/** Salary paid twice a month: an advance on the 16th and the rest on the 31st. */
class TwiceMonthlyTest {
    private fun salary(
        firstDay: Int = 16,
        secondDay: Int = 31,
        startsOn: LocalDate = LocalDate.of(2026, 1, 1),
        type: EntryType = EntryType.INCOME,
    ) = Recurrence(
        id = 3,
        type = type,
        amountCents = 200000,
        description = "Salário",
        frequency = Frequency.TWICE_MONTHLY,
        dayOfMonth = firstDay,
        secondDayOfMonth = secondDay,
        secondAmountCents = 300000,
        startsOn = startsOn,
    )

    @Test
    fun `two occurrences each month, the 31st on the last day of short months`() {
        val dates = salary().occurrencesBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30))
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 16), LocalDate.of(2026, 4, 30),
            ),
            dates,
        )
    }

    @Test
    fun `days that clamp to the same date keep apart, the first one day earlier`() {
        val dates = salary(firstDay = 29, secondDay = 30)
            .occurrencesBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
        assertEquals(listOf(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 28)), dates)
    }

    @Test
    fun `each payment is predicted with its own amount`() {
        val entries = salary().predictedEntriesBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
        assertEquals(listOf(200000L, 300000L), entries.map { it.amountCents })
        assertEquals(listOf(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 28)), entries.map { it.scheduledFor })
        assertEquals(setOf(EntryStatus.PREDICTED), entries.map { it.status }.toSet())
    }

    @Test
    fun `a used slot drops only its own payment`() {
        val entries = salary().predictedEntriesBetween(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28),
            alreadyScheduled = setOf(LocalDate.of(2026, 2, 16)),
        )
        assertEquals(listOf(LocalDate.of(2026, 2, 28)), entries.map { it.occurredOn })
    }

    @Test
    fun `moving the days does not duplicate a month already scheduled`() {
        // Slots from the former 16/31 schedule still count for the new 15/30 one.
        val entries = salary(firstDay = 15, secondDay = 30).predictedEntriesBetween(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
            alreadyScheduled = setOf(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 31)),
        )
        assertEquals(emptyList<Entry>(), entries)
    }

    @Test
    fun `applying an edit to upcoming changes only that payment`() {
        val recurrence = salary()
        val rest = recurrence.predictedEntriesBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))[1]
        val updated = recurrence.withValuesFrom(rest.copy(amountCents = 320000, description = "Salário (saldo)"))
        assertEquals(200000L, updated.amountCents)
        assertEquals(320000L, updated.secondAmountCents)
        assertEquals("Salário (saldo)", updated.description)

        val advance = recurrence.predictedEntriesBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))[0]
        val raised = recurrence.withValuesFrom(advance.copy(amountCents = 210000))
        assertEquals(210000L, raised.amountCents)
        assertEquals(300000L, raised.secondAmountCents)
    }

    @Test
    fun `one cycle adds both payments`() {
        assertEquals(500000L, salary().cycleAmountCents)
    }

    @Test
    fun `subscriptions count both charges of a twice monthly bill`() {
        val bill = salary(type = EntryType.FIXED_EXPENSE)
        val summary = subscriptionsSummary(listOf(bill), today = LocalDate.of(2026, 1, 1))
        assertEquals(12 * 500000L, summary.yearlyCents)
        assertEquals(500000L, summary.monthlyAverageCents)
    }

    @Test
    fun `second day must come after the first and have an amount`() {
        assertThrows<IllegalArgumentException> { salary(firstDay = 31, secondDay = 16) }
        assertThrows<IllegalArgumentException> { salary(firstDay = 16, secondDay = 16) }
        assertThrows<IllegalArgumentException> { salary().copy(secondAmountCents = null) }
        assertThrows<IllegalArgumentException> {
            salary().copy(frequency = Frequency.MONTHLY)
        }
    }

    // Schedule changes: slots used under the old days move to the same payment under the new ones.

    private fun monthly(day: Int) = Recurrence(
        id = 3, type = EntryType.INCOME, amountCents = 500000, description = "Salário",
        frequency = Frequency.MONTHLY, dayOfMonth = day, startsOn = LocalDate.of(2026, 1, 1),
    )

    private fun predictedAfterChange(previous: Recurrence, next: Recurrence, used: Set<LocalDate>): List<Pair<LocalDate, Long>> {
        val moves = next.carrySlotsFrom(previous, used)
        val carried = used.map { moves.moved[it] ?: it }.toSet() + moves.skipped
        return next.predictedEntriesBetween(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), carried)
            .map { it.occurredOn to it.amountCents }
    }

    @Test
    fun `both payments confirmed, then a big move adds nothing to that month`() {
        val used = setOf(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 31))
        assertEquals(emptyList<Pair<LocalDate, Long>>(), predictedAfterChange(salary(), salary(firstDay = 1, secondDay = 10), used))
    }

    @Test
    fun `advance confirmed, then a big move still predicts the rest`() {
        val used = setOf(LocalDate.of(2026, 3, 16))
        assertEquals(
            listOf(LocalDate.of(2026, 3, 20) to 300000L),
            predictedAfterChange(salary(), salary(firstDay = 5, secondDay = 20), used),
        )
    }

    @Test
    fun `rest skipped, then a big move keeps it skipped and predicts the advance`() {
        val used = setOf(LocalDate.of(2026, 3, 31))
        assertEquals(
            listOf(LocalDate.of(2026, 3, 1) to 200000L),
            predictedAfterChange(salary(), salary(firstDay = 1, secondDay = 10), used),
        )
    }

    @Test
    fun `a month already paid as monthly is not paid again after switching to twice monthly`() {
        assertEquals(emptyList<Pair<LocalDate, Long>>(), predictedAfterChange(monthly(5), salary(), setOf(LocalDate.of(2026, 3, 5))))
        assertEquals(emptyList<Pair<LocalDate, Long>>(), predictedAfterChange(monthly(31), salary(), setOf(LocalDate.of(2026, 3, 31))))
    }

    @Test
    fun `switching from twice monthly to monthly keeps the month paid`() {
        val used = setOf(LocalDate.of(2026, 3, 16))
        val moves = monthly(5).carrySlotsFrom(salary(), used)
        assertEquals(emptyMap<LocalDate, LocalDate>(), moves.moved)
        assertEquals(emptyList<Entry>(), monthly(5).predictedEntriesBetween(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), used))
    }

    @Test
    fun `an unchanged schedule moves nothing`() {
        val moves = salary().copy(amountCents = 210000).carrySlotsFrom(salary(), setOf(LocalDate.of(2026, 3, 16)))
        assertEquals(emptyMap<LocalDate, LocalDate>(), moves.moved)
        assertEquals(emptySet<LocalDate>(), moves.skipped)
    }

    @Test
    fun `payment of a date outside both days goes to the nearer one`() {
        assertEquals(0, salary().paymentOf(LocalDate.of(2026, 3, 1)))
        assertEquals(1, salary().paymentOf(LocalDate.of(2026, 3, 30)))
        assertEquals(0, monthly(5).paymentOf(LocalDate.of(2026, 3, 30)))
    }

    @Test
    fun `leap February keeps 29 and 30 apart`() {
        val dates = salary(firstDay = 29, secondDay = 30).occurrencesBetween(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29))
        assertEquals(listOf(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 2, 29)), dates)
    }

    @Test
    fun `start and end dates between the two days keep only the payments inside`() {
        val started = salary(startsOn = LocalDate.of(2026, 3, 20))
        assertEquals(listOf(LocalDate.of(2026, 3, 31)), started.occurrencesBetween(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
        val ended = salary().copy(endsOn = LocalDate.of(2026, 3, 20))
        assertEquals(listOf(LocalDate.of(2026, 3, 16)), ended.occurrencesBetween(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30)))
    }
}
