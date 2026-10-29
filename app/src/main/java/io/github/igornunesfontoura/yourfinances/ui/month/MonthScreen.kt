package io.github.igornunesfontoura.yourfinances.ui.month

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.budget.MonthTotals
import io.github.igornunesfontoura.yourfinances.domain.model.DateBasis
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatDay
import io.github.igornunesfontoura.yourfinances.ui.format.formatMonth
import io.github.igornunesfontoura.yourfinances.ui.format.hint
import io.github.igornunesfontoura.yourfinances.ui.format.label
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.YearMonth

@Composable
fun MonthRoute(
    month: YearMonth,
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: MonthViewModel = koinViewModel(key = "month-$month") { parametersOf(month) },
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
        onBack = onBack,
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
    onBack: () -> Unit,
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
                title = { Text(formatMonth(state.month)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.month_previous))
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.month_next))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, stringResource(R.string.entry_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item { BasisSelector(state.basis, onBasisChange) }
            item { TotalsCard(state.totals) }
            if (!state.loading && state.days.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.month_empty), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            state.days.forEach { day ->
                item(key = "day-${day.date}") {
                    Text(
                        text = formatDay(day.date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
                    )
                }
                items(day.entries, key = { it.id }) { entry ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasisSelector(basis: DateBasis, onBasisChange: (DateBasis) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TotalsCard(totals: MonthTotals) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TotalLine(R.string.month_income, totals.incomeCents)
            TotalLine(R.string.month_expenses, totals.expensesCents)
            TotalLine(R.string.month_investments, totals.investmentCents)
            if (totals.withdrawalsCents > 0) TotalLine(R.string.month_withdrawals, totals.withdrawalsCents)
            TotalLine(R.string.month_balance, totals.balanceCents, emphasized = true)
        }
    }
}

@Composable
private fun TotalLine(label: Int, cents: Long, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = style)
        Text(formatCents(cents), style = style)
    }
}

@Composable
private fun EntryRow(entry: Entry, cardName: String?, onClick: () -> Unit, onConfirm: () -> Unit) {
    val title = entry.description.ifBlank { stringResource(entry.type.label) }
    val details = buildList {
        add(stringResource(entry.type.label))
        if (entry.installmentNumber != null && entry.installmentCount != null) {
            add(stringResource(R.string.entry_installment, entry.installmentNumber, entry.installmentCount))
        }
        if (cardName != null) add(cardName)
        if (entry.status == EntryStatus.PREDICTED) add(stringResource(R.string.entry_predicted))
    }.joinToString(stringResource(R.string.entry_details_separator))
    val isIncome = entry.type.isInflow
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = if (entry.status == EntryStatus.PREDICTED) {
            {
                IconButton(onClick = onConfirm) {
                    Icon(Icons.Outlined.CheckCircle, stringResource(R.string.entry_confirm))
                }
            }
        } else {
            null
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(details) },
        trailingContent = {
            Text(
                text = stringResource(
                    if (isIncome) R.string.amount_income else R.string.amount_expense,
                    formatCents(entry.amountCents),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}
