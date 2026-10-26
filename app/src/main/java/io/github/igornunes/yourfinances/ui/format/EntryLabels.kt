package io.github.igornunes.yourfinances.ui.format

import androidx.annotation.StringRes
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.EntryType

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
