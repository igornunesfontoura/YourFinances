package io.github.igornunes.yourfinances.domain.recurrence

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RecurrenceRulesTest {
    @Test
    fun `window covers the current month and two more`() {
        assertEquals(
            PredictionWindow(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31)),
            predictionWindow(LocalDate.of(2026, 3, 15)),
        )
    }

    @Test
    fun `window crosses the year`() {
        assertEquals(
            PredictionWindow(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31)),
            predictionWindow(LocalDate.of(2026, 11, 30)),
        )
    }

    @Test
    fun `window ending in February uses its last day`() {
        assertEquals(LocalDate.of(2028, 2, 29), predictionWindow(LocalDate.of(2027, 12, 31)).to)
    }

    private val recurrence = Recurrence(
        id = 4,
        type = EntryType.FIXED_EXPENSE,
        amountCents = 15000,
        description = "Luz",
        frequency = Frequency.MONTHLY,
        dayOfMonth = 10,
        startsOn = LocalDate.of(2026, 1, 1),
        categoryId = 2,
    )

    private fun occurrence(recurrenceId: Long) = Entry(
        type = EntryType.FLEXIBLE_EXPENSE,
        amountCents = 17890,
        description = "Luz (bandeira vermelha)",
        occurredOn = LocalDate.of(2026, 3, 12),
        paidOn = LocalDate.of(2026, 3, 12),
        categoryId = 5,
        recurrenceId = recurrenceId,
        scheduledFor = LocalDate.of(2026, 3, 10),
    )

    @Test
    fun `apply to upcoming copies values but keeps the schedule`() {
        val updated = recurrence.withValuesFrom(occurrence(4))
        assertEquals(17890L, updated.amountCents)
        assertEquals(EntryType.FLEXIBLE_EXPENSE, updated.type)
        assertEquals("Luz (bandeira vermelha)", updated.description)
        assertEquals(5L, updated.categoryId)
        assertEquals(10, updated.dayOfMonth)
        assertEquals(recurrence.startsOn, updated.startsOn)
    }

    @Test
    fun `apply to upcoming rejects an entry of another recurrence`() {
        assertThrows<IllegalArgumentException> { recurrence.withValuesFrom(occurrence(99)) }
    }
}
