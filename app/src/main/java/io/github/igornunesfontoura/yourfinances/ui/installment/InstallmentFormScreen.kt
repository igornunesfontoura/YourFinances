package io.github.igornunesfontoura.yourfinances.ui.installment

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.domain.installment.MAX_INSTALLMENTS
import io.github.igornunesfontoura.yourfinances.ui.components.DateChip
import io.github.igornunesfontoura.yourfinances.ui.components.ErrorText
import io.github.igornunesfontoura.yourfinances.ui.components.FormCard
import io.github.igornunesfontoura.yourfinances.ui.components.FormScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.FormSection
import io.github.igornunesfontoura.yourfinances.ui.components.HeroAmountField
import io.github.igornunesfontoura.yourfinances.ui.components.HintText
import io.github.igornunesfontoura.yourfinances.ui.components.NumberField
import io.github.igornunesfontoura.yourfinances.ui.components.ChoiceChips
import io.github.igornunesfontoura.yourfinances.ui.components.PaymentSelector
import io.github.igornunesfontoura.yourfinances.ui.components.PrimaryButton
import io.github.igornunesfontoura.yourfinances.ui.format.formatCents
import io.github.igornunesfontoura.yourfinances.ui.format.formatShortDate
import io.github.igornunesfontoura.yourfinances.ui.theme.finance
import org.koin.androidx.compose.koinViewModel

@Composable
fun InstallmentFormRoute(onDone: () -> Unit, viewModel: InstallmentFormViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.status.finished) { if (state.status.finished) currentOnDone() }
    val enabled = !state.status.isBusy

    FormScaffold(title = R.string.installment_new_title, onBack = onDone) {
        HeroAmountField(
            digits = state.totalDigits,
            onDigitsChange = viewModel::onTotalChange,
            label = R.string.installment_total,
            color = MaterialTheme.finance.expense,
            enabled = enabled,
            onDone = viewModel::save,
        )
        FormCard {
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.entry_description)) },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
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
            if (state.cards.isNotEmpty()) {
                FormSection(R.string.entry_payment) { PaymentSelector(state.cards, state.cardId, viewModel::onCardChange, enabled) }
            }
            if (state.categories.isNotEmpty()) {
                FormSection(R.string.entry_category) {
                    ChoiceChips(R.string.category_none, state.categories.map { it.id to it.name }, state.categoryId, viewModel::onCategoryChange, enabled)
                }
            }
            if (state.card == null) {
                DateChip(state.firstPaymentOn, state.today, R.string.installment_first_payment, enabled, viewModel::onFirstPaymentChange)
            }
            state.paymentDates?.let { dates ->
                HintText(stringResource(R.string.installment_dates, formatShortDate(dates.first()), formatShortDate(dates.last())))
            }
        }
        if (state.firstBeforePurchase) ErrorText(R.string.installment_first_before_purchase)
        if (state.status.failed) ErrorText(R.string.save_failed)
        PrimaryButton(stringResource(R.string.entry_save), onClick = viewModel::save, enabled = state.canSave)
    }
}
