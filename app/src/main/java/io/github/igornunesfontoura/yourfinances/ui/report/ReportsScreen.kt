package io.github.igornunesfontoura.yourfinances.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.budget.CategorySpending
import io.github.igornunesfontoura.yourfinances.domain.report.MonthEvolution
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.components.CardListItem
import io.github.igornunesfontoura.yourfinances.ui.components.DetailText
import io.github.igornunesfontoura.yourfinances.ui.components.EmptyState
import io.github.igornunesfontoura.yourfinances.ui.components.MonthSwitcher
import io.github.igornunesfontoura.yourfinances.ui.components.SectionHeader
import io.github.igornunesfontoura.yourfinances.ui.components.cardGroup
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatMonth
import io.github.igornunesfontoura.yourfinances.ui.format.formatShortMonth
import io.github.igornunesfontoura.yourfinances.ui.theme.finance
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsRoute(onBack: () -> Unit, viewModel: ReportsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { MonthSwitcher(state.month, viewModel::previousMonth, viewModel::nextMonth) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionHeader(stringResource(R.string.reports_by_category))
                    DetailText(stringResource(R.string.reports_basis_hint))
                    if (state.beyondPredictions) {
                        DetailText(stringResource(R.string.reports_beyond_predictions), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            if (!state.loading && state.spending.isEmpty()) {
                item { EmptyState(Icons.Outlined.BarChart, R.string.reports_no_expenses) }
            }
            cardGroup("spending", state.spending) { CategoryBudgetRow(it) }
            item {
                Column {
                    SectionHeader(stringResource(R.string.reports_evolution))
                    AppCard(Modifier.fillMaxWidth()) { EvolutionChart(state.evolution) }
                }
            }
            cardGroup("months", state.evolution.reversed()) { month ->
                CardListItem(
                    headline = formatMonth(month.month),
                    supporting = {
                        Text(
                            stringResource(
                                R.string.reports_month_line,
                                formatCents(month.totals.incomeCents),
                                formatCents(month.totals.expensesCents),
                            ),
                        )
                    },
                    trailing = {
                        Text(
                            formatCents(month.totals.balanceCents),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (month.totals.balanceCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetRow(spending: CategorySpending) {
    val limit = spending.limitCents
    val overColor = if (spending.overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    CardListItem(
        headline = spending.category?.name ?: stringResource(R.string.category_none),
        supporting = if (limit != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(
                            if (spending.overLimit) R.string.reports_over_limit else R.string.reports_of_limit,
                            formatCents(limit),
                        ),
                        color = overColor,
                    )
                    LinearProgressIndicator(
                        progress = { (spending.usage ?: 0.0).coerceIn(0.0, 1.0).toFloat() },
                        color = if (spending.overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        } else {
            null
        },
        trailing = { Text(formatCents(spending.spentCents), style = MaterialTheme.typography.titleSmall, color = overColor) },
    )
}

/**
 * Income and expense bars per month on one scale. Drawn with Canvas (no chart library); the
 * list below the chart carries the same numbers for screen readers.
 */
@Composable
private fun EvolutionChart(evolution: List<MonthEvolution>) {
    val incomeColor = MaterialTheme.finance.income
    val expenseColor = MaterialTheme.finance.expense
    val max = evolution.maxOfOrNull { maxOf(it.totals.incomeCents, it.totals.expensesCents) }?.takeIf { it > 0 } ?: 1L
    val description = stringResource(R.string.reports_chart_description)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = description }) {
            if (evolution.isEmpty()) return@Canvas
            val slot = size.width / evolution.size
            val bar = slot * 0.35f
            val radius = CornerRadius(bar / 4, bar / 4)
            evolution.forEachIndexed { index, month ->
                val left = index * slot + slot * 0.1f
                fun drawBar(cents: Long, x: Float, color: Color) {
                    val height = size.height * cents / max
                    drawRoundRect(color, Offset(x, size.height - height), Size(bar, height), radius)
                }
                drawBar(month.totals.incomeCents, left, incomeColor)
                drawBar(month.totals.expensesCents, left + bar, expenseColor)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            evolution.forEach { month ->
                Text(
                    formatShortMonth(month.month),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(incomeColor, stringResource(R.string.month_income))
            Legend(expenseColor, stringResource(R.string.month_expenses))
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
