package io.github.igornunesfontoura.yourfinances.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.YearMonth

/**
 * The month the Início and Lançamentos tabs show; switching it in one tab switches both. It outlives
 * the Activity, so a fresh start calls [reset] to open on the current month again.
 */
class SelectedMonth(initial: YearMonth) {
    private val month = MutableStateFlow(initial)

    val value: StateFlow<YearMonth> = month.asStateFlow()

    fun reset(current: YearMonth) {
        month.value = current
    }

    fun previous() = month.update { it.minusMonths(1) }

    fun next() = month.update { it.plusMonths(1) }
}
