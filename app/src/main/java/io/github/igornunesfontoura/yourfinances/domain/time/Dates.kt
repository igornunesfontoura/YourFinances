package io.github.igornunesfontoura.yourfinances.domain.time

import java.time.LocalDate
import java.time.YearMonth

/** [day] in this month, or the last day of the month when the month is shorter. */
fun YearMonth.atDayClamped(day: Int): LocalDate = atDay(minOf(day, lengthOfMonth()))
