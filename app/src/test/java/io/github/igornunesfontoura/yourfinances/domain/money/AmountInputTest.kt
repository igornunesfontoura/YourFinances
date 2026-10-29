package io.github.igornunesfontoura.yourfinances.domain.money

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AmountInputTest {
    @Test
    fun `last two digits are cents`() {
        assertEquals(1234L, centsFromAmountDigits("1234"))
        assertEquals(5L, centsFromAmountDigits("5"))
        assertEquals(100000L, centsFromAmountDigits("100000"))
    }

    @Test
    fun `empty input is zero`() {
        assertEquals(0L, centsFromAmountDigits(""))
        assertEquals("", normalizeAmountDigits("000"))
    }

    @Test
    fun `non digits and leading zeros are dropped`() {
        assertEquals("123456", normalizeAmountDigits("R$ 1.234,56"))
        assertEquals("42", normalizeAmountDigits("0042"))
        assertEquals("15", normalizeAmountDigits("-15"))
    }

    @Test
    fun `input is capped at the maximum digits`() {
        val digits = normalizeAmountDigits("1234567890123456")
        assertEquals(MAX_AMOUNT_DIGITS, digits.length)
        assertEquals(123456789012L, centsFromAmountDigits(digits))
    }
}
