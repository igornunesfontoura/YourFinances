package io.github.igornunes.yourfinances.domain.card

import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import java.time.LocalDate

/** The user paid the invoice due on [dueDate]. */
data class InvoicePayment(val cardId: Long, val dueDate: LocalDate, val paidOn: LocalDate)

/** A card with everything charged to it and the invoices already paid. */
data class CardActivity(val card: CreditCard, val entries: List<Entry>, val payments: List<InvoicePayment>)

enum class InvoiceStatus {
    /** Still taking purchases. */
    OPEN,

    /** Closed, waiting for payment. */
    CLOSED,

    /** Past its due date and not marked as paid. */
    OVERDUE,
    PAID,
}

/**
 * One invoice. [periodStart] and [closingDate] are null for a due date that no longer matches the
 * card's days (entries left in an invoice closed before the days changed): that cycle is unknown.
 */
data class InvoiceSummary(
    val periodStart: LocalDate?,
    val closingDate: LocalDate?,
    val dueDate: LocalDate,
    val totalCents: Long,
    val entries: List<Entry>,
    val status: InvoiceStatus,
    val paidOn: LocalDate?,
)

/**
 * Invoices of the card, newest due date first. Card entries carry the invoice due date as their
 * cash date, so an invoice is the sum of the card entries paid on that date. The invoice taking
 * purchases today is always listed, even when empty.
 */
fun CardActivity.invoices(today: LocalDate): List<InvoiceSummary> {
    val byDueDate = entries.filter { it.creditCardId == card.id }.groupBy { it.paidOn }.toMutableMap()
    byDueDate.putIfAbsent(card.invoiceFor(today).dueDate, emptyList())
    val paid = payments.filter { it.cardId == card.id }.associateBy { it.dueDate }
    return byDueDate.map { (dueDate, invoiceEntries) ->
        val invoice = card.invoiceDueIn(dueDate).takeIf { it.dueDate == dueDate }
        val payment = paid[dueDate]
        InvoiceSummary(
            periodStart = invoice?.periodStart,
            closingDate = invoice?.closingDate,
            dueDate = dueDate,
            totalCents = invoiceEntries.sumOf { it.amountCents },
            entries = invoiceEntries.sortedWith(compareBy({ it.occurredOn }, { it.id })),
            status = when {
                payment != null -> InvoiceStatus.PAID
                today.isAfter(dueDate) -> InvoiceStatus.OVERDUE
                invoice == null || today.isAfter(invoice.closingDate) -> InvoiceStatus.CLOSED
                else -> InvoiceStatus.OPEN
            },
            paidOn = payment?.paidOn,
        )
    }.sortedByDescending { it.dueDate }
}

/** The invoice taking purchases today. */
fun CardActivity.currentInvoice(today: LocalDate): InvoiceSummary =
    invoices(today).first { it.dueDate == card.invoiceFor(today).dueDate }

/** Closed invoices not marked as paid, the earliest due first. */
fun CardActivity.awaitingPayment(today: LocalDate): List<InvoiceSummary> =
    invoices(today)
        .filter { it.status == InvoiceStatus.CLOSED || it.status == InvoiceStatus.OVERDUE }
        .sortedBy { it.dueDate }

/**
 * After the card's closing or due day changed from [old]'s: the card entries whose invoice had not
 * closed yet, moved to invoices of the new days. Only entries whose dates change are returned.
 *
 * - Closed invoices (and so paid ones) keep their entries: their cycle already happened.
 * - Nothing moves into an invoice that is already closed under the new days.
 * - Installments keep one per invoice: the pending ones continue right after the last installment
 *   that stayed, or from the purchase's first invoice when none stayed. They accrue on their due
 *   date, so both dates move.
 */
fun CreditCard.rescheduled(
    old: CreditCard,
    entries: List<Entry>,
    purchases: List<InstallmentPurchase>,
    today: LocalDate,
): List<Entry> {
    require(old.id == id) { "old and new must be the same card" }
    val firstOpenDue = invoiceFor(today).dueDate
    fun isPending(entry: Entry) = !today.isAfter(old.invoiceDueIn(entry.paidOn).closingDate)

    val cardEntries = entries.filter { it.creditCardId == id }
    val singles = cardEntries
        .filter { it.installmentPurchaseId == null && isPending(it) }
        .map { it.copy(paidOn = maxOf(paymentDateFor(it.occurredOn), firstOpenDue)) }

    val purchasesById = purchases.associateBy { it.id }
    val installments = cardEntries
        .filter { it.installmentPurchaseId != null }
        .groupBy { it.installmentPurchaseId!! }
        .flatMap { (purchaseId, plan) ->
            val (pending, kept) = plan.sortedBy { it.installmentNumber }.partition(::isPending)
            if (pending.isEmpty()) return@flatMap emptyList()
            val start = kept.lastOrNull()
                ?.let { last -> maxOf(nextInvoiceDueDate(last.paidOn), firstOpenDue) }
                ?: maxOf(dueDatesFrom(purchasesById.getValue(purchaseId).purchasedOn, 1).single(), firstOpenDue)
            val startMonth = invoiceDueIn(start).closingMonth
            pending.mapIndexed { index, entry ->
                val dueDate = invoiceClosingIn(startMonth.plusMonths(index.toLong())).dueDate
                entry.copy(occurredOn = dueDate, paidOn = dueDate)
            }
        }

    val byId = cardEntries.associateBy { it.id }
    return (singles + installments).filter { it != byId[it.id] }
}

/** Due date of the invoice after the one due in [dueDate]'s month. */
private fun CreditCard.nextInvoiceDueDate(dueDate: LocalDate): LocalDate =
    invoiceClosingIn(invoiceDueIn(dueDate).closingMonth.plusMonths(1)).dueDate
