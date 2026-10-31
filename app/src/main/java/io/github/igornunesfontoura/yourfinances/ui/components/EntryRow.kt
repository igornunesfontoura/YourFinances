package io.github.igornunesfontoura.yourfinances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.label
import io.github.igornunesfontoura.yourfinances.ui.theme.finance

val EntryType.icon: ImageVector
    get() = when (this) {
        EntryType.INCOME -> Icons.Outlined.Payments
        EntryType.FIXED_EXPENSE -> Icons.AutoMirrored.Outlined.ReceiptLong
        EntryType.FLEXIBLE_EXPENSE -> Icons.Outlined.ShoppingBag
        EntryType.INVESTMENT -> Icons.Outlined.Savings
        EntryType.WITHDRAWAL -> Icons.Outlined.AccountBalanceWallet
    }

/** Income green, expenses red, savings movements in the savings color. */
val EntryType.tint: Color
    @Composable
    get() = when (this) {
        EntryType.INCOME -> MaterialTheme.finance.income
        EntryType.FIXED_EXPENSE, EntryType.FLEXIBLE_EXPENSE -> MaterialTheme.finance.expense
        EntryType.INVESTMENT, EntryType.WITHDRAWAL -> MaterialTheme.finance.savings
    }

/** Round tinted badge with the type's icon. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(40.dp).background(tint.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** With [onConfirm], a predicted entry gets a one-tap confirm button. */
@Composable
fun EntryRow(entry: Entry, cardName: String?, onClick: () -> Unit, onConfirm: (() -> Unit)? = null) {
    val predicted = entry.status == EntryStatus.PREDICTED
    val title = entry.description.ifBlank { stringResource(entry.type.label) }
    val details = buildList {
        // Without a description the type is already the title.
        if (entry.description.isNotBlank()) add(stringResource(entry.type.label))
        if (entry.installmentNumber != null && entry.installmentCount != null) {
            add(stringResource(R.string.entry_installment, entry.installmentNumber, entry.installmentCount))
        }
        if (cardName != null) add(cardName)
        if (predicted) add(stringResource(R.string.entry_predicted))
    }.joinToString(stringResource(R.string.entry_details_separator))
    val amountColor = when (entry.type) {
        EntryType.INCOME, EntryType.INVESTMENT, EntryType.WITHDRAWAL -> entry.type.tint
        EntryType.FIXED_EXPENSE, EntryType.FLEXIBLE_EXPENSE -> MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { IconBadge(entry.type.icon, entry.type.tint) },
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(details, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (entry.type.isInflow) R.string.amount_income else R.string.amount_expense,
                        formatCents(entry.amountCents),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (predicted) amountColor.copy(alpha = 0.6f) else amountColor,
                )
                if (predicted && onConfirm != null) {
                    IconButton(onClick = onConfirm) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            stringResource(R.string.entry_confirm),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )
}
