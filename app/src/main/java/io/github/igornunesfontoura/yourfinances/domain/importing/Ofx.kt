package io.github.igornunesfontoura.yourfinances.domain.importing

import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Text of an OFX file. A UTF-8 BOM, or content that is valid UTF-8 with accents, is read as UTF-8
 * whatever the header says (several banks declare CHARSET:1252 and write UTF-8). Otherwise OFX 1.x
 * (SGML) follows its header, usually `CHARSET:1252`, and OFX 2.x (XML) its prolog.
 */
fun decodeOfx(bytes: ByteArray): String {
    val hasBom = bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
    if (hasBom || (bytes.any { it < 0 } && isValidUtf8(bytes))) return String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
    val head = String(bytes, 0, minOf(bytes.size, 1024), Charsets.ISO_8859_1)
    val declared = Regex("encoding=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(head)?.groupValues?.get(1)
    val name = when {
        Regex("ENCODING:\\s*UTF-?8", RegexOption.IGNORE_CASE).containsMatchIn(head) -> "UTF-8"
        declared != null -> declared
        head.trimStart().startsWith("<?xml") -> "UTF-8"
        else -> "windows-1252"
    }
    val charset = runCatching { Charset.forName(name) }.getOrDefault(Charset.forName("windows-1252"))
    return String(bytes, charset)
}

private fun isValidUtf8(bytes: ByteArray): Boolean = try {
    Charsets.UTF_8.newDecoder()
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        .decode(java.nio.ByteBuffer.wrap(bytes))
    true
} catch (_: java.nio.charset.CharacterCodingException) {
    false
}

/** Only the start of the file is looked at, so large CSV files are not scanned for it. */
fun looksLikeOfx(bytes: ByteArray): Boolean =
    String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1).let { it.contains("<OFX>", ignoreCase = true) || it.contains("OFXHEADER") }

fun looksLikeOfx(text: String): Boolean = text.contains("<OFX>", ignoreCase = true)

/** Thrown for an OFX file with no transaction this app can read. */
class InvalidStatementException(message: String) : Exception(message)

/**
 * Every `STMTTRN` (bank and credit card statements alike), in file order. Leaf elements may be
 * unclosed, as SGML allows. Lines missing a date or an amount make the whole file invalid, so a
 * statement is never imported partially without the user knowing.
 */
fun parseOfx(text: String): List<StatementLine> {
    val blocks = Regex("<STMTTRN>(.*?)</STMTTRN>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .findAll(text).map { it.groupValues[1] }.toList()
    if (blocks.isEmpty()) throw InvalidStatementException("no transactions")
    return blocks.map { block ->
        // The first occurrence wins, so a nested <PAYEE><NAME> does not replace the transaction's NAME.
        val fields = Regex("<([A-Za-z0-9.]+)>([^<]*)").findAll(block)
            .map { it.groupValues[1].uppercase() to unescape(it.groupValues[2].trim()) }
            .filter { it.second.isNotEmpty() }
            .distinctBy { it.first }
            .toMap()
        val date = fields["DTPOSTED"]?.let(::ofxDate) ?: throw InvalidStatementException("transaction without date")
        val amount = fields["TRNAMT"]?.let { parseAmountCents(it, decimalSeparator = if ('.' in it) '.' else ',') }
            ?: throw InvalidStatementException("transaction without amount")
        val description = listOfNotNull(fields["MEMO"], fields["NAME"]).firstOrNull { it.isNotBlank() }.orEmpty()
        StatementLine(date, amount, description, fields["FITID"]?.takeIf { it.isNotBlank() })
    }
}

private val OFX_DATE = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(java.time.format.ResolverStyle.STRICT)

private fun ofxDate(value: String): LocalDate? = try {
    LocalDate.parse(value.take(8), OFX_DATE)
} catch (_: DateTimeParseException) {
    null
}

private fun unescape(value: String) = value
    .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
