package io.github.igornunesfontoura.yourfinances.domain.installment

import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.InstallmentPurchase
import java.time.LocalDate

/** Most installment plans offered in Brazil stay well below this. */
const val MAX_INSTALLMENTS = 120

/** First installment due one month after the purchase (same day, clamped in short months). */
fun defaultFirstPayment(purchasedOn: LocalDate): LocalDate = purchasedOn.plusMonths(1)

/** Progress of a purchase on a given day (cash basis: an installment counts as paid on its paidOn). */
data class InstallmentProgress(
    val purchase: InstallmentPurchase,
    val paidCount: Int,
    val remainingCents: Long,
    val endsOn: LocalDate?,
) {
    val isFinished: Boolean get() = paidCount >= purchase.installmentCount

    /** Share of the installments already paid, from 0 to 1. */
    val paidShare: Float get() = (paidCount.toFloat() / purchase.installmentCount.coerceAtLeast(1)).coerceIn(0f, 1f)
}

fun InstallmentPurchase.progress(installments: List<Entry>, today: LocalDate): InstallmentProgress {
    require(installments.all { it.installmentPurchaseId == id }) { "installments of another purchase" }
    val (paid, remaining) = installments.partition { !it.paidOn.isAfter(today) }
    return InstallmentProgress(
        purchase = this,
        paidCount = paid.size,
        remainingCents = remaining.sumOf { it.amountCents },
        endsOn = installments.maxOfOrNull { it.paidOn },
    )
}

/** A purchase together with its installment entries. */
data class InstallmentPlan(val purchase: InstallmentPurchase, val installments: List<Entry>) {
    fun progress(today: LocalDate): InstallmentProgress = purchase.progress(installments, today)
}
