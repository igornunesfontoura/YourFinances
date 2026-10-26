package io.github.igornunes.yourfinances.domain.entry

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import java.time.LocalDate

/** A confirmed entry paid when it happened (not on a card, not in installments). */
fun newEntry(type: EntryType, amountCents: Long, description: String, occurredOn: LocalDate): Entry =
    Entry(
        type = type,
        amountCents = amountCents,
        description = description.trim(),
        occurredOn = occurredOn,
        paidOn = occurredOn,
        status = EntryStatus.CONFIRMED,
    )

/** The user checked a predicted entry against reality and kept its values. */
fun Entry.confirmed(): Entry = copy(status = EntryStatus.CONFIRMED)

/**
 * Installments must keep adding up to their purchase and card entries must stay on the invoice of
 * their date, so the quick form only edits their description. Changing them is a purchase/card
 * operation.
 */
val Entry.isQuickEditable: Boolean
    get() = installmentPurchaseId == null && creditCardId == null

/** Deleting a single installment would break its purchase total: the whole purchase goes instead. */
val Entry.deletesWholePurchase: Boolean
    get() = installmentPurchaseId != null

/**
 * Applies the quick-form fields to an existing entry. Saving confirms it: the user has reviewed
 * the real values of a predicted entry. Entries that are not [isQuickEditable] only take the new
 * description.
 */
fun Entry.edited(type: EntryType, amountCents: Long, description: String, occurredOn: LocalDate): Entry {
    val reviewed = copy(description = description.trim(), status = EntryStatus.CONFIRMED)
    if (!isQuickEditable) return reviewed
    return reviewed.copy(type = type, amountCents = amountCents, occurredOn = occurredOn, paidOn = occurredOn)
}
