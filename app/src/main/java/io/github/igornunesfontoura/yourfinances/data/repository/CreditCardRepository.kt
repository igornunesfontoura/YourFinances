package io.github.igornunesfontoura.yourfinances.data.repository

import androidx.room.withTransaction
import io.github.igornunesfontoura.yourfinances.data.db.AppDatabase
import io.github.igornunesfontoura.yourfinances.data.db.entity.toDomain
import io.github.igornunesfontoura.yourfinances.data.db.entity.toEntity
import io.github.igornunesfontoura.yourfinances.domain.card.CardActivity
import io.github.igornunesfontoura.yourfinances.domain.card.InvoicePayment
import io.github.igornunesfontoura.yourfinances.domain.card.rescheduled
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface CreditCardRepository {
    fun observeAll(): Flow<List<CreditCard>>

    /** Every card with its entries and paid invoices. */
    fun observeActivity(): Flow<List<CardActivity>>

    suspend fun getById(id: Long): CreditCard?

    suspend fun add(card: CreditCard): Long

    /**
     * Saves the card and, when its days changed, moves the entries of invoices not closed yet to
     * invoices of the new days (see [io.github.igornunesfontoura.yourfinances.domain.card.rescheduled]).
     */
    suspend fun update(card: CreditCard, today: LocalDate)

    /** Deletes the card; returns false, changing nothing, when anything is or will be charged to it. */
    suspend fun delete(card: CreditCard): Boolean

    suspend fun markPaid(payment: InvoicePayment)

    suspend fun unmarkPaid(cardId: Long, dueDate: LocalDate)
}

class RoomCreditCardRepository(private val database: AppDatabase) : CreditCardRepository {
    private val cardDao = database.creditCardDao()
    private val entryDao = database.entryDao()
    private val purchaseDao = database.installmentPurchaseDao()
    private val paymentDao = database.invoicePaymentDao()
    private val recurrenceDao = database.recurrenceDao()

    override fun observeAll(): Flow<List<CreditCard>> = cardDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActivity(): Flow<List<CardActivity>> =
        combine(cardDao.observeAll(), entryDao.observeCardEntries(), paymentDao.observeAll()) { cards, entries, payments ->
            val entriesByCard = entries.map { it.toDomain() }.groupBy { it.creditCardId }
            val paymentsByCard = payments.map { it.toDomain() }.groupBy { it.cardId }
            cards.map { card ->
                CardActivity(card.toDomain(), entriesByCard[card.id].orEmpty(), paymentsByCard[card.id].orEmpty())
            }
        }

    override suspend fun getById(id: Long): CreditCard? = cardDao.getById(id)?.toDomain()

    override suspend fun add(card: CreditCard): Long = cardDao.insert(card.toEntity())

    override suspend fun update(card: CreditCard, today: LocalDate) = database.withTransaction {
        val old = requireNotNull(cardDao.getById(card.id)).toDomain()
        cardDao.update(card.toEntity())
        if (old.closingDay == card.closingDay && old.dueDay == card.dueDay) return@withTransaction
        // All of them: pending installments continue after the last installment that stays.
        val candidates = entryDao.getCardEntries(card.id).map { it.toDomain() }
        val purchases = purchaseDao.getByCard(card.id).map { it.toDomain() }
        entryDao.updateAll(card.rescheduled(old, candidates, purchases, today).map { it.toEntity() })
    }

    override suspend fun delete(card: CreditCard): Boolean = database.withTransaction {
        val used = entryDao.countByCard(card.id) + purchaseDao.countByCard(card.id) + recurrenceDao.countByCard(card.id)
        if (used > 0) return@withTransaction false
        cardDao.delete(card.toEntity())
        true
    }

    override suspend fun markPaid(payment: InvoicePayment) = paymentDao.upsert(payment.toEntity())

    override suspend fun unmarkPaid(cardId: Long, dueDate: LocalDate) = paymentDao.delete(cardId, dueDate)
}
