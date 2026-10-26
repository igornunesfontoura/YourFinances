package io.github.igornunes.yourfinances.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.domain.entry.edited
import io.github.igornunes.yourfinances.domain.entry.isQuickDeletable
import io.github.igornunes.yourfinances.domain.entry.isQuickEditable
import io.github.igornunes.yourfinances.domain.entry.newEntry
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.money.centsFromAmountDigits
import io.github.igornunes.yourfinances.domain.money.normalizeAmountDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class EntryFormUiState(
    val isEditing: Boolean,
    val loading: Boolean,
    val amountDigits: String = "",
    val type: EntryType = EntryType.FLEXIBLE_EXPENSE,
    val description: String = "",
    val occurredOn: LocalDate,
    val today: LocalDate,
    val canEditValues: Boolean = true,
    val canDelete: Boolean = true,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val finished: Boolean = false,
) {
    val amountCents: Long get() = centsFromAmountDigits(amountDigits)
    val isBusy: Boolean get() = loading || saving || finished
    val canSave: Boolean get() = !isBusy && amountCents > 0
}

/** Creates a new entry when [entryId] is null, otherwise edits that entry. */
class EntryFormViewModel(
    private val entryId: Long?,
    private val entries: EntryRepository,
    clock: Clock,
) : ViewModel() {
    private var original: Entry? = null

    private val today = LocalDate.now(clock)
    private val _state = MutableStateFlow(
        EntryFormUiState(isEditing = entryId != null, loading = entryId != null, occurredOn = today, today = today),
    )
    val state: StateFlow<EntryFormUiState> = _state.asStateFlow()

    init {
        if (entryId != null) {
            viewModelScope.launch {
                val entry = try {
                    entries.getById(entryId)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (entry == null) {
                    _state.update { it.copy(finished = true) }
                    return@launch
                }
                original = entry
                _state.update {
                    it.copy(
                        loading = false,
                        amountDigits = entry.amountCents.toString(),
                        type = entry.type,
                        description = entry.description,
                        occurredOn = entry.occurredOn,
                        canEditValues = entry.isQuickEditable,
                        canDelete = entry.isQuickDeletable,
                    )
                }
            }
        }
    }

    fun onAmountChange(input: String) = _state.update { it.copy(amountDigits = normalizeAmountDigits(input)) }

    fun onTypeChange(type: EntryType) = _state.update { it.copy(type = type) }

    fun onDescriptionChange(description: String) = _state.update { it.copy(description = description) }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(occurredOn = date) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val existing = original
        persist {
            if (existing == null) {
                entries.add(newEntry(current.type, current.amountCents, current.description, current.occurredOn))
            } else {
                entries.update(existing.edited(current.type, current.amountCents, current.description, current.occurredOn))
            }
        }
    }

    fun delete() {
        val existing = original ?: return
        if (_state.value.isBusy || !existing.isQuickDeletable) return
        persist { entries.delete(existing) }
    }

    /** Runs one write at a time; the form closes on success and stays open on failure. */
    private fun persist(write: suspend () -> Unit) {
        _state.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                write()
                _state.update { it.copy(saving = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Nothing is logged: the entry holds financial data.
                _state.update { it.copy(saving = false, failed = true) }
            }
        }
    }
}
