package io.github.igornunes.yourfinances.domain.budget

import io.github.igornunes.yourfinances.domain.model.Category
import io.github.igornunes.yourfinances.domain.model.CategoryKind
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class CategoryBudgetTest {
    private val march = YearMonth.of(2026, 3)
    private val market = Category(id = 1, name = "Mercado", kind = CategoryKind.EXPENSE, monthlyLimitCents = 100000)
    private val fun_ = Category(id = 2, name = "Lazer", kind = CategoryKind.EXPENSE)
    private val health = Category(id = 3, name = "Saúde", kind = CategoryKind.EXPENSE, monthlyLimitCents = 30000)
    private val salary = Category(id = 4, name = "Salário", kind = CategoryKind.INCOME)

    private fun entry(type: EntryType, amount: Long, category: Long?, on: LocalDate = LocalDate.of(2026, 3, 10), status: EntryStatus = EntryStatus.CONFIRMED) =
        Entry(type = type, amountCents = amount, description = "", occurredOn = on, paidOn = on, status = status, categoryId = category)

    @Test
    fun `spending per expense category, largest first, then uncategorized`() {
        val entries = listOf(
            entry(EntryType.FLEXIBLE_EXPENSE, 60000, 1),
            entry(EntryType.FIXED_EXPENSE, 50000, 1, status = EntryStatus.PREDICTED),
            entry(EntryType.FLEXIBLE_EXPENSE, 20000, 2),
            entry(EntryType.FLEXIBLE_EXPENSE, 7000, null),
            entry(EntryType.FLEXIBLE_EXPENSE, 3000, 99),
            entry(EntryType.INCOME, 500000, 4),
            entry(EntryType.INVESTMENT, 100000, null),
            entry(EntryType.FLEXIBLE_EXPENSE, 9999, 2, on = LocalDate.of(2026, 4, 1)),
        )
        val result = categorySpending(entries, listOf(market, fun_, health, salary), march, DateBasis.ACCRUAL)

        assertEquals(
            listOf(
                CategorySpending(market, 110000),
                CategorySpending(fun_, 20000),
                CategorySpending(health, 0),
                CategorySpending(null, 10000),
            ),
            result,
        )
        assertTrue(result[0].overLimit)
        assertEquals(1.1, result[0].usage!!, 1e-9)
        assertNull(result[1].usage)
        assertFalse(result[2].overLimit)
    }

    @Test
    fun `categories with no limit and no spending are left out`() {
        assertEquals(emptyList<CategorySpending>(), categorySpending(emptyList(), listOf(fun_), march, DateBasis.ACCRUAL))
    }

    @Test
    fun `category rules`() {
        assertThrows<IllegalArgumentException> { Category(name = " ", kind = CategoryKind.EXPENSE) }
        assertThrows<IllegalArgumentException> { Category(name = "x", kind = CategoryKind.INCOME, monthlyLimitCents = 100) }
        assertThrows<IllegalArgumentException> { Category(name = "x", kind = CategoryKind.EXPENSE, monthlyLimitCents = 0) }
    }
}
