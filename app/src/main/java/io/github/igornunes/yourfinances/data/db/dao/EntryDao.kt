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

    @Query("SELECT * FROM entries WHERE installment_purchase_id IS NOT NULL AND paid_on >= :from ORDER BY paid_on")
    fun observeInstallmentsPaidFrom(from: LocalDate): Flow<List<EntryEntity>>

    @Query("SELECT scheduled_for FROM entries WHERE recurrence_id = :recurrenceId")
    suspend fun scheduledDates(recurrenceId: Long): List<LocalDate>
}
