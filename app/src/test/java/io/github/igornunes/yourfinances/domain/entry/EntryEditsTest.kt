package io.github.igornunes.yourfinances.domain.entry

import io.github.igornunes.yourfinances.domain.model.CreditCard
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

class EntryEditsTest {
    private val march10 = LocalDate.of(2026, 3, 10)
    private val march12 = LocalDate.of(2026, 3, 12)

    @Test
    fun `new entry is confirmed and paid when it happened`() {
        val entry = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "  Cinema ", march10, card = null)
        assertEquals(EntryStatus.CONFIRMED, entry.status)
        assertEquals(march10, entry.paidOn)
        assertEquals("Cinema", entry.description)
    }

    @Test
    fun `editing a plain entry moves both dates`() {
        val edited = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "Cinema", march10, card = null)
            .edited(EntryType.FLEXIBLE_EXPENSE, 5000, "Cinema", march12, card = null)
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
        val edited = predicted.edited(EntryType.FIXED_EXPENSE, 16234, "Luz", march10, card = null)
        assertEquals(EntryStatus.CONFIRMED, edited.status)
        assertEquals(16234L, edited.amountCents)
        assertEquals(march10, edited.scheduledFor)
    }

    private val card = CreditCard(id = 1, name = "Card", closingDay = 10, dueDay = 20)

    @Test
    fun `card purchase is paid on the due date of its invoice`() {
        val beforeClosing = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "Jantar", march10, card)
        assertEquals(1L, beforeClosing.creditCardId)
        assertEquals(LocalDate.of(2026, 3, 20), beforeClosing.paidOn)

        val afterClosing = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "Jantar", march12, card)
        assertEquals(LocalDate.of(2026, 4, 20), afterClosing.paidOn)
    }

    @Test
    fun `editing a card entry moves it to the invoice of its new date`() {
        val onCard = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "Jantar", march10, card)
        assertTrue(onCard.isQuickEditable)
        assertFalse(onCard.deletesWholePurchase)

        val moved = onCard.edited(EntryType.FLEXIBLE_EXPENSE, 4800, "Jantar", march12, card)
        assertEquals(march12, moved.occurredOn)
        assertEquals(LocalDate.of(2026, 4, 20), moved.paidOn)
        assertEquals(4800L, moved.amountCents)

        val offCard = onCard.edited(EntryType.FLEXIBLE_EXPENSE, 4500, "Jantar", march10, card = null)
        assertNull(offCard.creditCardId)
        assertEquals(march10, offCard.paidOn)
    }

    @Test
    fun `fixing the description keeps the invoice even after the card's days changed`() {
        val onCard = newEntry(EntryType.FLEXIBLE_EXPENSE, 4500, "Jnatar", march10, card) // due Mar 20
        val daysChanged = card.copy(dueDay = 28)
        val fixed = onCard.edited(EntryType.FLEXIBLE_EXPENSE, 4500, "Jantar", march10, daysChanged)
        assertEquals(LocalDate.of(2026, 3, 20), fixed.paidOn)
        assertEquals("Jantar", fixed.description)
    }

    @Test
    fun `only expenses go on a card`() {
        assertThrows<IllegalArgumentException> { newEntry(EntryType.INCOME, 100, "", march10, card) }
        assertThrows<IllegalArgumentException> { newEntry(EntryType.INVESTMENT, 100, "", march10, card) }
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
        assertTrue(installment.deletesWholePurchase)
        val edited = installment.edited(EntryType.FLEXIBLE_EXPENSE, 1200, "TV sala", march12, card = null)
        assertEquals(installment.copy(description = "TV sala"), edited)
    }

    @Test
    fun `confirming keeps the predicted values`() {
        val predicted = Entry(
            type = EntryType.FIXED_EXPENSE,
            amountCents = 4990,
            description = "Streaming",
            occurredOn = march10,
            paidOn = march10,
            status = EntryStatus.PREDICTED,
            recurrenceId = 1,
            scheduledFor = march10,
        )
        assertEquals(predicted.copy(status = EntryStatus.CONFIRMED), predicted.confirmed())
    }
}
