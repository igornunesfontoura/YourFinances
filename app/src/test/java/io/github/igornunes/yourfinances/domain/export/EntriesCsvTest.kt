package io.github.igornunes.yourfinances.domain.export

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EntriesCsvTest {
    private fun entry(id: Long, type: EntryType, amount: Long, description: String, on: LocalDate) =
        Entry(id = id, type = type, amountCents = amount, description = description, occurredOn = on, paidOn = on)

    @Test
    fun `header and one row per entry, oldest first, CRLF`() {
        val csv = entriesCsv(
            listOf(
                entry(2, EntryType.FLEXIBLE_EXPENSE, 4590, "Cinema", LocalDate.of(2026, 3, 10)),
                entry(1, EntryType.INCOME, 500000, "Salário", LocalDate.of(2026, 3, 5)),
            ),
            emptyMap(),
            emptyMap(),
        )
        assertEquals(
            "occurred_on,paid_on,type,description,amount,status,card,category,installment_number,installment_count\r\n" +
                "2026-03-05,2026-03-05,INCOME,Salário,5000.00,CONFIRMED,,,,\r\n" +
                "2026-03-10,2026-03-10,FLEXIBLE_EXPENSE,Cinema,-45.90,CONFIRMED,,,,\r\n",
            csv,
        )
    }

    @Test
    fun `card, category, installment and status are filled in`() {
        val installment = entry(1, EntryType.FIXED_EXPENSE, 5, "TV", LocalDate.of(2026, 3, 5)).copy(
            paidOn = LocalDate.of(2026, 4, 5),
            status = EntryStatus.PREDICTED,
            creditCardId = 2,
            categoryId = 3,
            installmentPurchaseId = 9,
            installmentNumber = 2,
            installmentCount = 10,
        )
        val row = entriesCsv(listOf(installment), mapOf(2L to "Nubank"), mapOf(3L to "Casa")).split("\r\n")[1]
        assertEquals("2026-03-05,2026-04-05,FIXED_EXPENSE,TV,-0.05,PREDICTED,Nubank,Casa,2,10", row)
    }

    @Test
    fun `fields with commas, quotes or line breaks are quoted`() {
        val tricky = entry(1, EntryType.INVESTMENT, 100, "Tesouro, \"Selic\"\nparte 1", LocalDate.of(2026, 3, 5))
        val csv = entriesCsv(listOf(tricky), mapOf(), mapOf())
        assertEquals(
            "2026-03-05,2026-03-05,INVESTMENT,\"Tesouro, \"\"Selic\"\"\nparte 1\",-1.00,CONFIRMED,,,,\r\n",
            csv.substringAfter("\r\n"),
        )
    }

    @Test
    fun `text that looks like a formula is not run by spreadsheets`() {
        val entries = listOf("=HYPERLINK(1)", "+1", "-x", "@SUM", "ok").mapIndexed { index, text ->
            entry(index.toLong(), EntryType.FLEXIBLE_EXPENSE, 100, text, LocalDate.of(2026, 3, 5))
        }
        val descriptions = entriesCsv(entries, emptyMap(), emptyMap()).split("\r\n").drop(1).dropLast(1)
            .map { it.split(",")[3] }
        assertEquals(listOf("'=HYPERLINK(1)", "'+1", "'-x", "'@SUM", "ok"), descriptions)
    }
}
