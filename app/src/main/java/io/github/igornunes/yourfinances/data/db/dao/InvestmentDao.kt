package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.igornunes.yourfinances.data.db.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Insert
    suspend fun insert(investment: InvestmentEntity): Long

    @Update
    suspend fun update(investment: InvestmentEntity)

    @Delete
    suspend fun delete(investment: InvestmentEntity)

    @Query("SELECT * FROM investments ORDER BY invested_on")
    fun observeAll(): Flow<List<InvestmentEntity>>
}
