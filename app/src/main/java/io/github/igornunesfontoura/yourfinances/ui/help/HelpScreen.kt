package io.github.igornunesfontoura.yourfinances.ui.help

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.components.CardListItem
import io.github.igornunesfontoura.yourfinances.ui.components.IconBadge
import io.github.igornunesfontoura.yourfinances.ui.components.ListScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.cardGroup

private class HelpTopic(@StringRes val title: Int, @StringRes val body: Int, val icon: ImageVector)

private val topics = listOf(
    HelpTopic(R.string.help_available_title, R.string.help_available_body, Icons.Outlined.AccountBalanceWallet),
    HelpTopic(R.string.help_entry_title, R.string.help_entry_body, Icons.Outlined.AddCircle),
    HelpTopic(R.string.help_basis_title, R.string.help_basis_body, Icons.AutoMirrored.Outlined.ReceiptLong),
    HelpTopic(R.string.help_recurrence_title, R.string.help_recurrence_body, Icons.Outlined.Autorenew),
    HelpTopic(R.string.help_installment_title, R.string.help_installment_body, Icons.Outlined.CalendarMonth),
    HelpTopic(R.string.help_card_title, R.string.help_card_body, Icons.Outlined.CreditCard),
    HelpTopic(R.string.help_savings_title, R.string.help_savings_body, Icons.Outlined.Savings),
    HelpTopic(R.string.help_reports_title, R.string.help_reports_body, Icons.Outlined.BarChart),
    HelpTopic(R.string.help_import_title, R.string.help_import_body, Icons.Outlined.FileUpload),
    HelpTopic(R.string.help_backup_title, R.string.help_backup_body, Icons.Outlined.Shield),
)

/** Short guides, one per part of the app; each opens in place when tapped. */
@Composable
fun HelpScreen(onBack: () -> Unit, onReplayWelcome: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    ListScaffold(title = R.string.help_title, onBack = onBack) {
        cardGroup("replay", listOf(Unit)) {
            CardListItem(
                headline = stringResource(R.string.help_replay),
                supporting = { Text(stringResource(R.string.help_replay_hint)) },
                leading = { IconBadge(Icons.Outlined.PlayCircle, MaterialTheme.colorScheme.primary) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                onClick = onReplayWelcome,
            )
        }
        cardGroup("topics", topics, header = R.string.help_topics) { topic ->
            TopicRow(topic, expanded = expanded == topic.title) {
                expanded = if (expanded == topic.title) null else topic.title
            }
        }
    }
}

@Composable
private fun TopicRow(topic: HelpTopic, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val action = stringResource(if (expanded) R.string.help_collapse else R.string.help_expand)
    Column(Modifier.clickable(onClickLabel = action, role = Role.Button, onClick = onToggle)) {
        CardListItem(
            headline = stringResource(topic.title),
            leading = { IconBadge(topic.icon, MaterialTheme.colorScheme.primary) },
            trailing = { Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.rotate(rotation)) },
        )
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    stringResource(topic.body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                )
            }
        }
    }
}
