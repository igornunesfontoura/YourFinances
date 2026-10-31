package io.github.igornunesfontoura.yourfinances.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.savings.MonthSaved
import io.github.igornunesfontoura.yourfinances.domain.savings.PocketBalance
import io.github.igornunesfontoura.yourfinances.ui.components.CardListItem
import io.github.igornunesfontoura.yourfinances.ui.components.DetailText
import io.github.igornunesfontoura.yourfinances.ui.components.EmptyState
import io.github.igornunesfontoura.yourfinances.ui.components.IconBadge
import io.github.igornunesfontoura.yourfinances.ui.components.ListScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.SummaryCard
import io.github.igornunesfontoura.yourfinances.ui.components.cardGroup
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatMonth
import io.github.igornunesfontoura.yourfinances.ui.theme.finance
import org.koin.androidx.compose.koinViewModel

@Composable
fun SavingsRoute(
    onAddPocket: () -> Unit,
    onOpenPocket: (Long) -> Unit,
    viewModel: SavingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavingsScreen(state, onAddPocket, onOpenPocket)
}

@Composable
fun SavingsScreen(state: SavingsUiState, onAddPocket: () -> Unit, onOpenPocket: (Long) -> Unit) {
    ListScaffold(title = R.string.savings_title, onBack = null, addLabel = R.string.pocket_add, onAdd = onAddPocket) {
        item {
            SummaryCard(
                label = stringResource(R.string.savings_total),
                value = if (state.loading) "" else formatCents(state.summary.totalCents),
                valueColor = MaterialTheme.finance.savings,
            ) {
                DetailText(stringResource(R.string.savings_hint))
            }
        }
        if (!state.loading && state.summary.pockets.isEmpty()) {
            item { EmptyState(Icons.Outlined.Savings, R.string.savings_empty) }
        }
        cardGroup("pockets", state.summary.pockets) { balance ->
            PocketRow(balance, onClick = balance.pocket?.let { pocket -> { onOpenPocket(pocket.id) } })
        }
        cardGroup("months", state.summary.months, header = R.string.savings_by_month) { MonthLine(it) }
    }
}

/** [onClick] is null for "sem caixinha", which cannot be edited. */
@Composable
private fun PocketRow(balance: PocketBalance, onClick: (() -> Unit)?) {
    val target = balance.pocket?.targetCents
    val savings = MaterialTheme.finance.savings
    CardListItem(
        headline = balance.pocket?.name ?: stringResource(R.string.pocket_none),
        supporting = if (target != null) {
            {
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { balance.progress ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = savings,
                        trackColor = savings.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    DetailText(stringResource(R.string.pocket_target_of, formatCents(target)))
                }
            }
        } else {
            null
        },
        leading = { IconBadge(Icons.Outlined.Savings, savings) },
        trailing = { Text(formatCents(balance.savedCents), style = MaterialTheme.typography.titleSmall) },
        onClick = onClick,
    )
}

@Composable
private fun MonthLine(month: MonthSaved) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatMonth(month.month), style = MaterialTheme.typography.bodyLarge)
        Text(formatCents(month.savedCents), style = MaterialTheme.typography.titleSmall)
    }
}
