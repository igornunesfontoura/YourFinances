package io.github.igornunes.yourfinances.ui.importing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.domain.importing.CsvMapping
import io.github.igornunes.yourfinances.domain.importing.CsvTable
import io.github.igornunes.yourfinances.domain.importing.DuplicateKind
import io.github.igornunes.yourfinances.domain.importing.ImportCandidate
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.ui.components.ChoiceChips
import io.github.igornunes.yourfinances.ui.components.ErrorText
import io.github.igornunes.yourfinances.ui.components.HintText
import io.github.igornunes.yourfinances.ui.components.SegmentedSelector
import io.github.igornunes.yourfinances.ui.format.formatCents
import io.github.igornunes.yourfinances.ui.format.formatShortDate
import io.github.igornunes.yourfinances.ui.format.label
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportRoute(onDone: () -> Unit, viewModel: ImportViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFileChosen(uri.toString())
    }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            if (state.stage == ImportStage.REVIEW) {
                Button(
                    onClick = viewModel::import,
                    enabled = state.selectedCount > 0,
                    // Edge to edge: keep the button above the system navigation bar.
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                ) { Text(stringResource(R.string.import_confirm, state.selectedCount)) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.stage) {
                ImportStage.CHOOSE -> Choose(state, onPick = { pickFile.launch(arrayOf("*/*")) })
                ImportStage.WORKING -> LinearProgressIndicator(Modifier.fillMaxWidth())
                ImportStage.MAPPING -> Mapping(state, viewModel::onMappingChange, viewModel::confirmMapping)
                ImportStage.REVIEW -> Review(state, viewModel::onDestinationChange, viewModel::onToggle, onEdit = { editing = it })
                ImportStage.DONE -> Done(state.importedCount, onDone)
            }
        }
    }

    val candidate = editing?.let { key -> state.candidates.find { it.key == key } }
    if (candidate != null) {
        CandidateDialog(
            state = state,
            candidate = candidate,
            onTypeChange = { viewModel.onTypeChange(candidate.key, it) },
            onCategoryChange = { viewModel.onCategoryChange(candidate.key, it) },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun Choose(state: ImportUiState, onPick: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.import_intro))
        HintText(R.string.import_privacy)
        state.problem?.let { ErrorText(it.text) }
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_pick)) }
    }
}

@Composable
private fun Mapping(state: ImportUiState, onChange: (CsvMapping) -> Unit, onConfirm: () -> Unit) {
    val table = state.table ?: return
    val mapping = state.mapping ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.import_mapping_intro))
        state.problem?.let { ErrorText(it.text) }
        ColumnPicker(R.string.import_column_date, table, mapping.dateColumn) { onChange(mapping.copy(dateColumn = it)) }
        ColumnPicker(R.string.import_column_description, table, mapping.descriptionColumn) { onChange(mapping.copy(descriptionColumn = it)) }
        ColumnPicker(R.string.import_column_amount, table, mapping.amountColumn) { onChange(mapping.copy(amountColumn = it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.import_positive_is_expense))
                HintText(R.string.import_positive_is_expense_hint)
            }
            Switch(checked = mapping.positiveIsExpense, onCheckedChange = { onChange(mapping.copy(positiveIsExpense = it)) })
        }
        Text(stringResource(R.string.import_preview), style = MaterialTheme.typography.labelLarge)
        table.rows.take(3).forEach { row ->
            HintText("${row[mapping.dateColumn]} · ${row[mapping.descriptionColumn]} · ${row[mapping.amountColumn]}")
        }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_continue)) }
    }
}

@Composable
private fun ColumnPicker(@StringRes label: Int, table: CsvTable, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    fun name(index: Int) = table.header[index].ifBlank { "#${index + 1}" }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_column_choice, stringResource(label), name(selected)))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            table.header.indices.forEach { index ->
                DropdownMenuItem(
                    text = { Text(name(index)) },
                    onClick = {
                        open = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun Review(
    state: ImportUiState,
    onDestinationChange: (Long?) -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.import_destination), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(R.string.import_destination_account, state.cards.map { it.id to it.name }, state.cardId, onDestinationChange, enabled = true)
                if (state.cardId != null) HintText(R.string.import_card_hint)
                HintText(stringResource(R.string.import_review_hint, state.candidates.size))
                if (state.skippedRows > 0) HintText(stringResource(R.string.import_skipped_rows, state.skippedRows))
                state.problem?.let { ErrorText(it.text) }
            }
        }
        items(state.candidates, key = { it.key }) { candidate ->
            CandidateRow(candidate, state, onToggle = { onToggle(candidate.key) }, onEdit = { onEdit(candidate.key) })
        }
    }
}

@Composable
private fun CandidateRow(candidate: ImportCandidate, state: ImportUiState, onToggle: () -> Unit, onEdit: () -> Unit) {
    val line = candidate.line
    val category = candidate.categoryId?.let { id -> state.categories.find { it.id == id }?.name }
    val details = buildList {
        add(formatShortDate(line.date))
        add(stringResource(candidate.type.label))
        if (category != null) add(category)
        when (candidate.duplicate) {
            DuplicateKind.ALREADY_IMPORTED -> add(stringResource(R.string.import_already_imported))
            DuplicateKind.POSSIBLE -> add(stringResource(R.string.import_possible_duplicate))
            null -> {}
        }
    }.joinToString(stringResource(R.string.entry_details_separator))
    ListItem(
        modifier = Modifier.clickable(onClick = onEdit),
        leadingContent = { Checkbox(checked = candidate.selected, onCheckedChange = { onToggle() }) },
        headlineContent = { Text(line.description.ifBlank { stringResource(R.string.import_untitled) }) },
        supportingContent = { Text(details) },
        trailingContent = {
            Text(
                stringResource(
                    if (line.amountCents > 0) R.string.amount_income else R.string.amount_expense,
                    formatCents(kotlin.math.abs(line.amountCents)),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
    )
}

@Composable
private fun CandidateDialog(
    state: ImportUiState,
    candidate: ImportCandidate,
    onTypeChange: (EntryType) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val categories = state.categories.filter { it.kind == candidate.type.categoryKind }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(candidate.line.description.ifBlank { stringResource(R.string.import_untitled) }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedSelector(EntryType.entries, candidate.type, { it.label }, onTypeChange, enabled = true)
                if (categories.isNotEmpty()) {
                    ChoiceChips(R.string.category_none, categories.map { it.id to it.name }, candidate.categoryId, onCategoryChange, enabled = true)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

@Composable
private fun Done(count: Int, onDone: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.import_done, count), style = MaterialTheme.typography.titleMedium)
        HintText(R.string.import_done_hint)
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_ok)) }
    }
}

private val ImportProblem.text: Int
    get() = when (this) {
        ImportProblem.UNREADABLE -> R.string.import_unreadable
        ImportProblem.NO_TRANSACTIONS -> R.string.import_no_transactions
        ImportProblem.UNMAPPED -> R.string.import_unmapped
        ImportProblem.FAILED -> R.string.import_failed
    }
