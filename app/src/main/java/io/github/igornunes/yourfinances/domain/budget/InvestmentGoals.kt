package io.github.igornunes.yourfinances.domain.budget

import io.github.igornunes.yourfinances.domain.model.InvestmentGoal
import java.time.YearMonth

/** The goal in force in [month]: the latest one starting in or before it, or null when none does. */
fun List<InvestmentGoal>.goalFor(month: YearMonth): InvestmentGoal? =
    filter { it.startsIn <= month }.maxByOrNull { it.startsIn }

/** The first goal starting after [month], which ends the goal in force there. */
fun List<InvestmentGoal>.nextGoalAfter(month: YearMonth): InvestmentGoal? =
    filter { it.startsIn > month }.minByOrNull { it.startsIn }
