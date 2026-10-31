package io.github.igornunesfontoura.yourfinances.ui.installment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.installment.InstallmentProgress
import io.github.igornunesfontoura.yourfinances.ui.components.CardListItem
import io.github.igornunesfontoura.yourfinances.ui.components.ConfirmDialog
import io.github.igornunesfontoura.yourfinances.ui.components.DetailText
import io.github.igornunesfontoura.yourfinances.ui.components.EmptyState
import io.github.igornunesfontoura.yourfinances.ui.components.ListScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.cardGroup
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatMonth
import io.github.igornunesfontoura.yourfinances.ui.theme.finance
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth

@Composable
fun InstallmentListRoute(onBack: () -> Unit, onAdd: () -> Unit, viewModel: InstallmentListViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InstallmentListScreen(state, onBack, onAdd, viewModel::delete, viewModel::dismissError)
}

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

    ListScaffold(
        title = R.string.installments_title,
        onBack = onBack,
        addLabel = R.string.installment_add,
        onAdd = onAdd,
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        if (!state.loading && state.active.isEmpty() && state.finished.isEmpty()) {
            item { EmptyState(Icons.Outlined.CalendarMonth, R.string.installments_empty) }
        }
        cardGroup("committed", state.committedByMonth, header = R.string.installments_committed) { (month, cents) ->
            CommittedLine(month, cents)
        }
        cardGroup("active", state.active, header = R.string.installments_active) { PurchaseRow(it) { pendingDelete = it.purchase.id } }
        cardGroup("finished", state.finished, header = R.string.installments_finished) { PurchaseRow(it) { pendingDelete = it.purchase.id } }
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
private fun CommittedLine(month: YearMonth, cents: Long) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatMonth(month), style = MaterialTheme.typography.bodyLarge)
        Text(formatCents(cents), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.finance.expense)
    }
}

@Composable
private fun PurchaseRow(progress: InstallmentProgress, onDelete: () -> Unit) {
    val purchase = progress.purchase
    CardListItem(
        headline = purchase.description.ifBlank { stringResource(R.string.installment_untitled) },
        supporting = {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress.paidShare },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                DetailText(stringResource(R.string.installment_paid_of, progress.paidCount, purchase.installmentCount, formatCents(purchase.totalCents)))
                progress.endsOn?.let { DetailText(stringResource(R.string.installment_ends_in, formatMonth(YearMonth.from(it)))) }
                if (!progress.isFinished) {
                    DetailText(stringResource(R.string.installment_remaining, formatCents(progress.remainingCents)))
                }
            }
        },
        trailing = {
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, stringResource(R.string.entry_delete)) }
        },
    )
}
