package io.github.igornunesfontoura.yourfinances.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.CreditCardRepository
import io.github.igornunesfontoura.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunesfontoura.yourfinances.domain.subscription.SubscriptionsSummary
import io.github.igornunesfontoura.yourfinances.domain.subscription.subscriptionsSummary
import io.github.igornunesfontoura.yourfinances.ui.components.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

data class SubscriptionsUiState(
    val summary: SubscriptionsSummary = SubscriptionsSummary(emptyList(), 0),
    val cardNames: Map<Long, String> = emptyMap(),
    val loading: Boolean = true,
)

class SubscriptionsViewModel(recurrences: RecurrenceRepository, cards: CreditCardRepository, clock: Clock) : ViewModel() {
    val state: StateFlow<SubscriptionsUiState> =
        combine(recurrences.observeAll(), cards.observeAll(), clock.todayFlow()) { list, cardList, today ->
            SubscriptionsUiState(subscriptionsSummary(list, today), cardList.associate { it.id to it.name }, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsUiState())
}
