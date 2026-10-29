package io.github.igornunes.yourfinances.domain.model

import java.time.LocalDate

enum class EntryType {
    INCOME,
    FIXED_EXPENSE,
    FLEXIBLE_EXPENSE,

    /** Aporte: money set aside, into a [SavingsPocket] or none. */
    INVESTMENT,

    /** Resgate: money taken back from what was set aside; it is available to spend again. */
    WITHDRAWAL,
    ;

    /** Only expenses can be charged to a credit card. */
    val canUseCard: Boolean get() = this == FIXED_EXPENSE || this == FLEXIBLE_EXPENSE

    /** Contributions and withdrawals move money in or out of the savings pockets. */
    val movesSavings: Boolean get() = this == INVESTMENT || this == WITHDRAWAL

    /** Money arriving to the user's cash: income and withdrawals. */
    val isInflow: Boolean get() = this == INCOME || this == WITHDRAWAL
}

enum class EntryStatus { PREDICTED, CONFIRMED }

/**
 * A single money movement.
 *
 * [amountCents] is never negative: the direction comes from [type].
 * [occurredOn] is the accrual date (when the purchase/event happened) and [paidOn] the cash date
 * (when money left or arrived; for credit cards, the invoice due date).
 * [savingsPocketId] is the pocket an [EntryType.INVESTMENT] or [EntryType.WITHDRAWAL] moves money
 * in or out of; null means "sem caixinha".
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
    val savingsPocketId: Long? = null,
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
        require(creditCardId == null || type.canUseCard) { "only expenses can be charged to a card" }
        require(savingsPocketId == null || type.movesSavings) { "only contributions and withdrawals use a pocket" }
        if (installmentNumber != null && installmentCount != null) {
            require(installmentNumber in 1..installmentCount) { "installmentNumber out of range" }
        }
    }
}
