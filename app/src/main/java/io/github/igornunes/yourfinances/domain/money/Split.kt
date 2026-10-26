package io.github.igornunes.yourfinances.domain.money

/**
 * Splits [totalCents] into [parts] values that add up exactly to the total.
 * The remainder cents go to the first part.
 */
fun splitCents(totalCents: Long, parts: Int): List<Long> {
    require(totalCents >= 0) { "totalCents must not be negative" }
    require(parts >= 1) { "parts must be at least 1" }
    val base = totalCents / parts
    val remainder = totalCents % parts
    return List(parts) { index -> if (index == 0) base + remainder else base }
}
