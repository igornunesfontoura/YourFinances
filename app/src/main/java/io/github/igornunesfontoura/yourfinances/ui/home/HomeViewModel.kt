package io.github.igornunesfontoura.yourfinances.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.EntryRepository
import io.github.igornunesfontoura.yourfinances.data.repository.InvestmentGoalRepository
import io.github.igornunesfontoura.yourfinances.domain.budget.AvailableToSpend
import io.github.igornunesfontoura.yourfinances.domain.budget.availableToSpend
import io.github.igornunesfontoura.yourfinances.domain.budget.goalFor
import io.github.igornunesfontoura.yourfinances.domain.model.DateBasis
import io.github.igornunesfontoura.yourfinances.domain.recurrence.isBeyondPredictions
import io.github.igornunesfontoura.yourfinances.domain.savings.savedInMonthCents
import io.github.igornunesfontoura.yourfinances.ui.components.todayFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.YearMonth

/** The main screen counts card purchases in the month they were made, not in the invoice month. */
val HOME_BASIS = DateBasis.ACCRUAL

data class HomeUiState(
    val available: AvailableToSpend,
    val hasGoal: Boolean = false,
    /** Contributions minus withdrawals confirmed this month, to compare with the goal. */
    val savedCents: Long = 0,
    /** Recurring entries are not generated for this month yet, so the number leaves them out. */
    val beyondPredictions: Boolean = false,
    val loading: Boolean = true,
) {
    val month: YearMonth get() = available.month
}

private fun emptyState(month: YearMonth) = HomeUiState(availableToSpend(emptyList(), month, 0, HOME_BASIS))

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(entries: EntryRepository, goals: InvestmentGoalRepository, clock: Clock) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now(clock))

    /** The month the arrows chose, even before its numbers arrive in [state]. */
    val selectedMonth: YearMonth get() = month.value

    val state: StateFlow<HomeUiState> = month
        .flatMapLatest { month ->
            combine(entries.observeMonth(month, HOME_BASIS), goals.observeAll(), clock.todayFlow()) { list, goalList, today ->
                val goal = goalList.goalFor(month)
                HomeUiState(
                    available = availableToSpend(list, month, goal?.amountCents ?: 0, HOME_BASIS),
                    hasGoal = goal != null,
                    savedCents = savedInMonthCents(list, month, today),
                    beyondPredictions = isBeyondPredictions(month, today),
                    loading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyState(month.value))

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }
}
