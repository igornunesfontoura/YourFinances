package io.github.igornunes.yourfinances.domain.investment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class CdiYieldTest {
    private val cdi = BigDecimal("0.040168")

    private fun day(dayOfMonth: Int) = RateObservation(LocalDate.of(2026, 3, dayOfMonth), cdi)

    @Test
    fun `100 percent of CDI for one day`() {
        val factor = accumulatedDailyFactor(listOf(day(2)), BigDecimal(100), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3))
        assertEquals(0, BigDecimal("1.00040168").compareTo(factor))
    }

    @Test
    fun `110 percent of CDI compounds day by day`() {
        val factor = accumulatedDailyFactor(
            listOf(day(2), day(3)),
            BigDecimal(110),
            LocalDate.of(2026, 3, 2),
            LocalDate.of(2026, 3, 4),
        )
        // 0.040168% × 1.10 = 0.0441848% per day, twice.
        assertEquals(0, BigDecimal("1.000441848").pow(2).compareTo(factor))
    }

    @Test
    fun `uses observations from the start date up to the day before the end date`() {
        val factor = accumulatedDailyFactor(
            listOf(day(1), day(2), day(3), day(4)),
            BigDecimal(100),
            LocalDate.of(2026, 3, 2),
            LocalDate.of(2026, 3, 4),
        )
        assertEquals(0, BigDecimal("1.00040168").pow(2).compareTo(factor))
    }

    @Test
    fun `no observations means no yield`() {
        val factor = accumulatedDailyFactor(emptyList(), BigDecimal(100), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4))
        assertEquals(0, BigDecimal.ONE.compareTo(factor))
    }

    @Test
    fun `gross value truncates fractions of a cent`() {
        // 100000 × 1.000883891229... = 100088.389... cents
        assertEquals(100088L, grossValueCents(100000, BigDecimal("1.000441848").pow(2)))
        assertEquals(100000L, grossValueCents(100000, BigDecimal.ONE))
    }
}
