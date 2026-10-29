package io.github.igornunesfontoura.yourfinances.ui.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.subscription.Subscription
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatShortDate
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsRoute(
    onBack: () -> Unit,
    onOpenRecurrence: (Long) -> Unit,
    viewModel: SubscriptionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscriptions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.subscriptions_per_month), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.loading) "" else formatCents(state.summary.monthlyAverageCents),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        stringResource(R.string.subscriptions_per_year, formatCents(state.summary.yearlyCents)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.subscriptions_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!state.loading && state.summary.subscriptions.isEmpty()) {
                item {
                    Text(stringResource(R.string.subscriptions_empty), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                }
            }
            items(state.summary.subscriptions, key = { it.recurrence.id }) { subscription ->
                SubscriptionRow(
                    subscription,
                    cardName = subscription.recurrence.creditCardId?.let { state.cardNames[it] },
                    onClick = { onOpenRecurrence(subscription.recurrence.id) },
                )
            }
        }
    }
}

@Composable
private fun SubscriptionRow(subscription: Subscription, cardName: String?, onClick: () -> Unit) {
    val recurrence = subscription.recurrence
    val next = subscription.nextChargeOn
    val details = buildList {
        add(stringResource(if (recurrence.frequency == Frequency.MONTHLY) R.string.subscription_monthly else R.string.subscription_yearly))
        next?.let { add(stringResource(R.string.subscription_next, formatShortDate(it))) }
        if (cardName != null) add(cardName)
    }.joinToString(stringResource(R.string.entry_details_separator))
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(recurrence.description.ifBlank { stringResource(R.string.subscription_untitled) }) },
        supportingContent = { Text(details) },
        trailingContent = { Text(formatCents(recurrence.amountCents), style = MaterialTheme.typography.bodyLarge) },
    )
}
