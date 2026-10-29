package io.github.igornunes.yourfinances.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.CategoryRepository
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.domain.budget.CategorySpending
import io.github.igornunes.yourfinances.domain.budget.categorySpending
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.report.MonthEvolution
import io.github.igornunes.yourfinances.domain.report.monthlyEvolution
import io.github.igornunes.yourfinances.domain.report.monthsEndingAt
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

/** Reports use the same basis as the main screen, so the numbers agree with it. */
val REPORTS_BASIS = DateBasis.ACCRUAL

const val EVOLUTION_MONTHS = 12

data class ReportsUiState(
    val month: YearMonth,
    val spending: List<CategorySpending> = emptyList(),
    /** The [EVOLUTION_MONTHS] months ending at [month], oldest first. */
    val evolution: List<MonthEvolution> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(entries: EntryRepository, categories: CategoryRepository, clock: Clock) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now(clock))

    val state: StateFlow<ReportsUiState> = month
        .flatMapLatest { month ->
            val months = monthsEndingAt(month, EVOLUTION_MONTHS)
            combine(entries.observeMonths(months.first(), month, REPORTS_BASIS), categories.observeAll()) { list, categoryList ->
                ReportsUiState(
                    month = month,
                    spending = categorySpending(list, categoryList, month, REPORTS_BASIS),
                    evolution = monthlyEvolution(list, months, REPORTS_BASIS),
                    loading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState(month.value))

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }
}
