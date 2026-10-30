package io.github.igornunesfontoura.yourfinances.ui.installment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.installment.InstallmentProgress
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.components.ConfirmDialog
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatMonth
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth

@Composable
fun InstallmentListRoute(onBack: () -> Unit, onAdd: () -> Unit, viewModel: InstallmentListViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InstallmentListScreen(state, onBack, onAdd, viewModel::delete, viewModel::dismissError)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentListScreen(
    state: InstallmentListUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
    onDismissError: () -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.save_failed)
    LaunchedEffect(state.actionFailed) {
        if (state.actionFailed) {
            snackbar.showSnackbar(errorMessage)
            onDismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.installments_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, stringResource(R.string.installment_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            if (!state.loading && state.active.isEmpty() && state.finished.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.installments_empty), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (state.committedByMonth.isNotEmpty()) {
                item { CommittedCard(state.committedByMonth) }
            }
            if (state.active.isNotEmpty()) {
                item { SectionTitle(R.string.installments_active) }
                items(state.active, key = { it.purchase.id }) { PurchaseRow(it) { pendingDelete = it.purchase.id } }
            }
            if (state.finished.isNotEmpty()) {
                item { SectionTitle(R.string.installments_finished) }
                items(state.finished, key = { it.purchase.id }) { PurchaseRow(it) { pendingDelete = it.purchase.id } }
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = R.string.purchase_delete_confirm_title,
            message = R.string.purchase_delete_confirm_message,
            confirmLabel = R.string.entry_delete,
            onConfirm = {
                pendingDelete = null
                onDelete(id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun CommittedCard(committed: List<Pair<YearMonth, Long>>) {
    AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.installments_committed), style = MaterialTheme.typography.titleSmall)
            committed.forEach { (month, cents) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMonth(month), style = MaterialTheme.typography.bodyMedium)
                    Text(formatCents(cents), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PurchaseRow(progress: InstallmentProgress, onDelete: () -> Unit) {
    val purchase = progress.purchase
    ListItem(
        headlineContent = { Text(purchase.description.ifBlank { stringResource(R.string.installment_untitled) }) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.installment_paid_of, progress.paidCount, purchase.installmentCount, formatCents(purchase.totalCents)))
                progress.endsOn?.let {
                    Text(stringResource(R.string.installment_ends_in, formatMonth(YearMonth.from(it))))
                }
                if (!progress.isFinished) {
                    Text(stringResource(R.string.installment_remaining, formatCents(progress.remainingCents)))
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, stringResource(R.string.entry_delete)) }
        },
    )
}
