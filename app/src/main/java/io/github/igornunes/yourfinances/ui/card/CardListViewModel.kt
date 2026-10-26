package io.github.igornunes.yourfinances.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.CreditCardRepository
import io.github.igornunes.yourfinances.domain.card.InvoiceSummary
import io.github.igornunes.yourfinances.domain.card.awaitingPayment
import io.github.igornunes.yourfinances.domain.card.currentInvoice
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.ui.components.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

data class CardOverview(val card: CreditCard, val current: InvoiceSummary, val toPay: InvoiceSummary?)

data class CardListUiState(val cards: List<CardOverview> = emptyList(), val loading: Boolean = true)

class CardListViewModel(cards: CreditCardRepository, clock: Clock) : ViewModel() {
    val state: StateFlow<CardListUiState> = combine(cards.observeActivity(), clock.todayFlow()) { activities, today ->
        CardListUiState(
            cards = activities.map {
                CardOverview(it.card, it.currentInvoice(today), it.awaitingPayment(today).firstOrNull())
            },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardListUiState())
}
