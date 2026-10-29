package io.github.igornunesfontoura.yourfinances.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.SavingsRepository
import io.github.igornunesfontoura.yourfinances.domain.savings.SavingsSummary
import io.github.igornunesfontoura.yourfinances.domain.savings.savingsSummary
import io.github.igornunesfontoura.yourfinances.ui.components.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

data class SavingsUiState(
    val summary: SavingsSummary = SavingsSummary(0, emptyList(), emptyList()),
    val loading: Boolean = true,
)

class SavingsViewModel(savings: SavingsRepository, clock: Clock) : ViewModel() {
    val state: StateFlow<SavingsUiState> =
        combine(savings.observeMovements(), savings.observePockets(), clock.todayFlow()) { movements, pockets, today ->
            SavingsUiState(savingsSummary(movements, pockets, today), loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsUiState())
}
