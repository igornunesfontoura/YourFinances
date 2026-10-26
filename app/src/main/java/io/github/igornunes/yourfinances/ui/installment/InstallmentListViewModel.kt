package io.github.igornunes.yourfinances.ui.installment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.InstallmentRepository
import io.github.igornunes.yourfinances.domain.installment.InstallmentProgress
import io.github.igornunes.yourfinances.domain.installment.pendingInstallmentsByMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class InstallmentListUiState(
    val committedByMonth: List<Pair<YearMonth, Long>> = emptyList(),
    val active: List<InstallmentProgress> = emptyList(),
    val finished: List<InstallmentProgress> = emptyList(),
    val loading: Boolean = true,
    val actionFailed: Boolean = false,
)

class InstallmentListViewModel(private val installments: InstallmentRepository, private val clock: Clock) : ViewModel() {
    private val actionFailed = MutableStateFlow(false)

    val state: StateFlow<InstallmentListUiState> = combine(installments.observePlans(), actionFailed) { plans, failed ->
        val today = LocalDate.now(clock)
        val progress = plans.map { it.progress(today) }.sortedBy { it.endsOn }
        InstallmentListUiState(
            committedByMonth = pendingInstallmentsByMonth(plans.flatMap { it.installments }, today).toList(),
            active = progress.filterNot { it.isFinished },
            finished = progress.filter { it.isFinished }.reversed(),
            loading = false,
            actionFailed = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentListUiState())

    fun delete(purchaseId: Long) {
        viewModelScope.launch {
            try {
                installments.delete(purchaseId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                actionFailed.value = true
            }
        }
    }

    fun dismissError() = actionFailed.update { false }
}
