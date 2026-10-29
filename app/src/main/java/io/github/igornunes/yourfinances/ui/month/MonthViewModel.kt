package io.github.igornunes.yourfinances.ui.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.CreditCardRepository
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.domain.budget.MonthTotals
import io.github.igornunes.yourfinances.domain.budget.monthTotals
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.entry.confirmed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DayGroup(val date: LocalDate, val entries: List<Entry>)

data class MonthUiState(
    val month: YearMonth,
    val basis: DateBasis,
    val totals: MonthTotals = MonthTotals(),
    val days: List<DayGroup> = emptyList(),
    val loading: Boolean = true,
    val actionFailed: Boolean = false,
    val cardNames: Map<Long, String> = emptyMap(),
)

/** Opens on [initialMonth], the month the main screen was showing. */
@OptIn(ExperimentalCoroutinesApi::class)
class MonthViewModel(
    initialMonth: YearMonth,
    private val entries: EntryRepository,
    cards: CreditCardRepository,
) : ViewModel() {
    private val selection = MutableStateFlow(initialMonth to DateBasis.ACCRUAL)
    private val actionFailed = MutableStateFlow(false)

    val state: StateFlow<MonthUiState> = selection
        .flatMapLatest { (month, basis) ->
            entries.observeMonth(month, basis).map { list ->
                MonthUiState(
                    month = month,
                    basis = basis,
                    totals = monthTotals(list),
                    days = list.groupBy { basis.dateOf(it) }
                        .toSortedMap(reverseOrder())
                        .map { (date, dayEntries) -> DayGroup(date, dayEntries) },
                    loading = false,
                )
            }
        }
        .combine(actionFailed) { state, failed -> state.copy(actionFailed = failed) }
        .combine(cards.observeAll()) { state, list -> state.copy(cardNames = list.associate { it.id to it.name }) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            selection.value.let { (month, basis) -> MonthUiState(month, basis) },
        )

    fun previousMonth() = selection.update { (month, basis) -> month.minusMonths(1) to basis }

    fun nextMonth() = selection.update { (month, basis) -> month.plusMonths(1) to basis }

    fun setBasis(basis: DateBasis) = selection.update { (month, _) -> month to basis }

    /** One tap: the predicted entry happened with the predicted values. */
    fun confirm(entry: Entry) {
        viewModelScope.launch {
            try {
                entries.update(entry.confirmed())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                actionFailed.value = true
            }
        }
    }

    fun dismissError() = actionFailed.update { false }
}
