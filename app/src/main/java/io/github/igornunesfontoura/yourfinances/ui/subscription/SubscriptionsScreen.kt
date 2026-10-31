package io.github.igornunesfontoura.yourfinances.ui.subscription

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.subscription.Subscription
import io.github.igornunesfontoura.yourfinances.ui.components.CardListItem
import io.github.igornunesfontoura.yourfinances.ui.components.DetailText
import io.github.igornunesfontoura.yourfinances.ui.components.EmptyState
import io.github.igornunesfontoura.yourfinances.ui.components.IconBadge
import io.github.igornunesfontoura.yourfinances.ui.components.ListScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.SummaryCard
import io.github.igornunesfontoura.yourfinances.ui.components.cardGroup
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatShortDate
import io.github.igornunesfontoura.yourfinances.ui.theme.finance
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionsRoute(
    onBack: () -> Unit,
    onOpenRecurrence: (Long) -> Unit,
    viewModel: SubscriptionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListScaffold(title = R.string.subscriptions_title, onBack = onBack) {
        item {
            SummaryCard(
                label = stringResource(R.string.subscriptions_per_month),
                value = if (state.loading) "" else formatCents(state.summary.monthlyAverageCents),
                valueColor = MaterialTheme.finance.expense,
            ) {
                Text(stringResource(R.string.subscriptions_per_year, formatCents(state.summary.yearlyCents)), style = MaterialTheme.typography.bodyMedium)
                DetailText(stringResource(R.string.subscriptions_hint))
            }
        }
        if (!state.loading && state.summary.subscriptions.isEmpty()) {
            item { EmptyState(Icons.Outlined.Subscriptions, R.string.subscriptions_empty) }
        }
        cardGroup("subscriptions", state.summary.subscriptions) { subscription ->
            SubscriptionRow(
                subscription,
                cardName = subscription.recurrence.creditCardId?.let { state.cardNames[it] },
                onClick = { onOpenRecurrence(subscription.recurrence.id) },
            )
        }
    }
}

@Composable
private fun SubscriptionRow(subscription: Subscription, cardName: String?, onClick: () -> Unit) {
    val recurrence = subscription.recurrence
    val next = subscription.nextChargeOn
    val details = buildList {
        add(
            stringResource(
                when (recurrence.frequency) {
                    Frequency.MONTHLY -> R.string.subscription_monthly
                    Frequency.TWICE_MONTHLY -> R.string.subscription_twice_monthly
                    Frequency.YEARLY -> R.string.subscription_yearly
                },
            ),
        )
        next?.let { add(stringResource(R.string.subscription_next, formatShortDate(it))) }
        if (cardName != null) add(cardName)
    }.joinToString(stringResource(R.string.entry_details_separator))
    CardListItem(
        headline = recurrence.description.ifBlank { stringResource(R.string.subscription_untitled) },
        supporting = { Text(details) },
        leading = { IconBadge(Icons.Outlined.Subscriptions, MaterialTheme.finance.expense) },
        trailing = { Text(formatCents(recurrence.cycleAmountCents), style = MaterialTheme.typography.titleSmall) },
        onClick = onClick,
    )
}
