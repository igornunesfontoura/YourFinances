package io.github.igornunesfontoura.yourfinances.ui.month

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.budget.MonthTotals
import io.github.igornunesfontoura.yourfinances.domain.model.DateBasis
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.components.EntryRow
import io.github.igornunesfontoura.yourfinances.ui.components.MonthSwitcher
import io.github.igornunesfontoura.yourfinances.ui.components.SectionHeader
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatDay
import io.github.igornunesfontoura.yourfinances.ui.format.hint
import io.github.igornunesfontoura.yourfinances.ui.format.label
import io.github.igornunesfontoura.yourfinances.ui.theme.finance
import org.koin.androidx.compose.koinViewModel

@Composable
fun MonthRoute(
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: MonthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MonthScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onBasisChange = viewModel::setBasis,
        onAddEntry = onAddEntry,
        onOpenEntry = onOpenEntry,
        onConfirmEntry = viewModel::confirm,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    state: MonthUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBasisChange: (DateBasis) -> Unit,
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onConfirmEntry: (Entry) -> Unit,
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { MonthSwitcher(state.month, onPreviousMonth, onNextMonth) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.entry_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { BasisSelector(state.basis, onBasisChange) }
            item { TotalsCard(state.totals) }
            if (!state.loading && state.days.isEmpty()) {
                item { EmptyMonth() }
            }
            items(state.days, key = { "day-${it.date}" }) { day ->
                Column {
                    SectionHeader(formatDay(day.date))
                    AppCard(Modifier.fillMaxWidth()) {
                        day.entries.forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            EntryRow(
                                entry = entry,
                                cardName = entry.creditCardId?.let { state.cardNames[it] },
                                onClick = { onOpenEntry(entry.id) },
                                onConfirm = { onConfirmEntry(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasisSelector(basis: DateBasis, onBasisChange: (DateBasis) -> Unit) {
    Column {
        val options = DateBasis.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == basis,
                    onClick = { onBasisChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(stringResource(option.label)) }
            }
        }
        Text(
            text = stringResource(basis.hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
    }
}

@Composable
private fun TotalsCard(totals: MonthTotals) {
    val finance = MaterialTheme.finance
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TotalStat(R.string.month_income, totals.incomeCents, finance.income, Modifier.weight(1f))
                TotalStat(R.string.month_expenses, totals.expensesCents, finance.expense, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TotalStat(R.string.month_investments, totals.investmentCents, finance.savings, Modifier.weight(1f))
                if (totals.withdrawalsCents > 0) {
                    TotalStat(R.string.month_withdrawals, totals.withdrawalsCents, finance.savings, Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.month_balance), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    formatCents(totals.balanceCents),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (totals.balanceCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Label with a colored dot over the amount. */
@Composable
private fun TotalStat(@StringRes label: Int, cents: Long, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatCents(cents), style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun EmptyMonth() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Text(
            stringResource(R.string.month_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
