package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceSkipEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RecurrenceDao {
    @Query("SELECT COUNT(*) FROM recurrences WHERE credit_card_id = :cardId")
    suspend fun countByCard(cardId: Long): Int
    @Insert
    suspend fun insert(recurrence: RecurrenceEntity): Long

    @Update
    suspend fun update(recurrence: RecurrenceEntity)

    @Delete
    suspend fun delete(recurrence: RecurrenceEntity)

    @Query("SELECT * FROM recurrences WHERE id = :id")
    suspend fun getById(id: Long): RecurrenceEntity?

    @Query("SELECT * FROM recurrences")
    suspend fun getAll(): List<RecurrenceEntity>

    @Query("SELECT * FROM recurrences ORDER BY description")
    fun observeAll(): Flow<List<RecurrenceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkip(skip: RecurrenceSkipEntity)

    @Query("SELECT slot FROM recurrence_skips WHERE recurrence_id = :recurrenceId")
    suspend fun skippedSlots(recurrenceId: Long): List<LocalDate>
}
