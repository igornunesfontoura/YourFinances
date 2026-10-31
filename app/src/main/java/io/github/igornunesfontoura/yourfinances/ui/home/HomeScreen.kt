package io.github.igornunesfontoura.yourfinances.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.components.EntryRow
import io.github.igornunesfontoura.yourfinances.ui.components.IconBadge
import io.github.igornunesfontoura.yourfinances.ui.components.MonthSwitcher
import io.github.igornunesfontoura.yourfinances.ui.components.SectionHeader
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.theme.finance
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth

@Composable
fun HomeRoute(
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onSeeAllEntries: () -> Unit,
    onEditGoal: (YearMonth) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onAddEntry = onAddEntry,
        onOpenEntry = onOpenEntry,
        onSeeAllEntries = onSeeAllEntries,
        onEditGoal = { onEditGoal(viewModel.selectedMonth) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onSeeAllEntries: () -> Unit,
    onEditGoal: () -> Unit,
) {
    Scaffold(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvailableCard(state)
            // Hidden while loading so an unloaded goal never reads as "Definir".
            if (!state.loading) {
                SummaryTiles(state)
                Breakdown(state, onEditGoal)
                RecentEntries(state, onOpenEntry, onSeeAllEntries)
            }
            Text(
                stringResource(R.string.home_basis_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(88.dp))
        }
    }
}

@Composable
private fun AvailableCard(state: HomeUiState) {
    val available = state.available.availableCents
    val overspent = !state.loading && available < 0
    val finance = MaterialTheme.finance
    val container = if (overspent) finance.heroOverspent else finance.hero
    val content = if (overspent) finance.onHeroOverspent else finance.onHero
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_available), style = MaterialTheme.typography.titleSmall, color = content.copy(alpha = 0.85f))
            Text(
                text = if (state.loading) "" else formatCents(available),
                style = MaterialTheme.typography.displaySmall,
            )
            if (!state.loading) {
                Spacer(Modifier.height(12.dp))
                val share = state.available.committedShare
                if (share != null) {
                    LinearProgressIndicator(
                        progress = { share.coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = content,
                        trackColor = content.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Text(
                        stringResource(R.string.home_committed, state.available.committedPercent ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text(stringResource(R.string.home_no_income), style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.85f))
                }
                if (overspent) {
                    Text(stringResource(R.string.home_overspent), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (state.beyondPredictions) {
                Text(
                    stringResource(R.string.home_beyond_predictions),
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryTiles(state: HomeUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryTile(
            label = R.string.home_income,
            value = formatCents(state.available.incomeCents),
            icon = Icons.Outlined.Payments,
            tint = MaterialTheme.finance.income,
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            label = R.string.home_spending,
            value = formatCents(state.available.spendingCents),
            icon = Icons.Outlined.ShoppingBag,
            tint = MaterialTheme.finance.expense,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(@StringRes label: Int, value: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBadge(icon, tint)
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun Breakdown(state: HomeUiState, onEditGoal: () -> Unit) {
    val available = state.available
    val finance = MaterialTheme.finance
    Column {
        SectionHeader(stringResource(R.string.home_where_it_goes))
        AppCard(Modifier.fillMaxWidth()) {
            BreakdownLine(
                label = R.string.home_fixed,
                value = stringResource(R.string.amount_expense, formatCents(available.fixedExpensesCents)),
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                tint = finance.expense,
            )
            BreakdownLine(
                label = R.string.home_goal_line,
                // Shows what is reserved: the goal, or more when more was contributed.
                value = if (state.hasGoal || available.contributionsCents > 0) {
                    stringResource(R.string.amount_expense, formatCents(available.reservedForSavingsCents))
                } else {
                    stringResource(R.string.home_goal_unset)
                },
                icon = Icons.Outlined.Savings,
                tint = finance.savings,
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
                label = R.string.home_flexible,
                value = stringResource(R.string.amount_expense, formatCents(available.flexibleExpensesCents)),
                icon = Icons.Outlined.ShoppingBag,
                tint = finance.expense,
            )
            if (available.withdrawalsCents > 0) {
                BreakdownLine(
                    label = R.string.home_withdrawals,
                    value = stringResource(R.string.amount_income, formatCents(available.withdrawalsCents)),
                    icon = Icons.Outlined.AccountBalanceWallet,
                    tint = finance.savings,
                )
            }
        }
    }
}

/** With [onClick], the line shows a chevron and the whole line is tappable. */
@Composable
private fun BreakdownLine(
    @StringRes label: Int,
    value: String,
    icon: ImageVector,
    tint: Color,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { IconBadge(icon, tint) },
        headlineContent = { Text(stringResource(label)) },
        supportingContent = supporting?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        stringResource(R.string.goal_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
private fun RecentEntries(state: HomeUiState, onOpenEntry: (Long) -> Unit, onSeeAll: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(stringResource(R.string.home_recent), Modifier.weight(1f))
            TextButton(onClick = onSeeAll) { Text(stringResource(R.string.home_see_all)) }
        }
        AppCard(Modifier.fillMaxWidth()) {
            if (state.recentEntries.isEmpty()) {
                Text(
                    stringResource(R.string.home_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
            state.recentEntries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                EntryRow(entry = entry, cardName = null, onClick = { onOpenEntry(entry.id) })
            }
        }
    }
}
