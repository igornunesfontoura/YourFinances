package io.github.igornunes.yourfinances.ui.format

import androidx.annotation.StringRes
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.domain.card.InvoiceStatus

@get:StringRes
val InvoiceStatus.label: Int
    get() = when (this) {
        InvoiceStatus.OPEN -> R.string.invoice_open
        InvoiceStatus.CLOSED -> R.string.invoice_closed
        InvoiceStatus.OVERDUE -> R.string.invoice_overdue
        InvoiceStatus.PAID -> R.string.invoice_paid
    }
