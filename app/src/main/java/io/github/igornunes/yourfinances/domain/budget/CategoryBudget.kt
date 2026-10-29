package io.github.igornunes.yourfinances.domain.budget

import io.github.igornunes.yourfinances.domain.model.Category
import io.github.igornunes.yourfinances.domain.model.CategoryKind
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import java.time.YearMonth

/** [category] is null for expenses without a category. */
data class CategorySpending(val category: Category?, val spentCents: Long) {
    val limitCents: Long? get() = category?.monthlyLimitCents

    /** Share of the limit already spent; above 1 when over budget, null without a limit. */
    val usage: Double? get() = limitCents?.let { spentCents.toDouble() / it }

    val overLimit: Boolean get() = limitCents?.let { spentCents > it } == true
}

/**
 * Expenses (fixed and flexible, predicted included, as in "quanto ainda posso gastar") of [month]
 * per expense category: categories with a limit or with spending, largest spending first, then
 * the uncategorized total when there is any. Categories that no longer exist count as uncategorized.
 */
fun categorySpending(
    entries: List<Entry>,
    categories: List<Category>,
    month: YearMonth,
    basis: DateBasis,
): List<CategorySpending> {
    val expenses = entries.filter { it.type.categoryKind == CategoryKind.EXPENSE && YearMonth.from(basis.dateOf(it)) == month }
    val expenseCategories = categories.filter { it.kind == CategoryKind.EXPENSE }
    val known = expenseCategories.map { it.id }.toSet()
    val byCategory = expenses.groupBy { it.categoryId?.takeIf { id -> id in known } }
    val listed = expenseCategories
        .map { CategorySpending(it, byCategory[it.id].orEmpty().sumOf { entry -> entry.amountCents }) }
        .filter { it.spentCents > 0 || it.limitCents != null }
        .sortedWith(compareByDescending<CategorySpending> { it.spentCents }.thenBy { it.category?.name })
    val uncategorized = byCategory[null].orEmpty().sumOf { it.amountCents }
    return listed + listOfNotNull(CategorySpending(null, uncategorized).takeIf { uncategorized > 0 })
}
