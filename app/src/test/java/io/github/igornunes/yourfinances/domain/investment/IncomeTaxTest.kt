package io.github.igornunes.yourfinances.domain.investment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.LocalDate

class IncomeTaxTest {
    private val investedOn = LocalDate.of(2026, 1, 1)

    @ParameterizedTest
    @CsvSource("0,22.5", "180,22.5", "181,20", "360,20", "361,17.5", "720,17.5", "721,15", "3000,15")
    fun `regressive table by holding days`(days: Long, expected: String) {
        val rate = regressiveIncomeTaxRate(investedOn, investedOn.plusDays(days))
        assertEquals(0, BigDecimal(expected).compareTo(rate))
    }

    @Test
    fun `tax on gain`() {
        assertEquals(1500L, estimatedIncomeTaxCents(10000, investedOn, investedOn.plusDays(800)))
        assertEquals(2250L, estimatedIncomeTaxCents(10000, investedOn, investedOn.plusDays(30)))
    }

    @Test
    fun `half a cent rounds up`() {
        // 333 × 22.5% = 74.925
        assertEquals(75L, estimatedIncomeTaxCents(333, investedOn, investedOn.plusDays(10)))
    }

    @Test
    fun `no tax without gain`() {
        assertEquals(0L, estimatedIncomeTaxCents(0, investedOn, investedOn.plusDays(10)))
        assertEquals(0L, estimatedIncomeTaxCents(-500, investedOn, investedOn.plusDays(10)))
    }

    @Test
    fun `redemption before investment is rejected`() {
        assertThrows<IllegalArgumentException> { regressiveIncomeTaxRate(investedOn, investedOn.minusDays(1)) }
    }
}
