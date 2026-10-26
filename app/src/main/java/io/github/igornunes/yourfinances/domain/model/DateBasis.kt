package io.github.igornunes.yourfinances.domain.model

import java.time.LocalDate

/** Which date of an [Entry] a report uses. Reports must always state their basis. */
enum class DateBasis {
    /** Competência: [Entry.occurredOn]. */
    ACCRUAL,

    /** Caixa: [Entry.paidOn]. */
    CASH,
    ;

    fun dateOf(entry: Entry): LocalDate = when (this) {
        ACCRUAL -> entry.occurredOn
        CASH -> entry.paidOn
    }
}
