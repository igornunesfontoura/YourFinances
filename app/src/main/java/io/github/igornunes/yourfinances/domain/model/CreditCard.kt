package io.github.igornunes.yourfinances.domain.model

/** Days are nominal (1..31); months shorter than the day use their last day. */
data class CreditCard(
    val id: Long = 0,
    val name: String,
    val closingDay: Int,
    val dueDay: Int,
) {
    init {
        require(closingDay in 1..31) { "closingDay must be in 1..31" }
        require(dueDay in 1..31) { "dueDay must be in 1..31" }
    }
}
