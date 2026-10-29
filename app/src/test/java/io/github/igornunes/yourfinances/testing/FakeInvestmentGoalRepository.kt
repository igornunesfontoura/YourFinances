package io.github.igornunes.yourfinances.testing

import io.github.igornunes.yourfinances.data.repository.InvestmentGoalRepository
import io.github.igornunes.yourfinances.domain.model.InvestmentGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeInvestmentGoalRepository(initial: List<InvestmentGoal> = emptyList()) : InvestmentGoalRepository {
    val goals = MutableStateFlow(initial)
    var failWrites = false

    override fun observeAll(): Flow<List<InvestmentGoal>> = goals

    override suspend fun set(goal: InvestmentGoal) {
        check(!failWrites) { "write failed" }
        goals.update { list -> (list.filterNot { it.startsIn == goal.startsIn } + goal).sortedBy { it.startsIn } }
    }
}
