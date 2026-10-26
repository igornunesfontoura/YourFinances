package io.github.igornunes.yourfinances.ui.entry

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.ui.components.AmountField
import io.github.igornunes.yourfinances.ui.components.ConfirmDialog
import io.github.igornunes.yourfinances.ui.components.DateChip
import io.github.igornunes.yourfinances.ui.components.ErrorText
import io.github.igornunes.yourfinances.ui.components.FormScaffold
import io.github.igornunes.yourfinances.ui.components.HintText
import io.github.igornunes.yourfinances.ui.components.PaymentSelector
import io.github.igornunes.yourfinances.ui.components.SegmentedSelector
import io.github.igornunes.yourfinances.ui.format.formatShortDate
import io.github.igornunes.yourfinances.ui.format.label
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate

@Composable
fun EntryFormRoute(
    entryId: Long?,
    onDone: () -> Unit,
    viewModel: EntryFormViewModel = koinViewModel(key = "entry-$entryId") { parametersOf(entryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.status.finished) { if (state.status.finished) currentOnDone() }
    EntryFormScreen(
        state = state,
        onAmountChange = viewModel::onAmountChange,
        onTypeChange = viewModel::onTypeChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDateChange = viewModel::onDateChange,
        onApplyToUpcomingChange = viewModel::onApplyToUpcomingChange,
        onCardChange = viewModel::onCardChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onBack = onDone,
    )
}

@Composable
fun EntryFormScreen(
    state: EntryFormUiState,
    onAmountChange: (String) -> Unit,
    onTypeChange: (EntryType) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onApplyToUpcomingChange: (Boolean) -> Unit,
    onCardChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val valuesEnabled = !state.isBusy && state.canEditValues

    FormScaffold(
        title = if (state.isEditing) R.string.entry_edit_title else R.string.entry_new_title,
        onBack = onBack,
        actions = {
            if (state.isEditing) {
                IconButton(onClick = { confirmDelete = true }, enabled = !state.isBusy) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.entry_delete))
                }
            }
        },
    ) {
        // New entries open with the numeric keyboard ready: FAB, digits, save. The effect lives
        // in the same subcomposition as the field so the requester is attached when it runs.
        LaunchedEffect(state.status.loading) {
            if (!state.status.loading && !state.isEditing) {
                withFrameNanos {}
                amountFocus.requestFocus()
            }
        }

        if (!state.canEditValues) HintText(R.string.entry_locked_hint)

        AmountField(
            digits = state.amountDigits,
            onDigitsChange = onAmountChange,
            label = R.string.entry_amount,
            enabled = valuesEnabled,
            onDone = onSave,
            modifier = Modifier.focusRequester(amountFocus),
        )

        SegmentedSelector(EntryType.entries, state.type, { it.label }, onTypeChange, enabled = valuesEnabled)

        DateChip(
            date = state.occurredOn,
            today = state.today,
            label = R.string.entry_date,
            enabled = valuesEnabled,
            onDateChange = onDateChange,
        )

        if (state.showsPayment) {
            PaymentSelector(state.cards, state.cardId, onCardChange, enabled = valuesEnabled)
            state.cardDueDate?.let { HintText(stringResource(R.string.payment_card_due, formatShortDate(it))) }
        }

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.entry_description)) },
            singleLine = true,
            enabled = !state.isBusy,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.canApplyToUpcoming) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.entry_apply_to_upcoming))
                    HintText(R.string.entry_apply_to_upcoming_hint)
                }
                Switch(
                    checked = state.applyToUpcoming,
                    onCheckedChange = onApplyToUpcomingChange,
                    enabled = !state.isBusy,
                )
            }
        }

        if (state.status.failed) ErrorText(R.string.save_failed)

        Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.entry_save))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = if (state.deletesWholePurchase) R.string.purchase_delete_confirm_title else R.string.entry_delete_confirm_title,
            message = if (state.deletesWholePurchase) R.string.purchase_delete_confirm_message else R.string.entry_delete_confirm_message,
            confirmLabel = R.string.entry_delete,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
