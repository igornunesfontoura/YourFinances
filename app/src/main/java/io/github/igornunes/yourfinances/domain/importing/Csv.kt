package io.github.igornunes.yourfinances.domain.importing

/** A CSV file split into its header and rows, all rows padded to the header's width. */
data class CsvTable(val header: List<String>, val rows: List<List<String>>)

/**
 * RFC 4180 parsing with the separator guessed from the first line (`;`, `,` or tab, whichever
 * appears most outside quotes). A UTF-8 BOM and blank lines are ignored.
 */
fun parseCsv(text: String): CsvTable {
    val content = text.removePrefix("\uFEFF")
    val separator = guessSeparator(content.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty())
    val records = mutableListOf<List<String>>()
    val field = StringBuilder()
    var record = mutableListOf<String>()
    var quoted = false
    var i = 0
    while (i < content.length) {
        val c = content[i]
        when {
            quoted && c == '"' && content.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
            c == '"' -> quoted = !quoted
            !quoted && c == separator -> { record += field.toString(); field.clear() }
            !quoted && (c == '\n' || c == '\r') -> {
                if (c == '\r' && content.getOrNull(i + 1) == '\n') i++
                record += field.toString(); field.clear()
                if (record.any { it.isNotBlank() }) records += record
                record = mutableListOf()
            }
            else -> field.append(c)
        }
        i++
    }
    record += field.toString()
    if (record.any { it.isNotBlank() }) records += record
    if (records.isEmpty()) return CsvTable(emptyList(), emptyList())
    val header = records.first().map { it.trim() }
    val width = records.maxOf { it.size }
    return CsvTable(header + List(width - header.size) { "" }, records.drop(1).map { it.map(String::trim) + List(width - it.size) { "" } })
}

private fun guessSeparator(line: String): Char {
    var quoted = false
    val counts = mutableMapOf(';' to 0, ',' to 0, '\t' to 0)
    line.forEach { c ->
        if (c == '"') quoted = !quoted else if (!quoted && c in counts) counts[c] = counts.getValue(c) + 1
    }
    return counts.maxBy { it.value }.takeIf { it.value > 0 }?.key ?: ','
}

/**
 * Which columns hold the date, the description and the amount. [positiveIsExpense] is for files
 * that list purchases as positive numbers, as many credit card statements do.
 */
data class CsvMapping(
    val dateColumn: Int,
    val descriptionColumn: Int,
    val amountColumn: Int,
    val positiveIsExpense: Boolean = false,
)

/**
 * A best guess from the header names (data, descrição/histórico, valor) and, failing that, from
 * the content. Null when no column looks like a date or an amount.
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
    val amount = byName("valor", "amount", "value", "quantia").takeIf { it != date }
        ?: columns.firstOrNull { it != date && share(it) { v -> parseAmountCents(v) != null } > 0.8 }
        ?: return null
    val description = byName("descri", "hist", "title", "título", "memo", "estabelecimento", "lançamento")
        ?.takeIf { it != date && it != amount }
        ?: columns.filter { it != date && it != amount }.maxByOrNull { column -> table.rows.sumOf { it[column].length } }
        ?: amount
    val amounts = table.rows.mapNotNull { parseAmountCents(it[amount]) }
    val mostlyPositive = amounts.isNotEmpty() && amounts.count { it > 0 } > amounts.size * 0.7
    return CsvMapping(date, description, amount, positiveIsExpense = mostlyPositive)
}

/** Rows read with [mapping], and how many rows had no valid date or amount and were left out. */
data class CsvReading(val lines: List<StatementLine>, val skippedRows: Int)

fun readCsv(table: CsvTable, mapping: CsvMapping): CsvReading {
    var skipped = 0
    val lines = table.rows.mapNotNull { row ->
        val date = parseStatementDate(row[mapping.dateColumn])
        val amount = parseAmountCents(row[mapping.amountColumn])
        if (date == null || amount == null) {
            skipped++
            null
        } else {
            StatementLine(date, if (mapping.positiveIsExpense) -amount else amount, row[mapping.descriptionColumn])
        }
    }
    return CsvReading(lines, skipped)
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
