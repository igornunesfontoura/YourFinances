package io.github.igornunes.yourfinances.domain.model

import java.time.LocalDate

enum class Frequency { MONTHLY, YEARLY }

/**
 * Template that generates [EntryStatus.PREDICTED] entries.
 *
 * [dayOfMonth] is nominal (1..31) and is clamped to the last day of shorter months.
 * Yearly recurrences happen in the month of [startsOn].
 */
data class Recurrence(
    val id: Long = 0,
    val type: EntryType,
    val amountCents: Long,
    val description: String,
    val frequency: Frequency,
    val dayOfMonth: Int,
    val startsOn: LocalDate,
    val endsOn: LocalDate? = null,
    val categoryId: Long? = null,
) {
    init {
        require(amountCents >= 0) { "amountCents must not be negative" }
        require(dayOfMonth in 1..31) { "dayOfMonth must be in 1..31" }
        require(endsOn == null || !endsOn.isBefore(startsOn)) { "endsOn must not be before startsOn" }
    }
}
