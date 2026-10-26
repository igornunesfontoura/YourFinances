package io.github.igornunes.yourfinances.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence

@get:StringRes
val EntryType.label: Int
    get() = when (this) {
        EntryType.INCOME -> R.string.entry_type_income
        EntryType.FIXED_EXPENSE -> R.string.entry_type_fixed_expense
        EntryType.FLEXIBLE_EXPENSE -> R.string.entry_type_flexible_expense
        EntryType.INVESTMENT -> R.string.entry_type_investment
    }

@get:StringRes
val DateBasis.label: Int
    get() = when (this) {
        DateBasis.ACCRUAL -> R.string.basis_accrual
        DateBasis.CASH -> R.string.basis_cash
    }

@get:StringRes
val DateBasis.hint: Int
    get() = when (this) {
        DateBasis.ACCRUAL -> R.string.basis_accrual_hint
        DateBasis.CASH -> R.string.basis_cash_hint
    }

@get:StringRes
val Frequency.label: Int
    get() = when (this) {
        Frequency.MONTHLY -> R.string.frequency_monthly
        Frequency.YEARLY -> R.string.frequency_yearly
    }

/** "Todo dia 10" or "Todo ano, 10 de março". */
@Composable
fun scheduleText(recurrence: Recurrence): String = when (recurrence.frequency) {
    Frequency.MONTHLY -> stringResource(R.string.recurrence_schedule_monthly, recurrence.dayOfMonth)
    Frequency.YEARLY -> stringResource(
        R.string.recurrence_schedule_yearly,
        recurrence.dayOfMonth,
        formatMonthName(recurrence.startsOn.month),
    )
}
