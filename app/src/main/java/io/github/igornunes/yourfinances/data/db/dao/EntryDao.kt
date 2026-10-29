package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.igornunes.yourfinances.data.db.entity.EntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<EntryEntity>): List<Long>

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryEntity?

    @Query("SELECT * FROM entries WHERE paid_on BETWEEN :from AND :to ORDER BY paid_on, id")
    fun observePaidBetween(from: LocalDate, to: LocalDate): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE occurred_on BETWEEN :from AND :to ORDER BY occurred_on, id")
    fun observeOccurredBetween(from: LocalDate, to: LocalDate): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE installment_purchase_id = :purchaseId ORDER BY installment_number")
    fun observeInstallments(purchaseId: Long): Flow<List<EntryEntity>>

    @Query("SELECT scheduled_for FROM entries WHERE recurrence_id = :recurrenceId")
    suspend fun scheduledDates(recurrenceId: Long): List<LocalDate>

    @Query("DELETE FROM entries WHERE recurrence_id = :recurrenceId AND status = 'PREDICTED' AND scheduled_for >= :from")
    suspend fun deletePredictedFrom(recurrenceId: Long, from: LocalDate)

    /** Predicted entries the template no longer produces because of its start or end date. */
    @Query(
        "DELETE FROM entries WHERE recurrence_id = :recurrenceId AND status = 'PREDICTED' " +
            "AND (scheduled_for < :startsOn OR (:endsOn IS NOT NULL AND scheduled_for > :endsOn))",
    )
    suspend fun deletePredictedOutside(recurrenceId: Long, startsOn: LocalDate, endsOn: LocalDate?)

    @Update
    suspend fun updateAll(entries: List<EntryEntity>)

    @Query("SELECT * FROM entries WHERE credit_card_id IS NOT NULL")
    fun observeCardEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE credit_card_id = :cardId")
    suspend fun getCardEntries(cardId: Long): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE occurred_on BETWEEN :from AND :to")
    suspend fun getOccurredBetween(from: LocalDate, to: LocalDate): List<EntryEntity>

    /** Call with at most [MAX_QUERY_KEYS] keys: SQLite limits the number of bound values. */
    @Query("SELECT import_key FROM entries WHERE import_key IN (:keys)")
    suspend fun existingImportKeys(keys: List<String>): List<String>

    @Query("SELECT * FROM entries WHERE type IN ('INVESTMENT', 'WITHDRAWAL') ORDER BY occurred_on, id")
    fun observeSavingsMovements(): Flow<List<EntryEntity>>

    /** Entries and recurrences using the pocket. */
    @Query(
        "SELECT (SELECT COUNT(*) FROM entries WHERE savings_pocket_id = :pocketId) + " +
            "(SELECT COUNT(*) FROM recurrences WHERE savings_pocket_id = :pocketId)",
    )
    suspend fun countByPocket(pocketId: Long): Int

    @Query("SELECT COUNT(*) FROM entries WHERE credit_card_id = :cardId")
    suspend fun countByCard(cardId: Long): Int
}

const val MAX_QUERY_KEYS = 500
