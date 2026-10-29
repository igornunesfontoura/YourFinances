package io.github.igornunes.yourfinances.data.repository

import androidx.room.withTransaction
import io.github.igornunes.yourfinances.data.db.AppDatabase
import io.github.igornunes.yourfinances.data.db.dao.MAX_QUERY_KEYS
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.model.Entry
import java.time.LocalDate

/** What a statement import needs from the database. */
interface ImportRepository {
    /** The subset of [keys] already used by imported entries. */
    suspend fun existingKeys(keys: List<String>): Set<String>

    suspend fun entriesOccurredBetween(from: LocalDate, to: LocalDate): List<Entry>

    /** Saves all [entries] or none. */
    suspend fun importAll(entries: List<Entry>)
}

class RoomImportRepository(private val database: AppDatabase) : ImportRepository {
    private val entryDao = database.entryDao()

    override suspend fun existingKeys(keys: List<String>): Set<String> =
        keys.chunked(MAX_QUERY_KEYS).flatMap { entryDao.existingImportKeys(it) }.toSet()

    override suspend fun entriesOccurredBetween(from: LocalDate, to: LocalDate): List<Entry> =
        entryDao.getOccurredBetween(from, to).map { it.toDomain() }

    override suspend fun importAll(entries: List<Entry>) = database.withTransaction {
        entryDao.insertAll(entries.map { it.toEntity() })
        Unit
    }
}
