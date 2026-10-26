package io.github.igornunes.yourfinances.domain.entry

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EntryEditsTest {
    private val march10 = LocalDate.of(2026, 3, 10)
    private val march12 = LocalDate.of(2026, 3, 12)

    @Test
    fun `new entry is confirmed and paid when it happened`() {
        val entry = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "  Cinema ", march10)
        assertEquals(EntryStatus.CONFIRMED, entry.status)
        assertEquals(march10, entry.paidOn)
        assertEquals("Cinema", entry.description)
    }

    @Test
    fun `editing a plain entry moves both dates`() {
        val edited = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "Cinema", march10)
            .edited(EntryType.FLEXIBLE_EXPENSE, 5000, "Cinema", march12)
        assertEquals(march12, edited.occurredOn)
        assertEquals(march12, edited.paidOn)
        assertEquals(5000L, edited.amountCents)
    }

    @Test
    fun `editing confirms a predicted entry`() {
        val predicted = Entry(
            type = EntryType.FIXED_EXPENSE,
            amountCents = 15000,
            description = "Luz",
            occurredOn = march10,
            paidOn = march10,
            status = EntryStatus.PREDICTED,
            recurrenceId = 2,
            scheduledFor = march10,
        )
        val edited = predicted.edited(EntryType.FIXED_EXPENSE, 16234, "Luz", march10)
        assertEquals(EntryStatus.CONFIRMED, edited.status)
        assertEquals(16234L, edited.amountCents)
        assertEquals(march10, edited.scheduledFor)
    }

    @Test
    fun `card entry only takes the new description`() {
        val onCard = Entry(
            type = EntryType.FLEXIBLE_EXPENSE,
            amountCents = 4500,
            description = "Jantar",
            occurredOn = march10,
            paidOn = LocalDate.of(2026, 4, 5),
            creditCardId = 1,
        )
        assertFalse(onCard.isQuickEditable)
        assertTrue(onCard.isQuickDeletable)
        val edited = onCard.edited(EntryType.INCOME, 9999, "Jantar fora", march12)
        assertEquals(onCard.copy(description = "Jantar fora"), edited)
    }

    @Test
    fun `installment keeps amount, type and dates so the purchase still adds up`() {
        val installment = Entry(
            type = EntryType.FIXED_EXPENSE,
            amountCents = 1000,
            description = "TV",
            occurredOn = march10,
            paidOn = LocalDate.of(2026, 5, 10),
            installmentPurchaseId = 3,
            installmentNumber = 3,
            installmentCount = 5,
        )
        assertFalse(installment.isQuickEditable)
        assertFalse(installment.isQuickDeletable)
        val edited = installment.edited(EntryType.FLEXIBLE_EXPENSE, 1200, "TV sala", march12)
        assertEquals(installment.copy(description = "TV sala"), edited)
    }
}
