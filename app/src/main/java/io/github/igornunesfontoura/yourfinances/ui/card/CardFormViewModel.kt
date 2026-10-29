package io.github.igornunesfontoura.yourfinances.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.CreditCardRepository
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.ui.components.FormStatus
import io.github.igornunesfontoura.yourfinances.ui.components.load
import io.github.igornunesfontoura.yourfinances.ui.components.persist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class CardFormUiState(
    val isEditing: Boolean,
    val name: String = "",
    val closingText: String = "",
    val dueText: String = "",
    val deleteBlocked: Boolean = false,
    val status: FormStatus = FormStatus(),
) {
    val closingDay: Int? get() = closingText.toIntOrNull()?.takeIf { it in 1..31 }
    val dueDay: Int? get() = dueText.toIntOrNull()?.takeIf { it in 1..31 }
    val canSave: Boolean get() = !status.isBusy && name.isNotBlank() && closingDay != null && dueDay != null
}

/** Creates a card when [cardId] is null, otherwise edits it. */
class CardFormViewModel(
    cardId: Long?,
    private val cards: CreditCardRepository,
    private val clock: Clock,
) : ViewModel() {
    private var original: CreditCard? = null

    private val _state = MutableStateFlow(CardFormUiState(isEditing = cardId != null))
    val state: StateFlow<CardFormUiState> = _state.asStateFlow()

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    init {
        if (cardId != null) {
            load(updateStatus, read = { cards.getById(cardId) }) { card ->
                original = card
                _state.update {
                    it.copy(name = card.name, closingText = card.closingDay.toString(), dueText = card.dueDay.toString())
                }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }

    fun onClosingChange(day: String) = _state.update { it.copy(closingText = day) }

    fun onDueChange(day: String) = _state.update { it.copy(dueText = day) }

    fun save() {
        val current = _state.value
        val closing = current.closingDay
        val due = current.dueDay
        if (!current.canSave || closing == null || due == null) return
        val card = CreditCard(id = original?.id ?: 0, name = current.name.trim(), closingDay = closing, dueDay = due)
        persist(updateStatus) {
            if (original == null) cards.add(card) else cards.update(card, LocalDate.now(clock))
        }
    }

    /** A card with anything charged to it is kept, and the form says why. */
    fun delete() {
        val existing = original ?: return
        if (_state.value.status.isBusy) return
        _state.update { it.copy(deleteBlocked = false, status = it.status.copy(saving = true, failed = false)) }
        viewModelScope.launch {
            val outcome = try {
                cards.delete(existing)
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
