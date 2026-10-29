package io.github.igornunes.yourfinances.ui.recurrence

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
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.ui.components.AmountField
import io.github.igornunes.yourfinances.ui.components.ChoiceChips
import io.github.igornunes.yourfinances.ui.components.ConfirmDialog
import io.github.igornunes.yourfinances.ui.components.DateChip
import io.github.igornunes.yourfinances.ui.components.ErrorText
import io.github.igornunes.yourfinances.ui.components.FormScaffold
import io.github.igornunes.yourfinances.ui.components.HintText
import io.github.igornunes.yourfinances.ui.components.NumberField
import io.github.igornunes.yourfinances.ui.components.SegmentedSelector
import io.github.igornunes.yourfinances.ui.format.formatMonthName
import io.github.igornunes.yourfinances.ui.format.label
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RecurrenceFormRoute(
    recurrenceId: Long?,
    onDone: () -> Unit,
    viewModel: RecurrenceFormViewModel = koinViewModel(key = "recurrence-$recurrenceId") { parametersOf(recurrenceId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.status.finished) { if (state.status.finished) currentOnDone() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.isBusy

    FormScaffold(
        title = if (state.isEditing) R.string.recurrence_edit_title else R.string.recurrence_new_title,
        onBack = onDone,
        actions = {
            if (state.isEditing) {
                IconButton(onClick = { confirmDelete = true }, enabled = enabled) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.entry_delete))
                }
            }
        },
    ) {
        AmountField(state.amountDigits, viewModel::onAmountChange, R.string.entry_amount, enabled, onDone = viewModel::save)
        SegmentedSelector(EntryType.entries, state.type, { it.label }, viewModel::onTypeChange, enabled)
        if (state.showsPocket) {
            ChoiceChips(R.string.pocket_none, state.pockets.map { it.id to it.name }, state.pocketId, viewModel::onPocketChange, enabled)
        }
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.entry_description)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        SegmentedSelector(Frequency.entries, state.frequency, { it.label }, viewModel::onFrequencyChange, enabled)
        NumberField(
            value = state.dayText,
            onValueChange = viewModel::onDayChange,
            label = R.string.recurrence_day,
            enabled = enabled,
            supportingText = R.string.recurrence_day_hint,
        )
        DateChip(state.startsOn, state.today, R.string.recurrence_starts_on, enabled, viewModel::onStartChange)
        if (state.frequency == Frequency.YEARLY) {
            HintText(stringResource(R.string.recurrence_yearly_hint, formatMonthName(state.startsOn.month)))
        }
        DateChip(
            date = state.endsOn,
            today = state.today,
            label = R.string.recurrence_ends_on,
            enabled = enabled,
            onDateChange = viewModel::onEndChange,
            emptyLabel = R.string.recurrence_no_end,
            onClear = { viewModel.onEndChange(null) },
        )
        if (state.endsBeforeStart) ErrorText(R.string.recurrence_ends_before_start)
        HintText(R.string.recurrence_prediction_hint)
        if (state.status.failed) ErrorText(R.string.save_failed)
        Button(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.entry_save))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = R.string.recurrence_delete_confirm_title,
            message = R.string.recurrence_delete_confirm_message,
            confirmLabel = R.string.entry_delete,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
