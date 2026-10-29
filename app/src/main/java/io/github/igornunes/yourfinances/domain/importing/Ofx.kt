package io.github.igornunes.yourfinances.domain.importing

import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Text of an OFX file. OFX 1.x (SGML) declares its charset in the header (Brazilian banks usually
 * send `CHARSET:1252`); OFX 2.x is XML and UTF-8 unless its prolog says otherwise.
 */
fun decodeOfx(bytes: ByteArray): String {
    val head = String(bytes, 0, minOf(bytes.size, 1024), Charsets.ISO_8859_1)
    val name = when {
        Regex("ENCODING:\\s*UTF-?8", RegexOption.IGNORE_CASE).containsMatchIn(head) -> "UTF-8"
        Regex("encoding=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(head) != null ->
            Regex("encoding=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(head)!!.groupValues[1]
        head.trimStart().startsWith("<?xml") -> "UTF-8"
        else -> "windows-1252"
    }
    val charset = runCatching { Charset.forName(name) }.getOrDefault(Charset.forName("windows-1252"))
    return String(bytes, charset).removePrefix("﻿")
}

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
        val fields = Regex("<([A-Za-z0-9.]+)>([^<]*)").findAll(block)
            .associate { it.groupValues[1].uppercase() to unescape(it.groupValues[2].trim()) }
        val date = fields["DTPOSTED"]?.let(::ofxDate) ?: throw InvalidStatementException("transaction without date")
        val amount = fields["TRNAMT"]?.let { parseAmountCents(it, decimalSeparator = if ('.' in it) '.' else ',') }
            ?: throw InvalidStatementException("transaction without amount")
        val description = listOfNotNull(fields["MEMO"], fields["NAME"]).firstOrNull { it.isNotBlank() }.orEmpty()
        StatementLine(date, amount, description, fields["FITID"]?.takeIf { it.isNotBlank() })
    }
}

private val OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")

private fun ofxDate(value: String): LocalDate? = try {
    LocalDate.parse(value.take(8), OFX_DATE)
} catch (_: DateTimeParseException) {
    null
}

private fun unescape(value: String) = value
    .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
