package io.github.igornunes.yourfinances.domain.model

import java.time.LocalDate

enum class EntryType { INCOME, FIXED_EXPENSE, FLEXIBLE_EXPENSE, INVESTMENT }

enum class EntryStatus { PREDICTED, CONFIRMED }

/**
 * A single money movement.
 *
 * [amountCents] is never negative: the direction comes from [type].
 * [occurredOn] is the accrual date (when the purchase/event happened) and [paidOn] the cash date
 * (when money left or arrived; for credit cards, the invoice due date).
 * [scheduledFor] is the slot a [Recurrence] generated this entry for, kept even if the user moves
 * the entry to another date, so the same slot is never generated twice.
 */
data class Entry(
    val id: Long = 0,
    val type: EntryType,
    val amountCents: Long,
    val description: String,
    val occurredOn: LocalDate,
    val paidOn: LocalDate,
    val status: EntryStatus = EntryStatus.CONFIRMED,
    val categoryId: Long? = null,
    val creditCardId: Long? = null,
    val recurrenceId: Long? = null,
    val scheduledFor: LocalDate? = null,
    val installmentPurchaseId: Long? = null,
    val installmentNumber: Int? = null,
    val installmentCount: Int? = null,
) {
    init {
        require(amountCents >= 0) { "amountCents must not be negative" }
        require((recurrenceId == null) == (scheduledFor == null)) {
            "recurrenceId and scheduledFor must be set together"
        }
        val installmentFields = listOf(installmentPurchaseId, installmentNumber, installmentCount)
        require(installmentFields.all { it == null } || installmentFields.none { it == null }) {
            "installment fields must be set together"
        }
        if (installmentNumber != null && installmentCount != null) {
            require(installmentNumber in 1..installmentCount) { "installmentNumber out of range" }
        }
    }
}
