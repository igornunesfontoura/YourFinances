package io.github.igornunesfontoura.yourfinances.domain.importing

/** A CSV file split into its header and rows, all rows padded to the header's width. */
data class CsvTable(val header: List<String>, val rows: List<List<String>>)

private const val SAMPLE_LINES = 10

/**
 * RFC 4180 parsing, lenient as bank exports need: the separator (`;`, `,` or tab) is the one that
 * splits the first lines most consistently; a quote only opens a quoted field at its start, so a
 * stray quote inside a description (`TV 32" LED`) stays literal. Lines before the header that are
 * narrower than the data (a title, the account number) are skipped. A UTF-8 BOM and blank lines
 * are ignored.
 */
fun parseCsv(text: String): CsvTable {
    val content = text.removePrefix("\uFEFF")
    val sample = content.lineSequence().filter { it.isNotBlank() }.take(SAMPLE_LINES).joinToString("\n")
    val separator = listOf(';', ',', '\t').maxBy { candidate -> consistency(splitRecords(sample, candidate)) }
    val records = splitRecords(content, separator)
    if (records.isEmpty()) return CsvTable(emptyList(), emptyList())
    // The header is the first line as wide as most of the file.
    val width = records.groupingBy { it.size }.eachCount().maxWith(compareBy({ it.value }, { it.key })).key
    val headerIndex = records.indexOfFirst { it.size >= width }
    val header = records[headerIndex].map { it.trim() }
    val full = maxOf(header.size, records.drop(headerIndex).maxOf { it.size })
    return CsvTable(
        header + List(full - header.size) { "" },
        records.drop(headerIndex + 1).map { row -> row.map(String::trim) + List(full - row.size) { "" } },
    )
}

/** How well a separator splits the sample: lines sharing the most common width, times that width. */
private fun consistency(records: List<List<String>>): Int {
    val widths = records.map { it.size }.filter { it > 1 }
    if (widths.isEmpty()) return 0
    val (width, count) = widths.groupingBy { it }.eachCount().maxWith(compareBy({ it.value }, { it.key })).toPair()
    return count * 100 + width
}

private fun splitRecords(content: String, separator: Char): List<List<String>> {
    val records = mutableListOf<List<String>>()
    val field = StringBuilder()
    var record = mutableListOf<String>()
    var quoted = false
    var i = 0
    fun endRecord() {
        record += field.toString()
        field.clear()
        if (record.any { it.isNotBlank() }) records += record
        record = mutableListOf()
    }
    while (i < content.length) {
        val c = content[i]
        when {
            quoted && c == '"' && content.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
            quoted && c == '"' -> quoted = false
            quoted -> field.append(c)
            c == '"' && field.isBlank() -> { field.clear(); quoted = true }
            c == separator -> { record += field.toString(); field.clear() }
            c == '\n' || c == '\r' -> {
                if (c == '\r' && content.getOrNull(i + 1) == '\n') i++
                endRecord()
            }
            else -> field.append(c)
        }
        i++
    }
    endRecord()
    return records
}

/**
 * Which columns hold the date, the description and the amount. Some banks split the amount into a
 * debit column ([amountColumn]) and a [creditColumn]. [positiveIsExpense] is for files that list
 * purchases as positive numbers, as many credit card statements do.
 */
data class CsvMapping(
    val dateColumn: Int,
    val descriptionColumn: Int,
    val amountColumn: Int,
    val positiveIsExpense: Boolean = false,
    val creditColumn: Int? = null,
)

/** Columns that hold numbers but never the transaction amount. */
private val NOT_AMOUNT = listOf("saldo", "doc", "número", "numero", "nº", "id", "parcela", "agência", "conta")

/**
 * A best guess from the header names (data, descrição/histórico, valor, or débito and crédito)
 * and, failing that, from the content: amounts are the column with cents. Null when no column
 * looks like a date or an amount.
 */
fun guessCsvMapping(table: CsvTable): CsvMapping? {
    val names = table.header.map { it.lowercase() }
    fun byName(vararg keys: String) = names.indexOfFirst { name -> keys.any { name.contains(it) } }.takeIf { it >= 0 }
    fun share(column: Int, test: (String) -> Boolean): Double {
        val values = table.rows.map { it[column] }.filter { it.isNotBlank() }
        return if (values.isEmpty()) 0.0 else values.count(test).toDouble() / values.size
    }
    val columns = table.header.indices
    val date = byName("data", "date") ?: columns.firstOrNull { share(it) { v -> parseStatementDate(v) != null } > 0.8 }
        ?: return null
    val debit = byName("débito", "debito", "saída", "saida")
    val credit = byName("crédito", "credito", "entrada")
    val split = debit != null && credit != null && debit != credit && date !in setOf(debit, credit)
    val amount = if (split) {
        debit!!
    } else {
        byName("valor", "amount", "value", "quantia").takeIf { it != date }
            ?: columns
                .filter { it != date && NOT_AMOUNT.none { key -> names[it].contains(key) } }
                .filter { share(it) { v -> parseAmountCents(v) != null } > 0.8 }
                .maxByOrNull { share(it) { v -> Regex("[.,]\\d{2}\\s*-?$").containsMatchIn(v) } }
            ?: return null
    }
    val used = setOfNotNull(date, amount, credit.takeIf { split })
    val description = byName("descri", "hist", "title", "título", "memo", "estabelecimento", "lançamento")
        ?.takeIf { it !in used }
        ?: columns.filter { it !in used }.maxByOrNull { column -> table.rows.sumOf { it[column].length } }
        ?: amount
    val amounts = table.rows.mapNotNull { parseAmountCents(it[amount]) }
    val mostlyPositive = !split && amounts.isNotEmpty() && amounts.count { it > 0 } > amounts.size * 0.7
    return CsvMapping(date, description, amount, positiveIsExpense = mostlyPositive, creditColumn = credit.takeIf { split })
}

/** Rows read with [mapping], and how many rows had no valid date or amount and were left out. */
data class CsvReading(val lines: List<StatementLine>, val skippedRows: Int)

/**
 * A file without a header has a transaction in its first line: when that line reads as one with
 * [mapping], it is kept as data.
 */
fun readCsv(table: CsvTable, mapping: CsvMapping): CsvReading {
    var skipped = 0
    val headerLine = readRow(table.header, mapping)
    val lines = table.rows.mapNotNull { row ->
        readRow(row, mapping) ?: run {
            skipped++
            null
        }
    }
    return CsvReading(listOfNotNull(headerLine) + lines, skipped)
}

private fun readRow(row: List<String>, mapping: CsvMapping): StatementLine? {
    val date = parseStatementDate(row.getOrElse(mapping.dateColumn) { "" }) ?: return null
    val creditColumn = mapping.creditColumn
    val amount = if (creditColumn != null) {
        // Debit and credit columns usually hold unsigned values; the column gives the direction.
        val credit = row.getOrElse(creditColumn) { "" }.takeIf { it.isNotBlank() }?.let { parseAmountCents(it) ?: return null }
        val debit = row.getOrElse(mapping.amountColumn) { "" }.takeIf { it.isNotBlank() }?.let { parseAmountCents(it) ?: return null }
        when {
            credit != null && credit != 0L -> kotlin.math.abs(credit)
            debit != null -> -kotlin.math.abs(debit)
            else -> return null
        }
    } else {
        val value = parseAmountCents(row.getOrElse(mapping.amountColumn) { "" }) ?: return null
        if (mapping.positiveIsExpense) -value else value
    }
    return StatementLine(date, amount, row.getOrElse(mapping.descriptionColumn) { "" })
}

/** Text of a CSV file: UTF-8 when it is valid UTF-8, otherwise Windows-1252, common in Brazilian bank exports. */
fun decodeStatementText(bytes: ByteArray): String {
    val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
    return try {
        decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    } catch (_: java.nio.charset.CharacterCodingException) {
        String(bytes, java.nio.charset.Charset.forName("windows-1252"))
    }
}
