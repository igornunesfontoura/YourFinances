package io.github.igornunes.yourfinances.domain.importing

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Cents in an amount as banks write it: "-1.234,56", "1234.56", "R$ 12,30", "(45,00)", "45,00-".
 * With both separators the last one is the decimal one; with only one, it is decimal when one or
 * two digits follow it (or [decimalSeparator] says so), otherwise it groups thousands. Never goes
 * through floating point. Null when the text is not an amount or has more than two decimals.
 */
fun parseAmountCents(text: String, decimalSeparator: Char? = null): Long? {
    var raw = text.trim().replace("R$", "").replace(" ", "").replace("\u00A0", "")
    var negative = false
    if (raw.startsWith("(") && raw.endsWith(")")) {
        negative = true
        raw = raw.substring(1, raw.length - 1)
    }
    if (raw.startsWith("-")) {
        negative = !negative
        raw = raw.substring(1)
    } else if (raw.startsWith("+")) {
        raw = raw.substring(1)
    } else if (raw.endsWith("-")) {
        negative = !negative
        raw = raw.dropLast(1)
    }
    if (raw.isEmpty() || raw.any { it !in '0'..'9' && it != '.' && it != ',' }) return null
    val lastDot = raw.lastIndexOf('.')
    val lastComma = raw.lastIndexOf(',')
    val decimalIndex = when {
        lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
        decimalSeparator != null -> raw.lastIndexOf(decimalSeparator)
        else -> maxOf(lastDot, lastComma).takeIf { it >= 0 && raw.length - it - 1 in 1..2 } ?: -1
    }
    val integerText = if (decimalIndex >= 0) raw.substring(0, decimalIndex) else raw
    // Thousands groups after the first must have exactly three digits ("1.234.567", not "1,234,5678").
    val groups = integerText.split('.', ',')
    if (groups.size > 1 && (groups.first().length !in 1..3 || groups.drop(1).any { it.length != 3 })) return null
    val integerPart = integerText.filter { it.isDigit() }
    val fraction = if (decimalIndex >= 0) raw.substring(decimalIndex + 1) else ""
    if (fraction.length > 2 || fraction.any { !it.isDigit() } || integerPart.length > 15) return null
    if (integerPart.isEmpty() && fraction.isEmpty()) return null
    val cents = (integerPart.ifEmpty { "0" }.toLong() * 100) + fraction.padEnd(2, '0').ifEmpty { "00" }.toLong()
    return if (negative) -cents else cents
}

private val DATE_FORMATS = listOf("dd/MM/uuuu", "dd/MM/uu", "uuuu-MM-dd", "dd-MM-uuuu", "dd.MM.uuuu")
    .map { DateTimeFormatter.ofPattern(it).withResolverStyle(ResolverStyle.STRICT) }

/** A date as Brazilian banks write it (day first), or ISO. Text after the date (a time) is ignored. */
fun parseStatementDate(text: String): LocalDate? {
    val value = text.trim().substringBefore(' ').substringBefore('T')
    return DATE_FORMATS.firstNotNullOfOrNull { format ->
        try {
            LocalDate.parse(value, format)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
