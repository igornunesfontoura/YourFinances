package io.github.igornunesfontoura.yourfinances.domain.model

/** Income categories (Salário) and expense categories (Mercado), fixed or flexible alike. */
enum class CategoryKind { INCOME, EXPENSE }

/**
 * [monthlyLimitCents] is an optional budget for an expense category: how much the user means to
 * spend on it per month. It is a reference only and does not change "quanto ainda posso gastar".
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val monthlyLimitCents: Long? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(monthlyLimitCents == null || monthlyLimitCents > 0) { "monthlyLimitCents must be positive" }
        require(monthlyLimitCents == null || kind == CategoryKind.EXPENSE) { "only expense categories have a limit" }
    }
}
