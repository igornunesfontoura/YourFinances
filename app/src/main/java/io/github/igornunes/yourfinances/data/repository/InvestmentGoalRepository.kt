package io.github.igornunes.yourfinances.data.repository

import io.github.igornunes.yourfinances.data.db.dao.InvestmentGoalDao
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.model.InvestmentGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface InvestmentGoalRepository {
    /** Every goal, oldest first. */
    fun observeAll(): Flow<List<InvestmentGoal>>

    /** Saves [goal], replacing a goal that starts in the same month. */
    suspend fun set(goal: InvestmentGoal)
}

class RoomInvestmentGoalRepository(private val dao: InvestmentGoalDao) : InvestmentGoalRepository {
    override fun observeAll(): Flow<List<InvestmentGoal>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun set(goal: InvestmentGoal) = dao.upsert(goal.toEntity())
}
