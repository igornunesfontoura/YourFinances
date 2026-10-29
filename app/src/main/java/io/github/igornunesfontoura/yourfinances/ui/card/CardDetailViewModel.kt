package io.github.igornunesfontoura.yourfinances.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.CreditCardRepository
import io.github.igornunesfontoura.yourfinances.domain.card.InvoicePayment
import io.github.igornunesfontoura.yourfinances.domain.card.InvoiceSummary
import io.github.igornunesfontoura.yourfinances.domain.card.invoices
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.ui.components.todayFlow
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

data class CardDetailUiState(
    val card: CreditCard? = null,
    val invoices: List<InvoiceSummary> = emptyList(),
    val loading: Boolean = true,
    /** The card no longer exists (deleted from its form). */
    val gone: Boolean = false,
    val actionFailed: Boolean = false,
)

class CardDetailViewModel(
    private val cardId: Long,
    private val cards: CreditCardRepository,
    private val clock: Clock,
) : ViewModel() {
    private val actionFailed = MutableStateFlow(false)

    val state: StateFlow<CardDetailUiState> = combine(cards.observeActivity(), clock.todayFlow(), actionFailed) { activities, today, failed ->
        val activity = activities.find { it.card.id == cardId }
            ?: return@combine CardDetailUiState(loading = false, gone = true)
        CardDetailUiState(
            card = activity.card,
            invoices = activity.invoices(today),
            loading = false,
            actionFailed = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailUiState())

    fun markPaid(invoice: InvoiceSummary) = write { cards.markPaid(InvoicePayment(cardId, invoice.dueDate, LocalDate.now(clock))) }

    fun unmarkPaid(invoice: InvoiceSummary) = write { cards.unmarkPaid(cardId, invoice.dueDate) }

    fun dismissError() = actionFailed.update { false }

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                actionFailed.value = true
            }
        }
    }
}
