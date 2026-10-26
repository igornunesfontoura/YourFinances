package io.github.igornunes.yourfinances.ui.card

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.ui.components.ConfirmDialog
import io.github.igornunes.yourfinances.ui.components.ErrorText
import io.github.igornunes.yourfinances.ui.components.FormScaffold
import io.github.igornunes.yourfinances.ui.components.HintText
import io.github.igornunes.yourfinances.ui.components.NumberField
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CardFormRoute(
    cardId: Long?,
    onDone: () -> Unit,
    viewModel: CardFormViewModel = koinViewModel(key = "card-form-$cardId") { parametersOf(cardId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.status.finished) { if (state.status.finished) currentOnDone() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.status.isBusy

    FormScaffold(
        title = if (state.isEditing) R.string.card_edit_title else R.string.card_new_title,
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
            label = { Text(stringResource(R.string.card_name)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(state.closingText, viewModel::onClosingChange, R.string.card_closing_day, enabled)
        NumberField(state.dueText, viewModel::onDueChange, R.string.card_due_day, enabled)
        HintText(R.string.card_days_hint)
        if (state.isEditing) HintText(R.string.card_reschedule_hint)
        if (state.deleteBlocked) {
            ErrorText(R.string.card_delete_blocked)
        } else if (state.status.failed) {
            ErrorText(R.string.save_failed)
        }
        Button(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.entry_save))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = R.string.card_delete_confirm_title,
            message = R.string.card_delete_confirm_message,
            confirmLabel = R.string.entry_delete,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
