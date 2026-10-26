package io.github.igornunes.yourfinances.domain.recurrence

import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecurrenceScheduleTest {
    private fun recurrence(
        day: Int,
        frequency: Frequency = Frequency.MONTHLY,
        startsOn: LocalDate = LocalDate.of(2026, 1, 1),
        endsOn: LocalDate? = null,
    ) = Recurrence(
        id = 7,
        type = EntryType.FIXED_EXPENSE,
        amountCents = 4990,
        description = "Streaming",
        frequency = frequency,
        dayOfMonth = day,
        startsOn = startsOn,
        endsOn = endsOn,
    )

    @Test
    fun `day 31 falls on the last day of short months`() {
        val dates = recurrence(31).occurrencesBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 6, 30),
            ),
            dates,
        )
    }

    @Test
    fun `day 30 and 29 in February of a leap year`() {
        val from = LocalDate.of(2028, 2, 1)
        val to = LocalDate.of(2028, 3, 31)
        assertEquals(
            listOf(LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 30)),
            recurrence(30, startsOn = from).occurrencesBetween(from, to),
        )
        assertEquals(
            listOf(LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 29)),
            recurrence(29, startsOn = from).occurrencesBetween(from, to),
        )
    }

    @Test
    fun `clamping one month does not shift the next`() {
        val dates = recurrence(31).occurrencesBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31))
        assertEquals(LocalDate.of(2026, 3, 31), dates.last())
    }

    @Test
    fun `no occurrence before the start date`() {
        val dates = recurrence(10, startsOn = LocalDate.of(2026, 3, 15))
            .occurrencesBetween(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31))
        assertEquals(listOf(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 10)), dates)
    }

    @Test
    fun `no occurrence after the end date`() {
        val dates = recurrence(10, endsOn = LocalDate.of(2026, 3, 9))
            .occurrencesBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(listOf(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10)), dates)
    }

    @Test
    fun `range is inclusive on both ends`() {
        val dates = recurrence(10).occurrencesBetween(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10))
        assertEquals(listOf(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10)), dates)
    }

    @Test
    fun `yearly recurrence happens in the start month, clamping February 29`() {
        val yearly = recurrence(29, Frequency.YEARLY, startsOn = LocalDate.of(2028, 2, 1))
        val dates = yearly.occurrencesBetween(LocalDate.of(2028, 1, 1), LocalDate.of(2030, 12, 31))
        assertEquals(
            listOf(LocalDate.of(2028, 2, 29), LocalDate.of(2029, 2, 28), LocalDate.of(2030, 2, 28)),
            dates,
        )
    }

    @Test
    fun `empty when the range ends before the start`() {
        val dates = recurrence(10, startsOn = LocalDate.of(2026, 6, 1))
            .occurrencesBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31))
        assertTrue(dates.isEmpty())
    }

    @Test
    fun `generates predicted entries linked to the recurrence`() {
        val entries = recurrence(5).predictedEntriesBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28))
        assertEquals(2, entries.size)
        entries.forEach {
            assertEquals(EntryStatus.PREDICTED, it.status)
            assertEquals(7L, it.recurrenceId)
            assertEquals(4990L, it.amountCents)
            assertEquals(it.scheduledFor, it.occurredOn)
            assertEquals(it.scheduledFor, it.paidOn)
        }
    }

    @Test
    fun `skips slots already generated`() {
        val entries = recurrence(5).predictedEntriesBetween(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            alreadyScheduled = setOf(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 5)),
        )
        assertEquals(listOf(LocalDate.of(2026, 3, 5)), entries.map { it.scheduledFor })
    }

    @Test
    fun `changing the day does not duplicate a month that already has an entry`() {
        // Slots generated while the recurrence was on the 5th; now it is on the 10th.
        val entries = recurrence(10).predictedEntriesBetween(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            alreadyScheduled = setOf(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 5)),
        )
        assertEquals(listOf(LocalDate.of(2026, 3, 10)), entries.map { it.scheduledFor })
    }

    @Test
    fun `yearly recurrence uses the year as its period`() {
        val yearly = recurrence(15, Frequency.YEARLY, startsOn = LocalDate.of(2026, 4, 1))
        val entries = yearly.predictedEntriesBetween(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 12, 31),
            alreadyScheduled = setOf(LocalDate.of(2026, 4, 1)),
        )
        assertEquals(listOf(LocalDate.of(2027, 4, 15)), entries.map { it.scheduledFor })
    }

    @Test
    fun `slots of a former monthly schedule do not use up the yearly occurrence`() {
        val yearly = recurrence(15, Frequency.YEARLY, startsOn = LocalDate.of(2026, 12, 1))
        val entries = yearly.predictedEntriesBetween(
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 31),
            alreadyScheduled = setOf(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 10, 15)),
        )
        assertEquals(listOf(LocalDate.of(2026, 12, 15)), entries.map { it.scheduledFor })
    }
}
