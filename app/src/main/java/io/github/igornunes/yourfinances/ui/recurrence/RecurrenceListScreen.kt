package io.github.igornunes.yourfinances.ui.recurrence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.domain.model.Recurrence
import io.github.igornunes.yourfinances.ui.format.formatCents
import io.github.igornunes.yourfinances.ui.format.label
import io.github.igornunes.yourfinances.ui.format.scheduleText
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecurrenceListRoute(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: RecurrenceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecurrenceListScreen(state, onBack, onAdd, onOpen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceListScreen(state: RecurrenceListUiState, onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurrences_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, stringResource(R.string.recurrence_add))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            if (!state.loading && state.recurrences.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.recurrences_empty), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            items(state.recurrences, key = { it.id }) { recurrence ->
                RecurrenceRow(recurrence, onClick = { onOpen(recurrence.id) })
            }
        }
    }
}

@Composable
private fun RecurrenceRow(recurrence: Recurrence, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(recurrence.description.ifBlank { stringResource(recurrence.type.label) }) },
        supportingContent = {
            Text(stringResource(R.string.recurrence_row_details, stringResource(recurrence.type.label), scheduleText(recurrence)))
        },
        trailingContent = { Text(formatCents(recurrence.amountCents), style = MaterialTheme.typography.bodyLarge) },
    )
}
