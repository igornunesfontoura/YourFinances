package io.github.igornunesfontoura.yourfinances.domain.importing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class ValuesTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "-1.234,56|-123456",
            "1,234.56|123456",
            "1234.56|123456",
            "1234,5|123450",
            "R$ 12,30|1230",
            "(45,00)|-4500",
            "45,00-|-4500",
            "+7|700",
            "1.234|123400",
            "0,05|5",
            "-0.5|-50",
        ],
    )
    fun `amounts as banks write them`(text: String, cents: Long) {
        assertEquals(cents, parseAmountCents(text))
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = ["abc", "''", "1,234,5678", "12.345,678", "--5"])
    fun `not amounts`(text: String) {
        assertNull(parseAmountCents(text))
    }

    @org.junit.jupiter.api.Test
    fun `non-breaking space, as in formatted currency`() {
        assertEquals(-123456L, parseAmountCents("-R$\u00A01.234,56"))
    }

    @org.junit.jupiter.api.Test
    fun `decimal hint decides a lone separator`() {
        assertEquals(1234500L, parseAmountCents("12345.000".dropLast(1), decimalSeparator = '.'))
        assertEquals(-123400L, parseAmountCents("-1234", decimalSeparator = '.'))
    }

    @ParameterizedTest
    @CsvSource(value = ["05/03/2026", "05/03/26", "2026-03-05", "05-03-2026", "05.03.2026", "05/03/2026 14:30", "2026-03-05T10:00:00"])
    fun `dates, day first`(text: String) {
        assertEquals(LocalDate.of(2026, 3, 5), parseStatementDate(text))
    }

    @ParameterizedTest
    @CsvSource(value = ["31/02/2026", "2026/03/05", "março", "13/13/2026"])
    fun `not dates`(text: String) {
        assertNull(parseStatementDate(text))
    }

    @org.junit.jupiter.api.Test
    fun `debit and credit marks, unicode minus, extra zero decimals`() {
        assertEquals(-5000L, parseAmountCents("50,00 D"))
        assertEquals(5000L, parseAmountCents("50,00C"))
        assertEquals(-1234L, parseAmountCents("\u221212,34"))
        assertEquals(-5000L, parseAmountCents("-50.000", decimalSeparator = '.'))
        assertNull(parseAmountCents("-50.001", decimalSeparator = '.'))
        assertEquals(LocalDate.of(2026, 3, 5), parseStatementDate("5/3/2026"))
    }
}
