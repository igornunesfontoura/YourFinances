package io.github.igornunes.yourfinances.domain.investment

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Regressive fixed-income tax rate (percent) for the holding period. This is an estimate:
 * IOF on redemptions under 30 days and exempt products are not considered.
 */
fun regressiveIncomeTaxRate(investedOn: LocalDate, redeemedOn: LocalDate): BigDecimal {
    val days = ChronoUnit.DAYS.between(investedOn, redeemedOn)
    require(days >= 0) { "redeemedOn must not be before investedOn" }
    return when {
        days <= 180 -> BigDecimal("22.5")
        days <= 360 -> BigDecimal("20")
        days <= 720 -> BigDecimal("17.5")
        else -> BigDecimal("15")
    }
}

/** Estimated tax in cents on [grossGainCents]; zero when there is no gain. */
fun estimatedIncomeTaxCents(grossGainCents: Long, investedOn: LocalDate, redeemedOn: LocalDate): Long {
    val rate = regressiveIncomeTaxRate(investedOn, redeemedOn)
    if (grossGainCents <= 0) return 0
    return BigDecimal.valueOf(grossGainCents)
        .multiply(rate)
        .divide(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}
