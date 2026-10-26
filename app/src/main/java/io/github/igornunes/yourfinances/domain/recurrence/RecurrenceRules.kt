package io.github.igornunes.yourfinances.domain.recurrence

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.Recurrence
import java.time.LocalDate
import java.time.YearMonth

/** How many months after the current one get predicted entries. */
const val PREDICTION_MONTHS_AHEAD = 2L

/**
 * Dates predicted entries are kept for: from the first day of the current month to the end of
 * the month [PREDICTION_MONTHS_AHEAD] months later. Past months are never back-filled.
 */
data class PredictionWindow(val from: LocalDate, val to: LocalDate)

fun predictionWindow(today: LocalDate): PredictionWindow {
    val month = YearMonth.from(today)
    return PredictionWindow(month.atDay(1), month.plusMonths(PREDICTION_MONTHS_AHEAD).atEndOfMonth())
}

/** "Aplicar às próximas": the template takes the values the user just set on one occurrence. */
fun Recurrence.withValuesFrom(entry: Entry): Recurrence {
    require(entry.recurrenceId == id) { "entry does not belong to this recurrence" }
    return copy(
        type = entry.type,
        amountCents = entry.amountCents,
        description = entry.description,
        categoryId = entry.categoryId,
    )
}
