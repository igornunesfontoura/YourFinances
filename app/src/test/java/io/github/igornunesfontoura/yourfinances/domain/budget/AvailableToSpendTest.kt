package io.github.igornunesfontoura.yourfinances.domain.budget

import io.github.igornunesfontoura.yourfinances.domain.model.DateBasis
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class AvailableToSpendTest {
    private val march = YearMonth.of(2026, 3)

    private fun entry(
        type: EntryType,
        amount: Long,
        occurredOn: LocalDate = LocalDate.of(2026, 3, 10),
        paidOn: LocalDate = occurredOn,
        status: EntryStatus = EntryStatus.CONFIRMED,
    ) = Entry(type = type, amountCents = amount, description = "x", occurredOn = occurredOn, paidOn = paidOn, status = status)

    @Test
    fun `income minus fixed minus goal minus flexible`() {
        val entries = listOf(
            entry(EntryType.INCOME, 500000),
            entry(EntryType.INCOME, 50000),
            entry(EntryType.FIXED_EXPENSE, 150000),
            entry(EntryType.FLEXIBLE_EXPENSE, 12345),
        )
        val result = availableToSpend(entries, march, investmentGoalCents = 100000, basis = DateBasis.CASH)
        assertEquals(550000L, result.incomeCents)
        assertEquals(150000L, result.fixedExpensesCents)
        assertEquals(12345L, result.flexibleExpensesCents)
        assertEquals(550000L - 150000L - 100000L - 12345L, result.availableCents)
    }

    @Test
    fun `predicted fixed expenses count as much as confirmed ones`() {
        val entries = listOf(
            entry(EntryType.INCOME, 300000),
            entry(EntryType.FIXED_EXPENSE, 10000, status = EntryStatus.PREDICTED),
            entry(EntryType.FIXED_EXPENSE, 20000, status = EntryStatus.CONFIRMED),
        )
        val result = availableToSpend(entries, march, 0, DateBasis.CASH)
        assertEquals(30000L, result.fixedExpensesCents)
        assertEquals(270000L, result.availableCents)
    }

    @Test
    fun `investment entries are not subtracted, the goal is`() {
        val entries = listOf(
            entry(EntryType.INCOME, 300000),
            entry(EntryType.INVESTMENT, 80000),
        )
        assertEquals(200000L, availableToSpend(entries, march, 100000, DateBasis.CASH).availableCents)
    }

    @Test
    fun `contributions above the goal reserve more`() {
        val entries = listOf(entry(EntryType.INCOME, 300000), entry(EntryType.INVESTMENT, 150000))
        val result = availableToSpend(entries, march, 100000, DateBasis.CASH)
        assertEquals(150000L, result.reservedForSavingsCents)
        assertEquals(150000L, result.availableCents)
    }

    @Test
    fun `contributing and withdrawing the same amount cancel out`() {
        val entries = listOf(
            entry(EntryType.INCOME, 300000),
            entry(EntryType.INVESTMENT, 100000),
            entry(EntryType.WITHDRAWAL, 100000),
        )
        assertEquals(300000L, availableToSpend(entries, march, 0, DateBasis.CASH).availableCents)
    }

    @Test
    fun `predicted withdrawals count only once confirmed, predicted contributions already reserve`() {
        val entries = listOf(
            entry(EntryType.INCOME, 300000),
            entry(EntryType.WITHDRAWAL, 50000, status = EntryStatus.PREDICTED),
            entry(EntryType.INVESTMENT, 40000, status = EntryStatus.PREDICTED),
        )
        val result = availableToSpend(entries, march, 0, DateBasis.CASH)
        assertEquals(0L, result.withdrawalsCents)
        assertEquals(40000L, result.contributionsCents)
        assertEquals(260000L, result.availableCents)
    }

    @Test
    fun `withdrawals are available to spend again`() {
        val entries = listOf(
            entry(EntryType.INCOME, 300000),
            entry(EntryType.WITHDRAWAL, 50000),
            entry(EntryType.FLEXIBLE_EXPENSE, 20000),
        )
        val result = availableToSpend(entries, march, 100000, DateBasis.CASH)
        assertEquals(50000L, result.withdrawalsCents)
        assertEquals(300000L + 50000L - 100000L - 20000L, result.availableCents)
    }

    @Test
    fun `entries of other months are ignored`() {
        val entries = listOf(
            entry(EntryType.INCOME, 300000),
            entry(EntryType.FLEXIBLE_EXPENSE, 5000, occurredOn = LocalDate.of(2026, 2, 28)),
            entry(EntryType.FLEXIBLE_EXPENSE, 5000, occurredOn = LocalDate.of(2026, 4, 1)),
            entry(EntryType.FLEXIBLE_EXPENSE, 5000, occurredOn = LocalDate.of(2025, 3, 15)),
        )
        assertEquals(300000L, availableToSpend(entries, march, 0, DateBasis.CASH).availableCents)
    }

    @Test
    fun `basis decides the month of a card purchase`() {
        // Dinner in March, paid in the April invoice.
        val dinner = entry(EntryType.FLEXIBLE_EXPENSE, 20000, LocalDate.of(2026, 3, 28), LocalDate.of(2026, 4, 5))
        val income = entry(EntryType.INCOME, 100000)

        val accrual = availableToSpend(listOf(income, dinner), march, 0, DateBasis.ACCRUAL)
        assertEquals(80000L, accrual.availableCents)
        assertEquals(DateBasis.ACCRUAL, accrual.basis)

        val cash = availableToSpend(listOf(income, dinner), march, 0, DateBasis.CASH)
        assertEquals(100000L, cash.availableCents)
    }

    @Test
    fun `can be negative when overspent`() {
        val entries = listOf(
            entry(EntryType.INCOME, 100000),
            entry(EntryType.FIXED_EXPENSE, 90000),
            entry(EntryType.FLEXIBLE_EXPENSE, 30000),
        )
        assertEquals(-20000L, availableToSpend(entries, march, 0, DateBasis.CASH).availableCents)
    }

    @Test
    fun `empty month leaves only the goal`() {
        assertEquals(-50000L, availableToSpend(emptyList(), march, 50000, DateBasis.CASH).availableCents)
    }

    @Test
    fun `committed share is what is spent or set aside over what came in`() {
        val entries = listOf(
            entry(EntryType.INCOME, 300000),
            entry(EntryType.WITHDRAWAL, 100000),
            entry(EntryType.FIXED_EXPENSE, 150000),
            entry(EntryType.FLEXIBLE_EXPENSE, 50000),
        )
        assertEquals(0.75, availableToSpend(entries, march, 100000, DateBasis.CASH).committedShare)
    }

    @Test
    fun `committed share goes above one when overspent`() {
        val entries = listOf(entry(EntryType.INCOME, 100000), entry(EntryType.FIXED_EXPENSE, 150000))
        assertEquals(1.5, availableToSpend(entries, march, 0, DateBasis.CASH).committedShare)
    }

    @Test
    fun `committed share is null without money coming in`() {
        val entries = listOf(entry(EntryType.FIXED_EXPENSE, 150000))
        assertNull(availableToSpend(entries, march, 0, DateBasis.CASH).committedShare)
    }

    @Test
    fun `spending is fixed plus flexible, savings apart`() {
        val entries = listOf(
            entry(EntryType.FIXED_EXPENSE, 150000),
            entry(EntryType.FLEXIBLE_EXPENSE, 12345),
            entry(EntryType.INVESTMENT, 50000),
        )
        assertEquals(162345L, availableToSpend(entries, march, 0, DateBasis.CASH).spendingCents)
    }

    @Test
    fun `committed percent rounds down so 100 means nothing left`() {
        val almost = listOf(entry(EntryType.INCOME, 100000), entry(EntryType.FIXED_EXPENSE, 99960))
        assertEquals(99, availableToSpend(almost, march, 0, DateBasis.CASH).committedPercent)
        val all = listOf(entry(EntryType.INCOME, 100000), entry(EntryType.FIXED_EXPENSE, 100000))
        assertEquals(100, availableToSpend(all, march, 0, DateBasis.CASH).committedPercent)
        assertNull(availableToSpend(emptyList(), march, 0, DateBasis.CASH).committedPercent)
    }
}
