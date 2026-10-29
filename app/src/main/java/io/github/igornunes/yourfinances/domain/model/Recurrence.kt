package io.github.igornunes.yourfinances.domain.model

import java.time.LocalDate

enum class Frequency { MONTHLY, YEARLY }

/**
 * Template that generates [EntryStatus.PREDICTED] entries.
 *
 * [dayOfMonth] is nominal (1..31) and is clamped to the last day of shorter months.
 * Yearly recurrences happen in the month of [startsOn].
 * [savingsPocketId] is the pocket of a recurring contribution or withdrawal; [creditCardId] the card
 * a recurring expense is charged to (its entries are paid on the invoice due date).
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
    val savingsPocketId: Long? = null,
    val creditCardId: Long? = null,
) {
    init {
        require(amountCents >= 0) { "amountCents must not be negative" }
        require(dayOfMonth in 1..31) { "dayOfMonth must be in 1..31" }
        require(endsOn == null || !endsOn.isBefore(startsOn)) { "endsOn must not be before startsOn" }
        require(savingsPocketId == null || type.movesSavings) { "only contributions and withdrawals use a pocket" }
        require(creditCardId == null || type.canUseCard) { "only expenses can be charged to a card" }
    }
}
