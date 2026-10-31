package io.github.igornunesfontoura.yourfinances.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.ui.format.AmountVisualTransformation
import io.github.igornunesfontoura.yourfinances.ui.format.label

/** Big centered cash-register amount, the first thing a quick entry asks for. */
@Composable
fun HeroAmountField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    @StringRes label: Int,
    color: Color,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelText = stringResource(label)
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(labelText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = digits,
            onValueChange = onDigitsChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.displayMedium.copy(color = color, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(color),
            visualTransformation = AmountVisualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = labelText },
        )
    }
}

/** One chip per entry type, with its icon and color; wraps to a second line on narrow screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryTypeSelector(
    selected: EntryType,
    onSelect: (EntryType) -> Unit,
    enabled: Boolean,
    types: List<EntryType> = EntryType.entries,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        types.forEach { type ->
            val tint = type.tint
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                enabled = enabled,
                label = { Text(stringResource(type.label)) },
                leadingIcon = { Icon(type.icon, null, Modifier.padding(start = 2.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tint.copy(alpha = 0.16f),
                    selectedLabelColor = tint,
                    selectedLeadingIconColor = tint,
                ),
            )
        }
    }
}

/** Card holding the form's optional fields. */
@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

/** Small label over one field inside a [FormCard]. */
@Composable
fun FormSection(@StringRes label: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** Full-width tall button for the form's main action. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(56.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
