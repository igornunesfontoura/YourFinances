package io.github.igornunesfontoura.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.igornunesfontoura.yourfinances.data.db.entity.SavingsPocketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsPocketDao {
    @Insert
    suspend fun insert(pocket: SavingsPocketEntity): Long

    @Update
    suspend fun update(pocket: SavingsPocketEntity)

    @Delete
    suspend fun delete(pocket: SavingsPocketEntity)

    @Query("SELECT * FROM savings_pockets WHERE id = :id")
    suspend fun getById(id: Long): SavingsPocketEntity?

    @Query("SELECT * FROM savings_pockets ORDER BY name")
    fun observeAll(): Flow<List<SavingsPocketEntity>>
}
