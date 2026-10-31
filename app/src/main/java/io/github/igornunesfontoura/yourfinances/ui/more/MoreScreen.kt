package io.github.igornunesfontoura.yourfinances.ui.more

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.components.SectionHeader

private data class MoreItem(
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/** Every screen that is not a tab, grouped by what it is for. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenRecurrences: () -> Unit,
    onOpenInstallments: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val sections = listOf(
        R.string.more_section_planning to listOf(
            MoreItem(R.string.recurrences_title, R.string.more_recurrences_hint, Icons.Outlined.Autorenew, onOpenRecurrences),
            MoreItem(R.string.installments_title, R.string.more_installments_hint, Icons.Outlined.CalendarMonth, onOpenInstallments),
            MoreItem(R.string.cards_title, R.string.more_cards_hint, Icons.Outlined.CreditCard, onOpenCards),
            MoreItem(R.string.subscriptions_title, R.string.more_subscriptions_hint, Icons.Outlined.Subscriptions, onOpenSubscriptions),
        ),
        R.string.more_section_analysis to listOf(
            MoreItem(R.string.reports_title, R.string.more_reports_hint, Icons.Outlined.BarChart, onOpenReports),
            MoreItem(R.string.categories_title, R.string.more_categories_hint, Icons.Outlined.Category, onOpenCategories),
        ),
        R.string.more_section_data to listOf(
            MoreItem(R.string.import_title, R.string.more_import_hint, Icons.Outlined.FileUpload, onOpenImport),
            MoreItem(R.string.settings_title, R.string.more_settings_hint, Icons.Outlined.Settings, onOpenSettings),
        ),
        R.string.more_section_help to listOf(
            MoreItem(R.string.help_title, R.string.more_help_hint, Icons.AutoMirrored.Outlined.HelpOutline, onOpenHelp),
        ),
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_more)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            sections.forEach { (header, items) ->
                SectionHeader(stringResource(header))
                AppCard(Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        MoreRow(item)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MoreRow(item: MoreItem) {
    ListItem(
        modifier = Modifier.clickable(onClick = item.onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
        },
        headlineContent = { Text(stringResource(item.title)) },
        supportingContent = { Text(stringResource(item.hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}
