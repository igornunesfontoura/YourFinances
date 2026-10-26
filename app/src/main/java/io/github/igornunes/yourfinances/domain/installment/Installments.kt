package io.github.igornunes.yourfinances.domain.installment

import io.github.igornunes.yourfinances.domain.card.dueDatesFrom
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import io.github.igornunes.yourfinances.domain.money.splitCents
import java.time.LocalDate
import java.time.YearMonth

/** [count] monthly dates starting at [firstPayment], keeping its day (clamped in short months). */
fun monthlyPaymentDates(firstPayment: LocalDate, count: Int): List<LocalDate> =
    List(count) { index -> firstPayment.plusMonths(index.toLong()) }

/** Payment dates of a purchase made on [card]: the due dates of consecutive invoices. */
fun InstallmentPurchase.cardPaymentDates(card: CreditCard): List<LocalDate> {
    require(creditCardId == card.id) { "purchase does not belong to this card" }
    return card.dueDatesFrom(purchasedOn, installmentCount)
}

/**
 * One entry per installment, paid on the matching date in [paymentDates]. Each installment also
 * accrues in the month it is due (not the purchase month), so monthly budgets by competência
 * spread the purchase; [InstallmentPurchase.purchasedOn] keeps the real purchase date.
 * The remainder cents go to the first one.
 */
fun InstallmentPurchase.toEntries(paymentDates: List<LocalDate>): List<Entry> {
    require(paymentDates.size == installmentCount) { "expected $installmentCount payment dates" }
    return splitCents(totalCents, installmentCount).mapIndexed { index, amount ->
        Entry(
            type = type,
            amountCents = amount,
            description = description,
            occurredOn = paymentDates[index],
            paidOn = paymentDates[index],
            categoryId = categoryId,
            creditCardId = creditCardId,
            installmentPurchaseId = id,
            installmentNumber = index + 1,
            installmentCount = installmentCount,
        )
    }
}

/** Cash date of the last installment of each purchase found in [entries]. */
fun installmentEndDates(entries: List<Entry>): Map<Long, LocalDate> =
    entries
        .filter { it.installmentPurchaseId != null }
        .groupBy { it.installmentPurchaseId!! }
        .mapValues { (_, installments) -> installments.maxOf { it.paidOn } }

/** Installment amounts already committed per month (cash basis), from [from] onwards. */
fun committedInstallmentsByMonth(entries: List<Entry>, from: YearMonth): Map<YearMonth, Long> =
    entries
        .filter { it.installmentPurchaseId != null && !YearMonth.from(it.paidOn).isBefore(from) }
        .groupBy { YearMonth.from(it.paidOn) }
        .mapValues { (_, installments) -> installments.sumOf { it.amountCents } }
        .toSortedMap()

/** Installment amounts still to be paid after [today], per month (cash basis). */
fun pendingInstallmentsByMonth(entries: List<Entry>, today: LocalDate): Map<YearMonth, Long> =
    committedInstallmentsByMonth(entries.filter { it.paidOn.isAfter(today) }, YearMonth.from(today))
