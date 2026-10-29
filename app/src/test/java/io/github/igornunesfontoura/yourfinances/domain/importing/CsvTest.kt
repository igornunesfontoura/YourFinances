package io.github.igornunesfontoura.yourfinances.domain.importing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CsvTest {
    @Test
    fun `semicolon file with quotes, BOM and blank lines`() {
        val table = parseCsv("﻿Data;Histórico;Valor\r\n05/03/2026;\"Mercado; filial 2\";-123,45\r\n\r\n06/03/2026;\"Pix \"\"João\"\"\";50,00\r\n")
        assertEquals(listOf("Data", "Histórico", "Valor"), table.header)
        assertEquals(
            listOf(listOf("05/03/2026", "Mercado; filial 2", "-123,45"), listOf("06/03/2026", "Pix \"João\"", "50,00")),
            table.rows,
        )
    }

    @Test
    fun `mapping guessed from header names and read`() {
        val table = parseCsv("Data;Descrição;Valor\n05/03/2026;Mercado;-123,45\n06/03/2026;Salário;5.000,00\nxx;linha ruim;1,00")
        val mapping = guessCsvMapping(table)!!
        assertEquals(CsvMapping(0, 1, 2, positiveIsExpense = false), mapping)
        val reading = readCsv(table, mapping)
        assertEquals(
            listOf(
                StatementLine(LocalDate.of(2026, 3, 5), -12345, "Mercado"),
                StatementLine(LocalDate.of(2026, 3, 6), 500000, "Salário"),
            ),
            reading.lines,
        )
        assertEquals(1, reading.skippedRows)
    }

    @Test
    fun `card statement with positive purchases`() {
        val table = parseCsv("date,title,amount\n2026-03-05,Cinema,45.90\n2026-03-06,Livraria,30.00\n2026-03-07,Uber,20.00\n2026-03-08,Pagamento recebido,-75.90")
        val mapping = guessCsvMapping(table)!!
        assertEquals(CsvMapping(0, 1, 2, positiveIsExpense = true), mapping)
        assertEquals(listOf(-4590L, -3000L, -2000L, 7590L), readCsv(table, mapping).lines.map { it.amountCents })
    }

    @Test
    fun `columns guessed from content when names are unknown`() {
        val table = parseCsv("a,b,c\nMercado do bairro,-10.00,05/03/2026\nPadaria,-5.50,06/03/2026")
        assertEquals(CsvMapping(dateColumn = 2, descriptionColumn = 0, amountColumn = 1), guessCsvMapping(table))
        assertNull(guessCsvMapping(parseCsv("x,y\nfoo,bar")))
    }

    @Test
    fun `text is utf-8 when valid, windows-1252 otherwise`() {
        assertEquals("Salário", decodeStatementText("Salário".toByteArray(Charsets.UTF_8)))
        assertEquals("Salário", decodeStatementText("Salário".toByteArray(charset("windows-1252"))))
    }

    @Test
    fun `a stray quote inside a description stays literal`() {
        val table = parseCsv("Data;Descrição;Valor\n05/03/2026;TV 32\" LED;-999,00\n06/03/2026;Pix \"Joao;-10,00\n07/03/2026;Padaria;-5,00")
        assertEquals(listOf("TV 32\" LED", "Pix \"Joao", "Padaria"), table.rows.map { it[1] })
    }

    @Test
    fun `separate debit and credit columns, number columns ignored`() {
        val table = parseCsv(
            "Data;Histórico;Docto.;Crédito (R$);Débito (R$);Saldo (R$)\n" +
                "05/03/2026;Salário;123;5.000,00;;5.000,00\n" +
                "06/03/2026;Mercado;456;;123,45;4.876,55",
        )
        val mapping = guessCsvMapping(table)!!
        assertEquals(CsvMapping(dateColumn = 0, descriptionColumn = 1, amountColumn = 4, creditColumn = 3), mapping)
        assertEquals(listOf(500000L, -12345L), readCsv(table, mapping).lines.map { it.amountCents })
    }

    @Test
    fun `amount guessed from content skips document numbers and balances`() {
        val table = parseCsv("a;b;c;d\n05/03/2026;Mercado;12345;-123,45\n06/03/2026;Padaria;12346;-5,00")
        assertEquals(3, guessCsvMapping(table)!!.amountColumn)
    }

    @Test
    fun `title lines before the header are skipped and headerless files keep their first row`() {
        val withTitle = parseCsv("Extrato Conta Corrente\nAgência 0001\nData;Descrição;Valor\n05/03/2026;Mercado;-1,00")
        assertEquals(listOf("Data", "Descrição", "Valor"), withTitle.header)
        assertEquals(1, withTitle.rows.size)

        val headerless = parseCsv("05/03/2026;Mercado;-1,00\n06/03/2026;Padaria;-2,00")
        val reading = readCsv(headerless, CsvMapping(0, 1, 2))
        assertEquals(listOf(-100L, -200L), reading.lines.map { it.amountCents })
        assertEquals(0, reading.skippedRows)
    }
}
