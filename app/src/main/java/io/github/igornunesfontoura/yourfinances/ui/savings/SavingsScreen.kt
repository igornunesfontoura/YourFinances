package io.github.igornunesfontoura.yourfinances.ui.savings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.savings.PocketBalance
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatMonth
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(state: SavingsUiState, onAddPocket: () -> Unit, onOpenPocket: (Long) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.savings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPocket) {
                Icon(Icons.Filled.Add, stringResource(R.string.pocket_add))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.savings_total), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (state.loading) "" else formatCents(state.summary.totalCents),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        stringResource(R.string.savings_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!state.loading && state.summary.pockets.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.savings_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.summary.pockets, key = { it.pocket?.id ?: -1L }) { balance ->
                PocketRow(balance, onClick = balance.pocket?.let { pocket -> { onOpenPocket(pocket.id) } })
            }
            if (state.summary.months.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.savings_by_month)) }
                items(state.summary.months, key = { "month-${it.month}" }) { month ->
                    ListItem(
                        headlineContent = { Text(formatMonth(month.month)) },
                        trailingContent = { Text(formatCents(month.savedCents), style = MaterialTheme.typography.bodyLarge) },
                    )
                }
            }
        }
    }
}

/** [onClick] is null for "sem caixinha", which cannot be edited. */
@Composable
private fun PocketRow(balance: PocketBalance, onClick: (() -> Unit)?) {
    val target = balance.pocket?.targetCents
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        headlineContent = { Text(balance.pocket?.name ?: stringResource(R.string.pocket_none)) },
        supportingContent = if (target != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.pocket_target_of, formatCents(target)))
                    LinearProgressIndicator(progress = { balance.progress ?: 0f }, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            null
        },
        trailingContent = { Text(formatCents(balance.savedCents), style = MaterialTheme.typography.bodyLarge) },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

