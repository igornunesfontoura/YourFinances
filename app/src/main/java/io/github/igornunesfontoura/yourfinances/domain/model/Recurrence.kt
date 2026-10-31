package io.github.igornunesfontoura.yourfinances.domain.model

import java.time.LocalDate

/** [TWICE_MONTHLY]: two payments a month on fixed days, such as a salary advance and the rest. */
enum class Frequency { MONTHLY, TWICE_MONTHLY, YEARLY }

/**
 * Template that generates [EntryStatus.PREDICTED] entries.
 *
 * [dayOfMonth] is nominal (1..31) and is clamped to the last day of shorter months.
 * Yearly recurrences happen in the month of [startsOn].
 * Twice-monthly recurrences also pay [secondAmountCents] on [secondDayOfMonth], a later day of the
 * same month; both are null for the other frequencies. When both days fall on the same last day of
 * a short month (29 and 30 in February), the first payment comes one day earlier.
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
    val secondDayOfMonth: Int? = null,
    val secondAmountCents: Long? = null,
) {
    init {
        require(amountCents >= 0) { "amountCents must not be negative" }
        require(dayOfMonth in 1..31) { "dayOfMonth must be in 1..31" }
        require(endsOn == null || !endsOn.isBefore(startsOn)) { "endsOn must not be before startsOn" }
        require(savingsPocketId == null || type.movesSavings) { "only contributions and withdrawals use a pocket" }
        require(creditCardId == null || type.canUseCard) { "only expenses can be charged to a card" }
        if (frequency == Frequency.TWICE_MONTHLY) {
            require(secondDayOfMonth != null && secondDayOfMonth in (dayOfMonth + 1)..31) { "second day must come after the first, up to 31" }
            require(secondAmountCents != null && secondAmountCents >= 0) { "second payment needs an amount" }
        } else {
            require(secondDayOfMonth == null && secondAmountCents == null) { "only twice-monthly recurrences have a second payment" }
        }
    }

    /** What one cycle pays: both payments of a twice-monthly recurrence, otherwise the amount. */
    val cycleAmountCents: Long get() = amountCents + (secondAmountCents ?: 0)
}
