package io.github.igornunesfontoura.yourfinances.ui.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.CreditCardRepository
import io.github.igornunesfontoura.yourfinances.data.repository.EntryRepository
import io.github.igornunesfontoura.yourfinances.domain.budget.MonthTotals
import io.github.igornunesfontoura.yourfinances.domain.budget.monthTotals
import io.github.igornunesfontoura.yourfinances.domain.model.DateBasis
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.entry.confirmed
import io.github.igornunesfontoura.yourfinances.ui.components.SelectedMonth
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

/** Shows the month of [selectedMonth], shared with the main screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class MonthViewModel(
    private val selectedMonth: SelectedMonth,
    private val entries: EntryRepository,
    cards: CreditCardRepository,
) : ViewModel() {
    private val basis = MutableStateFlow(DateBasis.ACCRUAL)
    private val selection = selectedMonth.value.combine(basis) { month, basis -> month to basis }
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
            MonthUiState(selectedMonth.value.value, basis.value),
        )

    fun previousMonth() = selectedMonth.previous()

    fun nextMonth() = selectedMonth.next()

    fun setBasis(basis: DateBasis) {
        this.basis.value = basis
    }

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
