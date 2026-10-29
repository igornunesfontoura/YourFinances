package io.github.igornunesfontoura.yourfinances.domain.savings

import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.SavingsPocket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class SavingsTest {
    private val today = LocalDate.of(2026, 1, 15)
    private val reserve = SavingsPocket(id = 1, name = "Reserva", targetCents = 1000000)
    private val trip = SavingsPocket(id = 2, name = "Viagem")

    private fun entry(
        type: EntryType,
        amount: Long,
        on: LocalDate,
        pocket: Long? = null,
        status: EntryStatus = EntryStatus.CONFIRMED,
    ) = Entry(type = type, amountCents = amount, description = "", occurredOn = on, paidOn = on, status = status, savingsPocketId = pocket)

    @Test
    fun `contributions minus withdrawals, confirmed only`() {
        val entries = listOf(
            entry(EntryType.INVESTMENT, 100000, today),
            entry(EntryType.WITHDRAWAL, 30000, today),
            entry(EntryType.INVESTMENT, 50000, today, status = EntryStatus.PREDICTED),
            entry(EntryType.INCOME, 999999, today),
            entry(EntryType.FLEXIBLE_EXPENSE, 999, today),
        )
        assertEquals(70000L, netSavedCents(entries))
    }

    @Test
    fun `balances per pocket, then the money without a pocket`() {
        val entries = listOf(
            entry(EntryType.INVESTMENT, 300000, LocalDate.of(2025, 12, 5), pocket = 1),
            entry(EntryType.INVESTMENT, 200000, LocalDate.of(2026, 1, 5), pocket = 1),
            entry(EntryType.WITHDRAWAL, 50000, LocalDate.of(2026, 1, 10), pocket = 1),
            entry(EntryType.INVESTMENT, 10000, LocalDate.of(2026, 1, 10)),
        )
        val summary = savingsSummary(entries, listOf(reserve, trip), today, monthCount = 3)

        assertEquals(460000L, summary.totalCents)
        assertEquals(
            listOf(PocketBalance(reserve, 450000), PocketBalance(trip, 0), PocketBalance(null, 10000)),
            summary.pockets,
        )
        assertEquals(0.45f, summary.pockets[0].progress)
        assertNull(summary.pockets[1].progress)
        assertEquals(
            listOf(
                MonthSaved(YearMonth.of(2026, 1), 160000),
                MonthSaved(YearMonth.of(2025, 12), 300000),
                MonthSaved(YearMonth.of(2025, 11), 0),
            ),
            summary.months,
        )
    }

    @Test
    fun `future entries have not happened yet`() {
        val entries = listOf(
            entry(EntryType.INVESTMENT, 100000, today),
            entry(EntryType.INVESTMENT, 100000, today.plusDays(1)),
        )
        assertEquals(100000L, savingsSummary(entries, emptyList(), today).totalCents)
    }

    @Test
    fun `no "sem caixinha" row when nothing is there, and progress is capped`() {
        val entries = listOf(entry(EntryType.INVESTMENT, 2000000, today, pocket = 1))
        val summary = savingsSummary(entries, listOf(reserve), today)
        assertEquals(listOf(PocketBalance(reserve, 2000000)), summary.pockets)
        assertEquals(1f, summary.pockets.single().progress)
        assertEquals(12, summary.months.size)
    }

    @Test
    fun `withdrawing more than recorded goes negative and shows no progress`() {
        val entries = listOf(entry(EntryType.WITHDRAWAL, 5000, today, pocket = 1))
        val balance = savingsSummary(entries, listOf(reserve), today).pockets.single()
        assertEquals(-5000L, balance.savedCents)
        assertEquals(0f, balance.progress)
    }

    @Test
    fun `saved in a month uses only what already happened`() {
        val entries = listOf(
            entry(EntryType.INVESTMENT, 100000, LocalDate.of(2026, 1, 5)),
            entry(EntryType.INVESTMENT, 50000, LocalDate.of(2026, 1, 20)),
            entry(EntryType.WITHDRAWAL, 10000, LocalDate.of(2026, 1, 10)),
            entry(EntryType.INVESTMENT, 70000, LocalDate.of(2025, 12, 5)),
        )
        assertEquals(90000L, savedInMonthCents(entries, YearMonth.of(2026, 1), today))
        assertEquals(70000L, savedInMonthCents(entries, YearMonth.of(2025, 12), today))
        assertEquals(0L, savedInMonthCents(entries, YearMonth.of(2026, 2), today))
    }

    @Test
    fun `a pocket needs a name`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { SavingsPocket(name = " ") }
    }
}
