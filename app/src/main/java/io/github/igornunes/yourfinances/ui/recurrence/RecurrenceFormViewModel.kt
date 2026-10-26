package io.github.igornunes.yourfinances.ui.recurrence

import androidx.lifecycle.ViewModel
import io.github.igornunes.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import io.github.igornunes.yourfinances.domain.money.centsFromAmountDigits
import io.github.igornunes.yourfinances.domain.money.normalizeAmountDigits
import io.github.igornunes.yourfinances.domain.recurrence.predictionWindow
import io.github.igornunes.yourfinances.ui.components.FormStatus
import io.github.igornunes.yourfinances.ui.components.load
import io.github.igornunes.yourfinances.ui.components.persist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate

data class RecurrenceFormUiState(
    val isEditing: Boolean,
    val today: LocalDate,
    val amountDigits: String = "",
    val type: EntryType = EntryType.FIXED_EXPENSE,
    val description: String = "",
    val frequency: Frequency = Frequency.MONTHLY,
    val dayText: String = today.dayOfMonth.toString(),
    val startsOn: LocalDate = today,
    val endsOn: LocalDate? = null,
    val status: FormStatus = FormStatus(),
) {
    val amountCents: Long get() = centsFromAmountDigits(amountDigits)
    val dayOfMonth: Int? get() = dayText.toIntOrNull()?.takeIf { it in 1..31 }
    val endsBeforeStart: Boolean get() = endsOn != null && endsOn.isBefore(startsOn)
    val isBusy: Boolean get() = status.isBusy
    val canSave: Boolean get() = !isBusy && amountCents > 0 && dayOfMonth != null && !endsBeforeStart
}

/** Creates a recurrence when [recurrenceId] is null, otherwise edits it. */
class RecurrenceFormViewModel(
    recurrenceId: Long?,
    private val recurrences: RecurrenceRepository,
    private val clock: Clock,
) : ViewModel() {
    private var original: Recurrence? = null

    private val _state = MutableStateFlow(RecurrenceFormUiState(isEditing = recurrenceId != null, today = LocalDate.now(clock)))
    val state: StateFlow<RecurrenceFormUiState> = _state.asStateFlow()

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    init {
        if (recurrenceId != null) {
            load(updateStatus, read = { recurrences.getById(recurrenceId) }) { recurrence ->
                original = recurrence
                _state.update {
                    it.copy(
                        amountDigits = recurrence.amountCents.toString(),
                        type = recurrence.type,
                        description = recurrence.description,
                        frequency = recurrence.frequency,
                        dayText = recurrence.dayOfMonth.toString(),
                        startsOn = recurrence.startsOn,
                        endsOn = recurrence.endsOn,
                    )
                }
            }
        }
    }

    fun onAmountChange(input: String) = _state.update { it.copy(amountDigits = normalizeAmountDigits(input)) }

    fun onTypeChange(type: EntryType) = _state.update { it.copy(type = type) }

    fun onDescriptionChange(description: String) = _state.update { it.copy(description = description) }

    fun onFrequencyChange(frequency: Frequency) = _state.update { it.copy(frequency = frequency) }

    fun onDayChange(day: String) = _state.update { it.copy(dayText = day) }

    fun onStartChange(date: LocalDate) = _state.update { it.copy(startsOn = date) }

    fun onEndChange(date: LocalDate?) = _state.update { it.copy(endsOn = date) }

    fun save() {
        val current = _state.value
        val day = current.dayOfMonth
        if (!current.canSave || day == null) return
        val recurrence = Recurrence(
            id = original?.id ?: 0,
            type = current.type,
            amountCents = current.amountCents,
            description = current.description.trim(),
            frequency = current.frequency,
            dayOfMonth = day,
            startsOn = current.startsOn,
            endsOn = current.endsOn,
            categoryId = original?.categoryId,
        )
        val window = predictionWindow(LocalDate.now(clock))
        persist(updateStatus) {
            if (original == null) recurrences.add(recurrence, window) else recurrences.update(recurrence, window)
        }
    }

    fun delete() {
        val existing = original ?: return
        if (_state.value.isBusy) return
        persist(updateStatus) { recurrences.delete(existing, predictionWindow(LocalDate.now(clock))) }
    }
}
