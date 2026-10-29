package io.github.igornunes.yourfinances.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.ui.format.formatCents
import io.github.igornunes.yourfinances.ui.format.formatMonth
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth

@Composable
fun HomeRoute(
    onAddEntry: () -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
    onEditGoal: (YearMonth) -> Unit,
    onOpenRecurrences: () -> Unit,
    onOpenInstallments: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onAddEntry = onAddEntry,
        onOpenMonth = { onOpenMonth(viewModel.selectedMonth) },
        onEditGoal = { onEditGoal(viewModel.selectedMonth) },
        onOpenRecurrences = onOpenRecurrences,
        onOpenInstallments = onOpenInstallments,
        onOpenCards = onOpenCards,
        onOpenSavings = onOpenSavings,
        onOpenReports = onOpenReports,
        onOpenSubscriptions = onOpenSubscriptions,
        onOpenCategories = onOpenCategories,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddEntry: () -> Unit,
    onOpenMonth: () -> Unit,
    onEditGoal: () -> Unit,
    onOpenRecurrences: () -> Unit,
    onOpenInstallments: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(formatMonth(state.month)) },
                navigationIcon = {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.month_previous))
                    }
                },
                actions = {
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.month_next))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, stringResource(R.string.menu_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            listOf(
                                R.string.reports_title to onOpenReports,
                                R.string.savings_title to onOpenSavings,
                                R.string.subscriptions_title to onOpenSubscriptions,
                                R.string.recurrences_title to onOpenRecurrences,
                                R.string.installments_title to onOpenInstallments,
                                R.string.cards_title to onOpenCards,
                                R.string.categories_title to onOpenCategories,
                                R.string.settings_title to onOpenSettings,
                            ).forEach { (label, open) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(label)) },
                                    onClick = {
                                        menuOpen = false
                                        open()
                                    },
                                )
                            }
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AvailableHeader(state)
            // Hidden while loading so an unloaded goal never reads as "Definir".
            if (!state.loading) Breakdown(state, onEditGoal)
            OutlinedButton(onClick = onOpenMonth, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_open_month))
            }
        }
    }
}

@Composable
private fun AvailableHeader(state: HomeUiState) {
    val available = state.available.availableCents
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_available), style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (state.loading) "" else formatCents(available),
            style = MaterialTheme.typography.displayMedium,
            color = if (available < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (!state.loading && available < 0) {
            Text(
                stringResource(R.string.home_overspent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.beyondPredictions) {
            Text(
                stringResource(R.string.home_beyond_predictions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            stringResource(R.string.home_basis_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun Breakdown(state: HomeUiState, onEditGoal: () -> Unit) {
    val available = state.available
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            BreakdownLine(R.string.home_income, stringResource(R.string.amount_income, formatCents(available.incomeCents)))
            if (available.withdrawalsCents > 0) {
                BreakdownLine(
                    R.string.home_withdrawals,
                    stringResource(R.string.amount_income, formatCents(available.withdrawalsCents)),
                )
            }
            BreakdownLine(R.string.home_fixed, stringResource(R.string.amount_expense, formatCents(available.fixedExpensesCents)))
            BreakdownLine(
                label = R.string.home_goal,
                // Shows what is reserved: the goal, or more when more was contributed.
                value = if (state.hasGoal || available.contributionsCents > 0) {
                    stringResource(R.string.amount_expense, formatCents(available.reservedForSavingsCents))
                } else {
                    stringResource(R.string.home_goal_unset)
                },
                supporting = when {
                    state.hasGoal -> stringResource(
                        R.string.home_goal_and_saved,
                        formatCents(available.investmentGoalCents),
                        formatCents(state.savedCents),
                    )
                    state.savedCents != 0L -> stringResource(R.string.home_saved_this_month, formatCents(state.savedCents))
                    else -> null
                },
                onClick = onEditGoal,
            )
            BreakdownLine(
                R.string.home_flexible,
                stringResource(R.string.amount_expense, formatCents(available.flexibleExpensesCents)),
            )
        }
    }
}

/** With [onClick], the line shows an edit icon and the whole line is tappable. */
@Composable
private fun BreakdownLine(
    @StringRes label: Int,
    value: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(value, style = MaterialTheme.typography.bodyLarge)
        if (onClick != null) {
            Icon(Icons.Filled.Edit, stringResource(R.string.goal_edit), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
