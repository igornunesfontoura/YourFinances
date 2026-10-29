package io.github.igornunes.yourfinances.domain.export

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import java.math.BigDecimal

/** Stable, machine-friendly column names: the file is meant for spreadsheets and scripts. */
val ENTRIES_CSV_HEADER = listOf(
    "occurred_on",
    "paid_on",
    "type",
    "description",
    "amount",
    "status",
    "card",
    "category",
    "installment_number",
    "installment_count",
)

/**
 * RFC 4180 CSV of [entries], oldest first: ISO dates, `.` as the decimal separator and CRLF line
 * ends. `amount` is signed as a cash flow (income positive, everything else negative), so a
 * spreadsheet sum gives the balance; `type` keeps the full meaning.
 */
fun entriesCsv(entries: List<Entry>, cardNames: Map<Long, String>, categoryNames: Map<Long, String>): String {
    val rows = entries.sortedWith(compareBy({ it.occurredOn }, { it.id })).map { entry ->
        listOf(
            entry.occurredOn.toString(),
            entry.paidOn.toString(),
            entry.type.name,
            textField(entry.description),
            signedAmount(entry),
            entry.status.name,
            textField(entry.creditCardId?.let { cardNames[it] }.orEmpty()),
            textField(entry.categoryId?.let { categoryNames[it] }.orEmpty()),
            entry.installmentNumber?.toString().orEmpty(),
            entry.installmentCount?.toString().orEmpty(),
        )
    }
    return (listOf(ENTRIES_CSV_HEADER) + rows).joinToString(separator = "") { row ->
        row.joinToString(",", transform = ::csvField) + "\r\n"
    }
}

private fun signedAmount(entry: Entry): String {
    val cents = if (entry.type == EntryType.INCOME) entry.amountCents else -entry.amountCents
    return BigDecimal.valueOf(cents, 2).toPlainString()
}

/**
 * Free text starting like a formula gets a leading `'`, so a spreadsheet shows it instead of
 * running it (descriptions will come from imported bank statements too).
 */
private fun textField(value: String): String =
    if (value.firstOrNull() in FORMULA_STARTS) "'$value" else value

private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
