package io.github.igornunesfontoura.yourfinances.domain.model

import java.time.YearMonth

/**
 * How much the user plans to invest each month, from [startsIn] until a later goal replaces it.
 * Changing the goal adds a new one instead of rewriting past months.
 */
data class InvestmentGoal(val startsIn: YearMonth, val amountCents: Long) {
    init {
        require(amountCents >= 0) { "amountCents must not be negative" }
    }
}
