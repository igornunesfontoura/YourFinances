package io.github.igornunesfontoura.yourfinances.data.repository

import androidx.room.withTransaction
import io.github.igornunesfontoura.yourfinances.data.db.AppDatabase
import io.github.igornunesfontoura.yourfinances.data.db.entity.toDomain
import io.github.igornunesfontoura.yourfinances.data.db.entity.toEntity
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.SavingsPocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SavingsRepository {
    fun observePockets(): Flow<List<SavingsPocket>>

    /** Every contribution and withdrawal, any pocket, any date. */
    fun observeMovements(): Flow<List<Entry>>

    suspend fun getPocket(id: Long): SavingsPocket?

    suspend fun add(pocket: SavingsPocket): Long

    suspend fun update(pocket: SavingsPocket)

    /** Deletes the pocket; returns false, changing nothing, when any entry or recurrence uses it. */
    suspend fun delete(pocket: SavingsPocket): Boolean
}

class RoomSavingsRepository(private val database: AppDatabase) : SavingsRepository {
    private val pocketDao = database.savingsPocketDao()
    private val entryDao = database.entryDao()

    override fun observePockets(): Flow<List<SavingsPocket>> = pocketDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeMovements(): Flow<List<Entry>> =
        entryDao.observeSavingsMovements().map { list -> list.map { it.toDomain() } }

    override suspend fun getPocket(id: Long): SavingsPocket? = pocketDao.getById(id)?.toDomain()

    override suspend fun add(pocket: SavingsPocket): Long = pocketDao.insert(pocket.toEntity())

    override suspend fun update(pocket: SavingsPocket) = pocketDao.update(pocket.toEntity())

    override suspend fun delete(pocket: SavingsPocket): Boolean = database.withTransaction {
        if (entryDao.countByPocket(pocket.id) > 0) return@withTransaction false
        pocketDao.delete(pocket.toEntity())
        true
    }
}
