package io.github.igornunesfontoura.yourfinances.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.card.InvoiceStatus
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.components.DetailText
import io.github.igornunesfontoura.yourfinances.ui.components.EmptyState
import io.github.igornunesfontoura.yourfinances.ui.components.IconBadge
import io.github.igornunesfontoura.yourfinances.ui.components.ListScaffold
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatShortDate
import org.koin.androidx.compose.koinViewModel

@Composable
fun CardListRoute(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit, viewModel: CardListViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CardListScreen(state, onBack, onAdd, onOpen)
}

@Composable
fun CardListScreen(state: CardListUiState, onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    ListScaffold(title = R.string.cards_title, onBack = onBack, addLabel = R.string.card_add, onAdd = onAdd) {
        if (!state.loading && state.cards.isEmpty()) {
            item { EmptyState(Icons.Outlined.CreditCard, R.string.cards_empty) }
        }
        items(state.cards, key = { it.card.id }) { overview -> CardTile(overview) { onOpen(overview.card.id) } }
    }
}

/** The card's current invoice in large, and the invoice still to pay, if any. */
@Composable
private fun CardTile(overview: CardOverview, onClick: () -> Unit) {
    val card = overview.card
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Outlined.CreditCard, MaterialTheme.colorScheme.primary)
                Column {
                    Text(card.name, style = MaterialTheme.typography.titleMedium)
                    DetailText(stringResource(R.string.card_days, card.closingDay, card.dueDay))
                }
            }
            Text(
                stringResource(R.string.card_current_invoice, formatCents(overview.current.totalCents), formatShortDate(overview.current.dueDate)),
                style = MaterialTheme.typography.bodyLarge,
            )
            overview.toPay?.let { invoice ->
                val overdue = invoice.status == InvoiceStatus.OVERDUE
                Text(
                    text = stringResource(
                        if (overdue) R.string.card_overdue_invoice else R.string.card_closed_invoice,
                        formatCents(invoice.totalCents),
                        formatShortDate(invoice.dueDate),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
