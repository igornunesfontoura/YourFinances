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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.budget.CategorySpending
import io.github.igornunesfontoura.yourfinances.domain.report.MonthEvolution
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
                title = { Text(formatMonth(state.month)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.month_previous))
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.month_next))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionTitle(stringResource(R.string.reports_by_category)) }
            item {
                Text(
                    stringResource(R.string.reports_basis_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (state.beyondPredictions) {
                item {
                    Text(
                        stringResource(R.string.reports_beyond_predictions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            if (!state.loading && state.spending.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.reports_no_expenses),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.spending, key = { it.category?.id ?: -1L }) { CategoryBudgetRow(it) }
            item { SectionTitle(stringResource(R.string.reports_evolution)) }
            item { EvolutionChart(state.evolution) }
            items(state.evolution.reversed(), key = { "month-${it.month}" }) { month ->
                ListItem(
                    headlineContent = { Text(formatMonth(month.month)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.reports_month_line,
                                formatCents(month.totals.incomeCents),
                                formatCents(month.totals.expensesCents),
                            ),
                        )
                    },
                    trailingContent = { Text(formatCents(month.totals.balanceCents), style = MaterialTheme.typography.bodyLarge) },
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetRow(spending: CategorySpending) {
    val limit = spending.limitCents
    val overColor = if (spending.overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(spending.category?.name ?: stringResource(R.string.category_none)) },
        supportingContent = if (limit != null) {
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
                    )
                }
            }
        } else {
            null
        },
        trailingContent = { Text(formatCents(spending.spentCents), style = MaterialTheme.typography.bodyLarge, color = overColor) },
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}
