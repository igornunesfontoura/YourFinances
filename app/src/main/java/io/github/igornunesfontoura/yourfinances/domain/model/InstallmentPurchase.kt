package io.github.igornunesfontoura.yourfinances.domain.model

import java.time.LocalDate

/** A purchase split into [installmentCount] entries linked back to it. */
data class InstallmentPurchase(
    val id: Long = 0,
    val description: String,
    val totalCents: Long,
    val installmentCount: Int,
    val purchasedOn: LocalDate,
    val type: EntryType = EntryType.FIXED_EXPENSE,
    val categoryId: Long? = null,
    val creditCardId: Long? = null,
) {
    init {
        require(totalCents >= 0) { "totalCents must not be negative" }
        require(installmentCount >= 1) { "installmentCount must be at least 1" }
    }
}
