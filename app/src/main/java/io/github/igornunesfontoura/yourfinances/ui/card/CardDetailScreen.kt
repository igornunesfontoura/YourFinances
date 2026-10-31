package io.github.igornunesfontoura.yourfinances.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.card.InvoiceStatus
import io.github.igornunesfontoura.yourfinances.domain.card.InvoiceSummary
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatShortDate
import io.github.igornunesfontoura.yourfinances.ui.format.label
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CardDetailRoute(
    cardId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: CardDetailViewModel = koinViewModel(key = "card-$cardId") { parametersOf(cardId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.gone) { if (state.gone) currentOnBack() }
    CardDetailScreen(state, onBack, onEdit, onOpenEntry, viewModel::markPaid, viewModel::unmarkPaid, viewModel::dismissError)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    state: CardDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onMarkPaid: (InvoiceSummary) -> Unit,
    onUnmarkPaid: (InvoiceSummary) -> Unit,
    onDismissError: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.save_failed)
    LaunchedEffect(state.actionFailed) {
        if (state.actionFailed) {
            snackbar.showSnackbar(errorMessage)
            onDismissError()
        }
    }
    // Due date (epoch day) of the invoice whose entries are shown.
    var expanded by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.card?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, enabled = state.card != null) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.card_edit_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.invoices, key = { it.dueDate.toEpochDay() }) { invoice ->
                val key = invoice.dueDate.toEpochDay()
                InvoiceCard(
                    invoice = invoice,
                    expanded = expanded == key,
                    onToggle = { expanded = if (expanded == key) null else key },
                    onOpenEntry = onOpenEntry,
                    onMarkPaid = { onMarkPaid(invoice) },
                    onUnmarkPaid = { onUnmarkPaid(invoice) },
                )
            }
        }
    }
}

@Composable
private fun InvoiceCard(
    invoice: InvoiceSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onMarkPaid: () -> Unit,
    onUnmarkPaid: () -> Unit,
) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.clickable(onClick = onToggle).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.invoice_due_on, formatShortDate(invoice.dueDate)), style = MaterialTheme.typography.titleSmall)
                Text(formatCents(invoice.totalCents), style = MaterialTheme.typography.titleLarge)
            }
            if (invoice.periodStart != null && invoice.closingDate != null) {
                Text(
                    text = stringResource(R.string.invoice_period, formatShortDate(invoice.periodStart), formatShortDate(invoice.closingDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if (invoice.status == InvoiceStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(
                    text = invoice.paidOn?.let { stringResource(R.string.invoice_paid_on, formatShortDate(it)) }
                        ?: stringResource(invoice.status.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                when (invoice.status) {
                    InvoiceStatus.CLOSED, InvoiceStatus.OVERDUE -> TextButton(onClick = onMarkPaid) {
                        Text(stringResource(R.string.invoice_mark_paid))
                    }
                    InvoiceStatus.PAID -> TextButton(onClick = onUnmarkPaid) { Text(stringResource(R.string.invoice_unmark_paid)) }
                    InvoiceStatus.OPEN -> Unit
                }
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                if (invoice.entries.isEmpty()) {
                    Text(stringResource(R.string.invoice_empty), style = MaterialTheme.typography.bodyMedium)
                }
                invoice.entries.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenEntry(entry.id) }.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.description.ifBlank { stringResource(R.string.invoice_entry_untitled) })
                            val details = buildList {
                                add(formatShortDate(entry.occurredOn))
                                if (entry.installmentNumber != null && entry.installmentCount != null) {
                                    add(stringResource(R.string.entry_installment, entry.installmentNumber, entry.installmentCount))
                                }
                            }
                            Text(
                                text = details.joinToString(stringResource(R.string.entry_details_separator)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(formatCents(entry.amountCents))
                    }
                }
            }
        }
    }
}
