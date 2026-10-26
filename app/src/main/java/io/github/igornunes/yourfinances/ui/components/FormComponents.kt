package io.github.igornunes.yourfinances.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.ui.format.AmountVisualTransformation
import io.github.igornunes.yourfinances.ui.format.formatShortDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Scaffold with a back arrow and a scrolling column that stays above the keyboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScaffold(
    @StringRes title: Int,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = actions,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** Cash-register amount input: digits only, shown as currency. */
@Composable
fun AmountField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    @StringRes label: Int,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = digits,
        onValueChange = onDigitsChange,
        label = { Text(stringResource(label)) },
        visualTransformation = AmountVisualTransformation,
        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.End),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Small whole-number input (days, installment counts). */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    @StringRes supportingText: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it in '0'..'9' }.take(3)) },
        label = { Text(stringResource(label)) },
        supportingText = supportingText?.let { { Text(stringResource(it)) } },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                enabled = enabled,
                icon = {},
            ) { Text(stringResource(label(option)), maxLines = 1, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/**
 * Chip showing [date] (or "Hoje") that opens a date picker. With [onClear], a null [date] shows
 * [emptyLabel] and the chip gets a clear button.
 */
@Composable
fun DateChip(
    date: LocalDate?,
    today: LocalDate,
    @StringRes label: Int,
    enabled: Boolean,
    onDateChange: (LocalDate) -> Unit,
    @StringRes emptyLabel: Int? = null,
    onClear: (() -> Unit)? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val text = when (date) {
        null -> emptyLabel?.let { stringResource(it) }.orEmpty()
        today -> stringResource(R.string.entry_today)
        else -> formatShortDate(date)
    }
    AssistChip(
        onClick = { showPicker = true },
        enabled = enabled,
        label = { Text(stringResource(R.string.date_chip, stringResource(label), text)) },
        leadingIcon = { Icon(Icons.Filled.DateRange, null) },
        trailingIcon = if (onClear != null && date != null) {
            {
                IconButton(onClick = onClear, enabled = enabled) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_clear))
                }
            }
        } else {
            null
        },
    )
    if (showPicker) {
        DatePickerDialogFor(
            initial = date ?: today,
            onConfirm = {
                onDateChange(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** The Material date picker works in UTC midnight milliseconds. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(initial: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
fun ConfirmDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes confirmLabel: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(confirmLabel)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun HintText(@StringRes text: Int) = HintText(stringResource(text))

@Composable
fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun ErrorText(@StringRes text: Int) = ErrorText(stringResource(text))

@Composable
fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}
