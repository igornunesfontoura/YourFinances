package io.github.igornunes.yourfinances.domain.entry

import io.github.igornunes.yourfinances.domain.card.paymentDateFor
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import java.time.LocalDate

/**
 * A confirmed single entry. Paid when it happened, or, on a [card], on the due date of the
 * invoice it falls into. [savingsPocketId] only applies to contributions and withdrawals.
 */
fun newEntry(
    type: EntryType,
    amountCents: Long,
    description: String,
    occurredOn: LocalDate,
    card: CreditCard?,
    savingsPocketId: Long? = null,
): Entry =
    Entry(
        type = type,
        amountCents = amountCents,
        description = description.trim(),
        occurredOn = occurredOn,
        paidOn = card?.paymentDateFor(occurredOn) ?: occurredOn,
        status = EntryStatus.CONFIRMED,
        creditCardId = card?.id,
        savingsPocketId = savingsPocketId.takeIf { type.movesSavings },
    )

/** The user checked a predicted entry against reality and kept its values. */
fun Entry.confirmed(): Entry = copy(status = EntryStatus.CONFIRMED)

/**
 * Installments must keep adding up to their purchase, so the quick form only edits their
 * description. Changing them is a purchase operation.
 */
val Entry.isQuickEditable: Boolean
    get() = installmentPurchaseId == null

/** Deleting a single installment would break its purchase total: the whole purchase goes instead. */
val Entry.deletesWholePurchase: Boolean
    get() = installmentPurchaseId != null

/**
 * Applies the quick-form fields to an existing entry, paid with [card] (null: not on a card).
 * Saving confirms it: the user has reviewed the real values of a predicted entry. Entries that are
 * not [isQuickEditable] only take the new description. A card entry whose card and date stay the
 * same keeps its invoice, even if the card's days changed since.
 */
fun Entry.edited(
    type: EntryType,
    amountCents: Long,
    description: String,
    occurredOn: LocalDate,
    card: CreditCard?,
    savingsPocketId: Long? = null,
): Entry {
    val reviewed = copy(description = description.trim(), status = EntryStatus.CONFIRMED)
    if (!isQuickEditable) return reviewed
    val sameInvoice = card != null && card.id == creditCardId && occurredOn == this.occurredOn
    return reviewed.copy(
        type = type,
        amountCents = amountCents,
        occurredOn = occurredOn,
        paidOn = when {
            sameInvoice -> paidOn
            card != null -> card.paymentDateFor(occurredOn)
            else -> occurredOn
        },
        creditCardId = card?.id,
        savingsPocketId = savingsPocketId.takeIf { type.movesSavings },
    )
}
