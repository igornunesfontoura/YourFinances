package io.github.igornunesfontoura.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.igornunesfontoura.yourfinances.data.db.entity.InvestmentGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentGoalDao {
    @Upsert
    suspend fun upsert(goal: InvestmentGoalEntity)

    @Query("SELECT * FROM investment_goals ORDER BY starts_in")
    fun observeAll(): Flow<List<InvestmentGoalEntity>>
}
