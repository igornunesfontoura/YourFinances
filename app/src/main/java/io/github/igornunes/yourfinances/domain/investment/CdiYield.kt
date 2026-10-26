package io.github.igornunes.yourfinances.domain.investment

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

private val MATH = MathContext.DECIMAL128
private val HUNDRED = BigDecimal(100)

/** One value of a BCB SGS series, in percent (CDI series 12 is percent per business day). */
data class RateObservation(val date: LocalDate, val valuePercent: BigDecimal)

/**
 * Accumulated factor of a post-fixed investment at [percentOfIndex]% of a daily index:
 * the product, business day by business day, of `1 + dailyRate/100 × percentOfIndex/100`.
 * Only observations in [from] (inclusive) to [until] (exclusive) are used.
 */
fun accumulatedDailyFactor(
    observations: List<RateObservation>,
    percentOfIndex: BigDecimal,
    from: LocalDate,
    until: LocalDate,
): BigDecimal {
    val share = percentOfIndex.divide(HUNDRED, MATH)
    return observations
        .filter { !it.date.isBefore(from) && it.date.isBefore(until) }
        .fold(BigDecimal.ONE) { factor, observation ->
            val dailyGrowth = BigDecimal.ONE.add(observation.valuePercent.divide(HUNDRED, MATH).multiply(share, MATH), MATH)
            factor.multiply(dailyGrowth, MATH)
        }
}

/** Gross value in cents; fractions of a cent are truncated so the estimate never overstates. */
fun grossValueCents(principalCents: Long, factor: BigDecimal): Long =
    BigDecimal.valueOf(principalCents)
        .multiply(factor, MATH)
        .setScale(0, RoundingMode.DOWN)
        .longValueExact()
