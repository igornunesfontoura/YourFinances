package io.github.igornunesfontoura.yourfinances.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.model.Category
import io.github.igornunesfontoura.yourfinances.domain.model.CategoryKind
import io.github.igornunesfontoura.yourfinances.ui.components.AmountField
import io.github.igornunesfontoura.yourfinances.ui.components.ConfirmDialog
import io.github.igornunesfontoura.yourfinances.ui.components.ErrorText
import io.github.igornunesfontoura.yourfinances.ui.components.FormScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.HintText
import io.github.igornunesfontoura.yourfinances.ui.components.SegmentedSelector
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.label
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListRoute(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: CategoryListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, stringResource(R.string.category_add)) }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            listOf(R.string.category_kind_expense to state.expense, R.string.category_kind_income to state.income)
                .forEach { (title, categories) ->
                    item(key = "title-$title") {
                        Text(
                            stringResource(title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(categories, key = { it.id }) { CategoryRow(it, onClick = { onOpen(it.id) }) }
                }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    val limit = category.monthlyLimitCents
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(category.name) },
        supportingContent = limit?.let { { Text(stringResource(R.string.category_limit_of, formatCents(it))) } },
    )
}

@Composable
fun CategoryFormRoute(
    categoryId: Long?,
    onDone: () -> Unit,
    viewModel: CategoryFormViewModel = koinViewModel(key = "category-form-$categoryId") { parametersOf(categoryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.status.finished) { if (state.status.finished) currentOnDone() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.status.isBusy

    FormScaffold(
        title = if (state.isEditing) R.string.category_edit_title else R.string.category_new_title,
        onBack = onDone,
        actions = {
            if (state.isEditing) {
                IconButton(onClick = { confirmDelete = true }, enabled = enabled) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.entry_delete))
                }
            }
        },
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.category_name)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        // Changing the kind would leave its entries pointing at a category of the other kind.
        SegmentedSelector(CategoryKind.entries, state.kind, { it.label }, viewModel::onKindChange, enabled && !state.isEditing)
        if (state.isEditing) HintText(R.string.category_kind_locked)
        if (state.kind == CategoryKind.EXPENSE) {
            AmountField(state.limitDigits, viewModel::onLimitChange, R.string.category_limit, enabled, onDone = viewModel::save)
            HintText(R.string.category_limit_hint)
        }
        if (state.status.failed) ErrorText(R.string.save_failed)
        Button(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.entry_save))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = R.string.category_delete_confirm_title,
            message = R.string.category_delete_confirm_message,
            confirmLabel = R.string.entry_delete,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
