package io.github.igornunes.yourfinances.ui.recurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunes.yourfinances.domain.model.Recurrence
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RecurrenceListUiState(val recurrences: List<Recurrence> = emptyList(), val loading: Boolean = true)

class RecurrenceListViewModel(recurrences: RecurrenceRepository) : ViewModel() {
    val state: StateFlow<RecurrenceListUiState> = recurrences.observeAll()
        .map { RecurrenceListUiState(it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurrenceListUiState())
}
