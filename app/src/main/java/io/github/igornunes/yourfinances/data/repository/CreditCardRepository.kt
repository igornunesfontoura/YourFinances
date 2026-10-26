package io.github.igornunes.yourfinances.data.repository

import io.github.igornunes.yourfinances.data.db.dao.CreditCardDao
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CreditCardRepository(private val dao: CreditCardDao) {
    fun observeAll(): Flow<List<CreditCard>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): CreditCard? = dao.getById(id)?.toDomain()

    suspend fun add(card: CreditCard): Long = dao.insert(card.toEntity())

    suspend fun update(card: CreditCard) = dao.update(card.toEntity())

    suspend fun delete(card: CreditCard) = dao.delete(card.toEntity())
}
