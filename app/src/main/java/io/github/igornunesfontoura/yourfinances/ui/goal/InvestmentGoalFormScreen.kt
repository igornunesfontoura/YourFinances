package io.github.igornunesfontoura.yourfinances.ui.goal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.components.AmountField
import io.github.igornunesfontoura.yourfinances.ui.components.ErrorText
import io.github.igornunesfontoura.yourfinances.ui.components.FormScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.HintText
import io.github.igornunesfontoura.yourfinances.ui.components.PrimaryButton
import io.github.igornunesfontoura.yourfinances.ui.format.formatMonthInSentence
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.YearMonth

@Composable
fun InvestmentGoalFormRoute(
    month: YearMonth,
    onDone: () -> Unit,
    viewModel: InvestmentGoalFormViewModel = koinViewModel(key = "goal-form-$month") { parametersOf(month) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.status.finished) { if (state.status.finished) currentOnDone() }
    val enabled = !state.status.isBusy

    val amountFocus = remember { FocusRequester() }

    FormScaffold(title = R.string.goal_title, onBack = onDone) {
        // Opens with the numeric keyboard ready once the goal in force is loaded into the field.
        LaunchedEffect(state.status.loading) {
            if (!state.status.loading) {
                withFrameNanos {}
                amountFocus.requestFocus()
            }
        }

        AmountField(
            digits = state.amountDigits,
            onDigitsChange = viewModel::onAmountChange,
            label = R.string.goal_amount,
            enabled = enabled,
            onDone = viewModel::save,
            modifier = Modifier.focusRequester(amountFocus),
        )
        val starts = formatMonthInSentence(state.month)
        val endsBefore = state.endsBefore
        HintText(
            if (endsBefore == null) {
                stringResource(R.string.goal_starts_hint, starts)
            } else {
                stringResource(R.string.goal_until_next_hint, starts, formatMonthInSentence(endsBefore))
            },
        )
        if (state.status.failed) ErrorText(R.string.save_failed)
        PrimaryButton(stringResource(R.string.entry_save), onClick = viewModel::save, enabled = state.canSave)
    }
}
