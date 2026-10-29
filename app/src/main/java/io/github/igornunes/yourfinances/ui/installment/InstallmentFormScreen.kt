package io.github.igornunes.yourfinances.ui.installment

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.domain.installment.MAX_INSTALLMENTS
import io.github.igornunes.yourfinances.ui.components.AmountField
import io.github.igornunes.yourfinances.ui.components.DateChip
import io.github.igornunes.yourfinances.ui.components.ErrorText
import io.github.igornunes.yourfinances.ui.components.FormScaffold
import io.github.igornunes.yourfinances.ui.components.HintText
import io.github.igornunes.yourfinances.ui.components.NumberField
import io.github.igornunes.yourfinances.ui.components.ChoiceChips
import io.github.igornunes.yourfinances.ui.components.PaymentSelector
import io.github.igornunes.yourfinances.ui.format.formatCents
import io.github.igornunes.yourfinances.ui.format.formatShortDate
import org.koin.androidx.compose.koinViewModel

@Composable
fun InstallmentFormRoute(onDone: () -> Unit, viewModel: InstallmentFormViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.status.finished) { if (state.status.finished) currentOnDone() }
    val enabled = !state.status.isBusy

    FormScaffold(title = R.string.installment_new_title, onBack = onDone) {
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.entry_description)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        AmountField(state.totalDigits, viewModel::onTotalChange, R.string.installment_total, enabled, onDone = viewModel::save)
        NumberField(
            value = state.countText,
            onValueChange = viewModel::onCountChange,
            label = R.string.installment_count,
            enabled = enabled,
        )
        if (state.countText.isNotEmpty() && state.count == null) {
            ErrorText(stringResource(R.string.installment_count_invalid, MAX_INSTALLMENTS))
        }
        state.preview?.let { parts ->
            val first = parts.first()
            val other = parts.getOrElse(1) { first }
            HintText(
                if (first == other) {
                    stringResource(R.string.installment_preview_equal, parts.size, formatCents(first))
                } else {
                    stringResource(R.string.installment_preview_remainder, formatCents(first), parts.size - 1, formatCents(other))
                },
            )
        }
        DateChip(state.purchasedOn, state.today, R.string.installment_purchased_on, enabled, viewModel::onPurchaseDateChange)
        if (state.cards.isNotEmpty()) PaymentSelector(state.cards, state.cardId, viewModel::onCardChange, enabled)
        if (state.categories.isNotEmpty()) {
            ChoiceChips(R.string.category_none, state.categories.map { it.id to it.name }, state.categoryId, viewModel::onCategoryChange, enabled)
        }
        if (state.card == null) {
            DateChip(state.firstPaymentOn, state.today, R.string.installment_first_payment, enabled, viewModel::onFirstPaymentChange)
        }
        state.paymentDates?.let { dates ->
            HintText(stringResource(R.string.installment_dates, formatShortDate(dates.first()), formatShortDate(dates.last())))
        }
        if (state.firstBeforePurchase) ErrorText(R.string.installment_first_before_purchase)
        if (state.status.failed) ErrorText(R.string.save_failed)
        Button(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.entry_save))
        }
    }
}
