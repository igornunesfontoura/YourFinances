package io.github.igornunesfontoura.yourfinances.ui.recurrence

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import io.github.igornunesfontoura.yourfinances.ui.components.CardListItem
import io.github.igornunesfontoura.yourfinances.ui.components.EmptyState
import io.github.igornunesfontoura.yourfinances.ui.components.IconBadge
import io.github.igornunesfontoura.yourfinances.ui.components.ListScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.cardGroup
import io.github.igornunesfontoura.yourfinances.ui.components.icon
import io.github.igornunesfontoura.yourfinances.ui.components.tint
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.label
import io.github.igornunesfontoura.yourfinances.ui.format.scheduleText
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecurrenceListRoute(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: RecurrenceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecurrenceListScreen(state, onBack, onAdd, onOpen)
}

@Composable
fun RecurrenceListScreen(state: RecurrenceListUiState, onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    ListScaffold(title = R.string.recurrences_title, onBack = onBack, addLabel = R.string.recurrence_add, onAdd = onAdd) {
        if (!state.loading && state.recurrences.isEmpty()) {
            item { EmptyState(Icons.Outlined.Autorenew, R.string.recurrences_empty) }
        }
        cardGroup("recurrences", state.recurrences) { recurrence ->
            RecurrenceRow(recurrence, onClick = { onOpen(recurrence.id) })
        }
    }
}

@Composable
private fun RecurrenceRow(recurrence: Recurrence, onClick: () -> Unit) {
    CardListItem(
        headline = recurrence.description.ifBlank { stringResource(recurrence.type.label) },
        supporting = {
            Text(stringResource(R.string.recurrence_row_details, stringResource(recurrence.type.label), scheduleText(recurrence)))
        },
        leading = { IconBadge(recurrence.type.icon, recurrence.type.tint) },
        trailing = { Text(formatCents(recurrence.amountCents), style = MaterialTheme.typography.titleSmall) },
        onClick = onClick,
    )
}
