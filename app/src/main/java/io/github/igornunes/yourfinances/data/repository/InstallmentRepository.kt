package io.github.igornunes.yourfinances.data.repository

import androidx.room.withTransaction
import io.github.igornunes.yourfinances.data.db.AppDatabase
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.installment.InstallmentPlan
import io.github.igornunes.yourfinances.domain.installment.cardPaymentDates
import io.github.igornunes.yourfinances.domain.installment.toEntries
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Installment purchases are created and deleted as a whole, never one installment at a time. */
interface InstallmentRepository {
    fun observePlans(): Flow<List<InstallmentPlan>>

    /** Stores a purchase outside a credit card and its installments atomically; returns its id. */
    suspend fun add(purchase: InstallmentPurchase, paymentDates: List<LocalDate>): Long

    /** Deletes the purchase and, by cascade, all of its installments. */
    suspend fun delete(purchaseId: Long)
}

class RoomInstallmentRepository(private val database: AppDatabase) : InstallmentRepository {
    private val purchaseDao = database.installmentPurchaseDao()
    private val entryDao = database.entryDao()

    override fun observePlans(): Flow<List<InstallmentPlan>> =
        purchaseDao.observeAllWithInstallments().map { list -> list.map { it.toDomain() } }

    override suspend fun add(purchase: InstallmentPurchase, paymentDates: List<LocalDate>): Long {
        require(purchase.creditCardId == null) { "card purchases must use addOnCard" }
        return insert(purchase, paymentDates)
    }

    /** Stores a card purchase; installments are paid on the due dates of consecutive invoices. */
    suspend fun addOnCard(purchase: InstallmentPurchase, card: CreditCard): Long =
        insert(purchase, purchase.cardPaymentDates(card))

    override suspend fun delete(purchaseId: Long) = purchaseDao.deleteById(purchaseId)

    private suspend fun insert(purchase: InstallmentPurchase, paymentDates: List<LocalDate>): Long =
        database.withTransaction {
            val purchaseId = purchaseDao.insert(purchase.toEntity())
            val entries = purchase.copy(id = purchaseId).toEntries(paymentDates)
            entryDao.insertAll(entries.map { it.toEntity() })
            purchaseId
        }
}
