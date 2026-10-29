package io.github.igornunesfontoura.yourfinances.domain.money

/** Up to 9.999.999.999,99: far beyond any personal entry, far below [Long.MAX_VALUE]. */
const val MAX_AMOUNT_DIGITS = 12

/**
 * Cash-register style input: only digits count, leading zeros are dropped and the last two digits
 * are the cents ("1234" means 12,34). Extra digits beyond [MAX_AMOUNT_DIGITS] are ignored.
 */
fun normalizeAmountDigits(input: String): String =
    input.filter { it in '0'..'9' }.trimStart('0').take(MAX_AMOUNT_DIGITS)

/** Cents represented by cash-register [digits]; zero when there are none. */
fun centsFromAmountDigits(digits: String): Long = normalizeAmountDigits(digits).ifEmpty { "0" }.toLong()
