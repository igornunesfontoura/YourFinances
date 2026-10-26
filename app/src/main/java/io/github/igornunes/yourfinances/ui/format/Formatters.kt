package io.github.igornunes.yourfinances.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR)
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
private val shortDateFormatter = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", PT_BR)

/** "R$ 1.234,56". Goes through BigDecimal so cents never pass through a Double. */
fun formatCents(cents: Long): String =
    NumberFormat.getCurrencyInstance(PT_BR).format(BigDecimal.valueOf(cents, 2))

fun formatMonth(month: YearMonth): String = month.format(monthFormatter).capitalized()

fun formatDay(date: LocalDate): String = date.format(dayFormatter).capitalized()

/** "março". */
fun formatMonthName(month: Month): String = month.getDisplayName(TextStyle.FULL_STANDALONE, PT_BR)

fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

private fun String.capitalized() = replaceFirstChar { it.titlecase(PT_BR) }
