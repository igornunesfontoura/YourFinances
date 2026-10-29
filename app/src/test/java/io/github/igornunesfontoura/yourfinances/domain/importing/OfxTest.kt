package io.github.igornunesfontoura.yourfinances.domain.importing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class OfxTest {
    /** OFX 1.x as Brazilian banks send it: SGML, unclosed leaves, Windows-1252, comma decimals. */
    private val sgml = """
        OFXHEADER:100
        DATA:OFXSGML
        VERSION:102
        ENCODING:USASCII
        CHARSET:1252

        <OFX>
        <BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>
        <STMTTRN>
        <TRNTYPE>DEBIT
        <DTPOSTED>20260305120000[-3:BRT]
        <TRNAMT>-45,90
        <FITID>ABC123
        <MEMO>Padaria São João &amp; Cia
        </STMTTRN>
        <STMTTRN>
        <TRNTYPE>CREDIT
        <DTPOSTED>20260306
        <TRNAMT>5000.00
        <FITID>ABC124
        <NAME>Salário
        </STMTTRN>
        </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1>
        </OFX>
    """.trimIndent()

    @Test
    fun `reads every transaction with its id`() {
        val bytes = sgml.toByteArray(charset("windows-1252"))
        assertTrue(looksLikeOfx(bytes))
        val text = decodeOfx(bytes)
        assertEquals(
            listOf(
                StatementLine(LocalDate.of(2026, 3, 5), -4590, "Padaria São João & Cia", "ABC123"),
                StatementLine(LocalDate.of(2026, 3, 6), 500000, "Salário", "ABC124"),
            ),
            parseOfx(text),
        )
    }

    @Test
    fun `xml ofx in utf-8, closed tags, credit card statement`() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?><?OFX OFXHEADER="200"?>
            <OFX><CREDITCARDMSGSRSV1><CCSTMTTRNRS><CCSTMTRS><BANKTRANLIST>
            <STMTTRN><TRNTYPE>DEBIT</TRNTYPE><DTPOSTED>20261201</DTPOSTED><TRNAMT>-129.99</TRNAMT><FITID>n1</FITID><MEMO>Livraria Ação</MEMO></STMTTRN>
            </BANKTRANLIST></CCSTMTRS></CCSTMTTRNRS></CREDITCARDMSGSRSV1></OFX>"""
        val lines = parseOfx(decodeOfx(xml.toByteArray(Charsets.UTF_8)))
        assertEquals(listOf(StatementLine(LocalDate.of(2026, 12, 1), -12999, "Livraria Ação", "n1")), lines)
    }

    @Test
    fun `files without readable transactions are refused`() {
        assertFalse(looksLikeOfx("date,amount".toByteArray()))
        assertThrows<InvalidStatementException> { parseOfx("<OFX></OFX>") }
        assertThrows<InvalidStatementException> { parseOfx("<OFX><STMTTRN><TRNAMT>-1,00</STMTTRN></OFX>") }
        assertThrows<InvalidStatementException> { parseOfx("<OFX><STMTTRN><DTPOSTED>20260101<TRNAMT>x</STMTTRN></OFX>") }
    }

    @Test
    fun `utf-8 content declared as 1252 is still read right`() {
        val lines = parseOfx(decodeOfx(sgml.replace("Salário", "Salário").toByteArray(Charsets.UTF_8)))
        assertEquals("Salário", lines[1].description)
        assertEquals("Padaria São João & Cia", lines[0].description)
    }

    @Test
    fun `extra zero decimals, repeated ids, strict dates`() {
        val file = "<OFX><STMTTRN><DTPOSTED>20260305<TRNAMT>-50.000<FITID>1<MEMO>A</STMTTRN>" +
            "<STMTTRN><DTPOSTED>20260306<TRNAMT>-10.00<FITID>1<MEMO>B</STMTTRN></OFX>"
        val lines = parseOfx(file)
        assertEquals(listOf(-5000L, -1000L), lines.map { it.amountCents })
        val keys = importKeys(lines)
        assertEquals(2, keys.toSet().size)
        assertThrows<InvalidStatementException> { parseOfx("<OFX><STMTTRN><DTPOSTED>20260231<TRNAMT>-1.00</STMTTRN></OFX>") }
    }
}
