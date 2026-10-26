package io.github.igornunes.yourfinances.ui.components

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate

/** Today's date, emitted again when the day turns, so date-dependent screens never go stale. */
fun Clock.todayFlow(): Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now(this@todayFlow))
        delay(60_000)
    }
}.distinctUntilChanged()
