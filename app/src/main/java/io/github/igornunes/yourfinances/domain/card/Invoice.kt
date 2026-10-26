package io.github.igornunes.yourfinances.domain.card

import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.time.atDayClamped
import java.time.LocalDate
import java.time.YearMonth

/** A billing cycle: purchases from [periodStart] to [closingDate] (inclusive), paid on [dueDate]. */
data class Invoice(
    val closingMonth: YearMonth,
    val periodStart: LocalDate,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
)

/** The invoice a purchase belongs to. A purchase after the closing day goes to the next invoice. */
fun CreditCard.invoiceFor(purchasedOn: LocalDate): Invoice {
    val month = YearMonth.from(purchasedOn)
    val closingMonth = if (purchasedOn.isAfter(month.atDayClamped(closingDay))) month.plusMonths(1) else month
    return invoiceClosingIn(closingMonth)
}

/**
 * The invoice that closes in [closingMonth]. The due date is in the same month when the nominal
 * due day comes after the nominal closing day, otherwise in the following month.
 */
fun CreditCard.invoiceClosingIn(closingMonth: YearMonth): Invoice {
    val closingDate = closingMonth.atDayClamped(closingDay)
    val periodStart = closingMonth.minusMonths(1).atDayClamped(closingDay).plusDays(1)
    val dueMonth = if (dueDay > closingDay) closingMonth else closingMonth.plusMonths(1)
    return Invoice(
        closingMonth = closingMonth,
        periodStart = periodStart,
        closingDate = closingDate,
        dueDate = dueMonth.atDayClamped(dueDay),
    )
}

/** Cash date of a single purchase on this card: the due date of the invoice it falls into. */
fun CreditCard.paymentDateFor(purchasedOn: LocalDate): LocalDate = invoiceFor(purchasedOn).dueDate

/** The invoice due on [dueDate]'s month under the card's current days. */
fun CreditCard.invoiceDueIn(dueDate: LocalDate): Invoice {
    val dueMonth = YearMonth.from(dueDate)
    return invoiceClosingIn(if (dueDay > closingDay) dueMonth else dueMonth.minusMonths(1))
}

/** Due dates of [count] consecutive invoices, starting with the one [purchasedOn] falls into. */
fun CreditCard.dueDatesFrom(purchasedOn: LocalDate, count: Int): List<LocalDate> {
    val first = invoiceFor(purchasedOn).closingMonth
    return List(count) { index -> invoiceClosingIn(first.plusMonths(index.toLong())).dueDate }
}

/** Invoice total: card entries whose cash date is the invoice due date. */
fun CreditCard.invoiceTotalCents(invoice: Invoice, entries: List<Entry>): Long =
    entries
        .filter { it.creditCardId == id && it.paidOn == invoice.dueDate }
        .sumOf { it.amountCents }
