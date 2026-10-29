package io.github.igornunesfontoura.yourfinances.ui.recurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.CategoryRepository
import io.github.igornunesfontoura.yourfinances.data.repository.CreditCardRepository
import io.github.igornunesfontoura.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunesfontoura.yourfinances.data.repository.SavingsRepository
import io.github.igornunesfontoura.yourfinances.domain.model.Category
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import io.github.igornunesfontoura.yourfinances.domain.model.SavingsPocket
import io.github.igornunesfontoura.yourfinances.domain.money.centsFromAmountDigits
import io.github.igornunesfontoura.yourfinances.domain.money.normalizeAmountDigits
import io.github.igornunesfontoura.yourfinances.domain.recurrence.predictionWindow
import io.github.igornunesfontoura.yourfinances.ui.components.FormStatus
import io.github.igornunesfontoura.yourfinances.ui.components.load
import io.github.igornunesfontoura.yourfinances.ui.components.persist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val pockets: List<SavingsPocket> = emptyList(),
    val pocketId: Long? = null,
    val categories: List<Category> = emptyList(),
    val categoryId: Long? = null,
    val cards: List<CreditCard> = emptyList(),
    val cardId: Long? = null,
    val status: FormStatus = FormStatus(),
) {
    val showsPocket: Boolean get() = type.movesSavings && pockets.isNotEmpty()
    val typeCategories: List<Category> get() = categories.filter { it.kind == type.categoryKind }
    val showsCategory: Boolean get() = typeCategories.isNotEmpty()
    val showsPayment: Boolean get() = type.canUseCard && cards.isNotEmpty()
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
    savings: SavingsRepository,
    categories: CategoryRepository,
    cards: CreditCardRepository,
) : ViewModel() {
    private var original: Recurrence? = null

    private val _state = MutableStateFlow(RecurrenceFormUiState(isEditing = recurrenceId != null, today = LocalDate.now(clock)))
    val state: StateFlow<RecurrenceFormUiState> = _state.asStateFlow()

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    init {
        viewModelScope.launch {
            savings.observePockets().collect { list ->
                // A pocket deleted meanwhile cannot be saved into.
                _state.update { state -> state.copy(pockets = list, pocketId = state.pocketId.takeIf { id -> list.any { it.id == id } }) }
            }
        }
        viewModelScope.launch {
            categories.observeAll().collect { list ->
                _state.update { state ->
                    // A category deleted meanwhile, or of the other kind, cannot be saved into.
                    state.copy(
                        categories = list,
                        categoryId = state.categoryId.takeIf { id -> list.any { it.id == id && it.kind == state.type.categoryKind } },
                    )
                }
            }
        }
        viewModelScope.launch {
            cards.observeAll().collect { list ->
                _state.update { state -> state.copy(cards = list, cardId = state.cardId.takeIf { id -> list.any { it.id == id } }) }
            }
        }
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
                        pocketId = recurrence.savingsPocketId,
                        categoryId = recurrence.categoryId,
                        cardId = recurrence.creditCardId,
                    )
                }
            }
        }
    }

    fun onAmountChange(input: String) = _state.update { it.copy(amountDigits = normalizeAmountDigits(input)) }

    fun onTypeChange(type: EntryType) = _state.update {
        it.copy(
            type = type,
            pocketId = it.pocketId.takeIf { type.movesSavings },
            categoryId = it.categoryId.takeIf { _ -> type.categoryKind == it.type.categoryKind },
            cardId = it.cardId.takeIf { type.canUseCard },
        )
    }

    fun onPocketChange(pocketId: Long?) = _state.update { it.copy(pocketId = pocketId) }

    fun onCategoryChange(categoryId: Long?) = _state.update { it.copy(categoryId = categoryId) }

    fun onCardChange(cardId: Long?) = _state.update { it.copy(cardId = cardId) }

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
            categoryId = current.categoryId.takeIf { current.type.categoryKind != null },
            savingsPocketId = current.pocketId.takeIf { current.type.movesSavings },
            creditCardId = current.cardId.takeIf { current.type.canUseCard },
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
