package io.github.igornunes.yourfinances.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.SavingsRepository
import io.github.igornunes.yourfinances.domain.model.SavingsPocket
import io.github.igornunes.yourfinances.domain.money.centsFromAmountDigits
import io.github.igornunes.yourfinances.domain.money.normalizeAmountDigits
import io.github.igornunes.yourfinances.ui.components.FormStatus
import io.github.igornunes.yourfinances.ui.components.load
import io.github.igornunes.yourfinances.ui.components.persist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PocketFormUiState(
    val isEditing: Boolean,
    val name: String = "",
    /** Empty means no target. */
    val targetDigits: String = "",
    val deleteBlocked: Boolean = false,
    val status: FormStatus = FormStatus(),
) {
    val targetCents: Long? get() = centsFromAmountDigits(targetDigits).takeIf { it > 0 }
    val canSave: Boolean get() = !status.isBusy && name.isNotBlank()
}

/** Creates a pocket when [pocketId] is null, otherwise edits it. */
class PocketFormViewModel(pocketId: Long?, private val savings: SavingsRepository) : ViewModel() {
    private var original: SavingsPocket? = null

    private val _state = MutableStateFlow(PocketFormUiState(isEditing = pocketId != null))
    val state: StateFlow<PocketFormUiState> = _state.asStateFlow()

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    init {
        if (pocketId != null) {
            load(updateStatus, read = { savings.getPocket(pocketId) }) { pocket ->
                original = pocket
                _state.update { it.copy(name = pocket.name, targetDigits = pocket.targetCents?.toString().orEmpty()) }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }

    fun onTargetChange(input: String) = _state.update { it.copy(targetDigits = normalizeAmountDigits(input)) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val pocket = SavingsPocket(id = original?.id ?: 0, name = current.name.trim(), targetCents = current.targetCents)
        persist(updateStatus) {
            if (original == null) savings.add(pocket) else savings.update(pocket)
        }
    }

    /** A pocket with any contribution or withdrawal is kept, and the form says why. */
    fun delete() {
        val existing = original ?: return
        if (_state.value.status.isBusy) return
        _state.update { it.copy(deleteBlocked = false, status = it.status.copy(saving = true, failed = false)) }
        viewModelScope.launch {
            val outcome = try {
                savings.delete(existing)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _state.update {
                it.copy(
                    deleteBlocked = outcome == false,
                    status = it.status.copy(saving = false, failed = outcome == null, finished = outcome == true),
                )
            }
        }
    }
}
