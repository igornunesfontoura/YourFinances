package io.github.igornunesfontoura.yourfinances.domain.budget

import io.github.igornunesfontoura.yourfinances.domain.model.InvestmentGoal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class InvestmentGoalsTest {
    private val march = InvestmentGoal(YearMonth.of(2026, 3), 100000)
    private val june = InvestmentGoal(YearMonth.of(2026, 6), 150000)
    private val goals = listOf(june, march)

    @Test
    fun `no goal before the first one starts`() {
        assertNull(goals.goalFor(YearMonth.of(2026, 2)))
        assertNull(emptyList<InvestmentGoal>().goalFor(YearMonth.of(2026, 3)))
    }

    @Test
    fun `a goal holds from its month until the next one`() {
        assertEquals(march, goals.goalFor(YearMonth.of(2026, 3)))
        assertEquals(march, goals.goalFor(YearMonth.of(2026, 5)))
        assertEquals(june, goals.goalFor(YearMonth.of(2026, 6)))
    }

    @Test
    fun `the latest goal holds across the year`() {
        assertEquals(june, goals.goalFor(YearMonth.of(2027, 1)))
    }

    @Test
    fun `a zero goal stops reserving money`() {
        val stopped = goals + InvestmentGoal(YearMonth.of(2026, 9), 0)
        assertEquals(0L, stopped.goalFor(YearMonth.of(2026, 10))?.amountCents)
    }

    @Test
    fun `the next goal is the first one starting after the month`() {
        assertEquals(march, goals.nextGoalAfter(YearMonth.of(2026, 2)))
        assertEquals(june, goals.nextGoalAfter(YearMonth.of(2026, 3)))
        assertNull(goals.nextGoalAfter(YearMonth.of(2026, 6)))
    }

    @Test
    fun `goal cannot be negative`() {
        assertThrows<IllegalArgumentException> { InvestmentGoal(YearMonth.of(2026, 3), -1) }
    }
}
