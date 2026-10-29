package io.github.igornunesfontoura.yourfinances.testing

import io.github.igornunesfontoura.yourfinances.data.repository.CreditCardRepository
import io.github.igornunesfontoura.yourfinances.domain.card.CardActivity
import io.github.igornunesfontoura.yourfinances.domain.card.InvoicePayment
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Cards with in-memory entries and payments; rescheduling itself is covered by the domain and DAO tests. */
class FakeCreditCardRepository(initial: List<CreditCard> = emptyList()) : CreditCardRepository {
    val cards = MutableStateFlow(initial)
    val entries = MutableStateFlow<List<Entry>>(emptyList())
    val payments = MutableStateFlow<List<InvoicePayment>>(emptyList())
    val updateDays = mutableListOf<LocalDate>()
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeAll(): Flow<List<CreditCard>> = cards

    override fun observeActivity(): Flow<List<CardActivity>> = combine(cards, entries, payments) { cards, entries, payments ->
        cards.map { card ->
            CardActivity(card, entries.filter { it.creditCardId == card.id }, payments.filter { it.cardId == card.id })
        }
    }

    override suspend fun getById(id: Long): CreditCard? = cards.value.find { it.id == id }

    override suspend fun add(card: CreditCard): Long {
        checkWritable()
        val id = nextId++
        cards.update { it + card.copy(id = id) }
        return id
    }

    override suspend fun update(card: CreditCard, today: LocalDate) {
        checkWritable()
        cards.update { list -> list.map { if (it.id == card.id) card else it } }
        updateDays += today
    }

    override suspend fun delete(card: CreditCard): Boolean {
        checkWritable()
        if (entries.value.any { it.creditCardId == card.id }) return false
        cards.update { list -> list.filterNot { it.id == card.id } }
        return true
    }

    override suspend fun markPaid(payment: InvoicePayment) {
        checkWritable()
        payments.update { list -> list.filterNot { it.cardId == payment.cardId && it.dueDate == payment.dueDate } + payment }
    }

    override suspend fun unmarkPaid(cardId: Long, dueDate: LocalDate) {
        checkWritable()
        payments.update { list -> list.filterNot { it.cardId == cardId && it.dueDate == dueDate } }
    }

    private fun checkWritable() = check(!failWrites) { "write failed" }
}
