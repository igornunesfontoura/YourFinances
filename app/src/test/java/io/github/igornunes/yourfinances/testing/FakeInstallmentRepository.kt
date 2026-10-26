package io.github.igornunes.yourfinances.testing

import io.github.igornunes.yourfinances.data.repository.InstallmentRepository
import io.github.igornunes.yourfinances.domain.installment.InstallmentPlan
import io.github.igornunes.yourfinances.domain.installment.cardPaymentDates
import io.github.igornunes.yourfinances.domain.installment.toEntries
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeInstallmentRepository(initial: List<InstallmentPlan> = emptyList()) : InstallmentRepository {
    val plans = MutableStateFlow(initial)
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.purchase.id } ?: 0) + 1

    override fun observePlans(): Flow<List<InstallmentPlan>> = plans

    override suspend fun add(purchase: InstallmentPurchase, paymentDates: List<LocalDate>): Long {
        check(!failWrites) { "write failed" }
        val stored = purchase.copy(id = nextId++)
        plans.update { it + InstallmentPlan(stored, stored.toEntries(paymentDates)) }
        return stored.id
    }

    override suspend fun addOnCard(purchase: InstallmentPurchase, card: CreditCard): Long =
        add(purchase, purchase.cardPaymentDates(card))

    override suspend fun delete(purchaseId: Long) {
        check(!failWrites) { "write failed" }
        plans.update { list -> list.filterNot { it.purchase.id == purchaseId } }
    }
}
