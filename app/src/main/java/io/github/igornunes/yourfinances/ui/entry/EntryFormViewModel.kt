package io.github.igornunes.yourfinances.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.CreditCardRepository
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.data.repository.InstallmentRepository
import io.github.igornunes.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunes.yourfinances.data.repository.SavingsRepository
import io.github.igornunes.yourfinances.domain.card.paymentDateFor
import io.github.igornunes.yourfinances.domain.entry.deletesWholePurchase
import io.github.igornunes.yourfinances.domain.entry.edited
import io.github.igornunes.yourfinances.domain.entry.isQuickEditable
import io.github.igornunes.yourfinances.domain.entry.newEntry
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.SavingsPocket
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
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class EntryFormUiState(
    val isEditing: Boolean,
    val amountDigits: String = "",
    val type: EntryType = EntryType.FLEXIBLE_EXPENSE,
    val description: String = "",
    val occurredOn: LocalDate,
    val today: LocalDate,
    val canEditValues: Boolean = true,
    val isRecurring: Boolean = false,
    val applyToUpcoming: Boolean = false,
    val deletesWholePurchase: Boolean = false,
    val cards: List<CreditCard> = emptyList(),
    val cardId: Long? = null,
    val pockets: List<SavingsPocket> = emptyList(),
    val pocketId: Long? = null,
    val status: FormStatus = FormStatus(),
) {
    val amountCents: Long get() = centsFromAmountDigits(amountDigits)
    val card: CreditCard? get() = cards.find { it.id == cardId }
    val showsPayment: Boolean get() = canEditValues && type.canUseCard && cards.isNotEmpty()
    val showsPocket: Boolean get() = canEditValues && type.movesSavings && pockets.isNotEmpty()

    /** Due date of the invoice the entry goes into, when paid with a card. */
    val cardDueDate: LocalDate? get() = card?.paymentDateFor(occurredOn)
    val isBusy: Boolean get() = status.isBusy
    /** The entry's card is known but the card list has not arrived yet. */
    private val cardPending: Boolean get() = cardId != null && card == null
    val canSave: Boolean get() = !isBusy && amountCents > 0 && !cardPending

    /** Recurrences do not hold a card yet, so applying a card occurrence to the next ones is not offered. */
    val canApplyToUpcoming: Boolean get() = isRecurring && cardId == null
}

/** Creates a new entry when [entryId] is null, otherwise edits that entry. */
class EntryFormViewModel(
    entryId: Long?,
    private val entries: EntryRepository,
    private val recurrences: RecurrenceRepository,
    private val installments: InstallmentRepository,
    cards: CreditCardRepository,
    private val clock: Clock,
    savings: SavingsRepository,
) : ViewModel() {
    private var original: Entry? = null

    private val _state = LocalDate.now(clock).let { today ->
        MutableStateFlow(EntryFormUiState(isEditing = entryId != null, occurredOn = today, today = today))
    }
    val state: StateFlow<EntryFormUiState> = _state.asStateFlow()

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    init {
        viewModelScope.launch { cards.observeAll().collect { list -> _state.update { it.copy(cards = list) } } }
        viewModelScope.launch {
            savings.observePockets().collect { list ->
                // A pocket deleted meanwhile cannot be saved into.
                _state.update { state -> state.copy(pockets = list, pocketId = state.pocketId.takeIf { id -> list.any { it.id == id } }) }
            }
        }
        if (entryId != null) {
            load(updateStatus, read = { entries.getById(entryId) }) { entry ->
                original = entry
                _state.update {
                    it.copy(
                        amountDigits = entry.amountCents.toString(),
                        type = entry.type,
                        description = entry.description,
                        occurredOn = entry.occurredOn,
                        canEditValues = entry.isQuickEditable,
                        isRecurring = entry.recurrenceId != null && entry.isQuickEditable,
                        deletesWholePurchase = entry.deletesWholePurchase,
                        cardId = entry.creditCardId,
                        pocketId = entry.savingsPocketId,
                    )
                }
            }
        }
    }

    fun onAmountChange(input: String) = _state.update { it.copy(amountDigits = normalizeAmountDigits(input)) }

    fun onTypeChange(type: EntryType) = _state.update {
        it.copy(
            type = type,
            cardId = it.cardId.takeIf { type.canUseCard },
            pocketId = it.pocketId.takeIf { type.movesSavings },
        )
    }

    fun onCardChange(cardId: Long?) = _state.update { it.copy(cardId = cardId) }

    fun onPocketChange(pocketId: Long?) = _state.update { it.copy(pocketId = pocketId) }

    fun onDescriptionChange(description: String) = _state.update { it.copy(description = description) }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(occurredOn = date) }

    fun onApplyToUpcomingChange(apply: Boolean) = _state.update { it.copy(applyToUpcoming = apply) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val existing = original
        persist(updateStatus) {
            if (existing == null) {
                entries.add(
                    newEntry(current.type, current.amountCents, current.description, current.occurredOn, current.card, current.pocketId),
                )
                return@persist
            }
            val edited = existing.edited(
                current.type, current.amountCents, current.description, current.occurredOn, current.card, current.pocketId,
            )
            if (current.canApplyToUpcoming && current.applyToUpcoming) {
                recurrences.applyToUpcoming(edited, predictionWindow(LocalDate.now(clock)))
            } else {
                entries.update(edited)
            }
        }
    }

    fun delete() {
        val existing = original ?: return
        if (_state.value.isBusy) return
        persist(updateStatus) {
            val purchaseId = existing.installmentPurchaseId
            if (purchaseId != null) installments.delete(purchaseId) else entries.delete(existing)
        }
    }
}
