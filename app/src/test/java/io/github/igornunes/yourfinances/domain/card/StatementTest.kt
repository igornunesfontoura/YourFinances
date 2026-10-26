package io.github.igornunes.yourfinances.domain.card

import io.github.igornunes.yourfinances.domain.installment.cardPaymentDates
import io.github.igornunes.yourfinances.domain.installment.toEntries
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatementTest {
    // Closes on the 25th, due on the 5th of the next month.
    private val card = CreditCard(id = 1, name = "Card", closingDay = 25, dueDay = 5)

    private fun purchase(id: Long, amount: Long, on: LocalDate, cardId: Long = 1) = Entry(
        id = id,
        type = EntryType.FLEXIBLE_EXPENSE,
        amountCents = amount,
        description = "x",
        occurredOn = on,
        paidOn = card.paymentDateFor(on),
        creditCardId = cardId,
    )

    @Test
    fun `payment date and invoice by due date`() {
        assertEquals(LocalDate.of(2026, 4, 5), card.paymentDateFor(LocalDate.of(2026, 3, 25)))
        assertEquals(LocalDate.of(2026, 5, 5), card.paymentDateFor(LocalDate.of(2026, 3, 26)))
        val invoice = card.invoiceDueIn(LocalDate.of(2026, 4, 5))
        assertEquals(LocalDate.of(2026, 3, 25), invoice.closingDate)
        assertEquals(LocalDate.of(2026, 2, 26), invoice.periodStart)
    }

    @Test
    fun `invoices sum card entries by due date, newest first, with their status`() {
        val today = LocalDate.of(2026, 4, 10)
        val activity = CardActivity(
            card = card,
            entries = listOf(
                purchase(1, 1000, LocalDate.of(2026, 2, 10)), // due Mar 5: overdue
                purchase(2, 2000, LocalDate.of(2026, 3, 1)), // due Apr 5: paid
                purchase(3, 3000, LocalDate.of(2026, 3, 30)), // due May 5: open until Apr 25
                purchase(4, 500, LocalDate.of(2026, 4, 2)),
                purchase(5, 9999, LocalDate.of(2026, 4, 2), cardId = 2),
            ),
            payments = listOf(InvoicePayment(1, LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 4))),
        )
        val invoices = activity.invoices(today)
        assertEquals(
            listOf(LocalDate.of(2026, 5, 5), LocalDate.of(2026, 4, 5), LocalDate.of(2026, 3, 5)),
            invoices.map { it.dueDate },
        )
        assertEquals(listOf(3500L, 2000L, 1000L), invoices.map { it.totalCents })
        assertEquals(
            listOf(InvoiceStatus.OPEN, InvoiceStatus.PAID, InvoiceStatus.OVERDUE),
            invoices.map { it.status },
        )
        assertEquals(LocalDate.of(2026, 4, 4), invoices[1].paidOn)
        assertNull(invoices[0].paidOn)
    }

    @Test
    fun `closed invoice waits for payment until its due date`() {
        val activity = CardActivity(card, listOf(purchase(1, 1000, LocalDate.of(2026, 3, 20))), emptyList())
        val invoice = activity.invoices(LocalDate.of(2026, 3, 28)).first { it.dueDate == LocalDate.of(2026, 4, 5) }
        assertEquals(InvoiceStatus.CLOSED, invoice.status)
        assertEquals(listOf(invoice), activity.awaitingPayment(LocalDate.of(2026, 3, 28)))
    }

    @Test
    fun `current invoice is listed even without purchases`() {
        val current = CardActivity(card, emptyList(), emptyList()).currentInvoice(LocalDate.of(2026, 3, 26))
        assertEquals(LocalDate.of(2026, 5, 5), current.dueDate)
        assertEquals(0L, current.totalCents)
        assertTrue(current.entries.isEmpty())
        assertEquals(InvoiceStatus.OPEN, current.status)
    }

    @Test
    fun `invoice of a due date that no longer matches the card has no known cycle`() {
        // Paid on the 10th: the card's due day was 10 before it changed to 5.
        val orphan = purchase(1, 1000, LocalDate.of(2026, 1, 20)).copy(paidOn = LocalDate.of(2026, 2, 10))
        val invoice = CardActivity(card, listOf(orphan), emptyList()).invoices(LocalDate.of(2026, 2, 1))
            .first { it.dueDate == LocalDate.of(2026, 2, 10) }
        assertNull(invoice.periodStart)
        assertNull(invoice.closingDate)
        assertEquals(InvoiceStatus.CLOSED, invoice.status)
    }

    private fun onCard(c: CreditCard, id: Long, on: LocalDate) = Entry(
        id = id,
        type = EntryType.FLEXIBLE_EXPENSE,
        amountCents = 1000,
        description = "x",
        occurredOn = on,
        paidOn = c.paymentDateFor(on),
        creditCardId = c.id,
    )

    @Test
    fun `entries of closed invoices stay, open ones move`() {
        val today = LocalDate.of(2026, 3, 10)
        val closed = onCard(card, 1, LocalDate.of(2026, 2, 1)) // closed Feb 25, due Mar 5
        val open = onCard(card, 2, LocalDate.of(2026, 3, 8)) // closes Mar 25, due Apr 5
        val changed = card.copy(closingDay = 5, dueDay = 15)

        val moved = changed.rescheduled(card, listOf(closed, open), emptyList(), today)

        // Mar 8 is after the new closing (5th): invoice closing Apr 5, due Apr 15.
        assertEquals(listOf(open.copy(paidOn = LocalDate.of(2026, 4, 15))), moved)
    }

    @Test
    fun `closed invoice not yet due is not moved, not even into the past`() {
        val old = CreditCard(id = 1, name = "C", closingDay = 10, dueDay = 5)
        val entry = onCard(old, 1, LocalDate.of(2026, 4, 15)) // closed May 10, due Jun 5
        val moved = old.copy(dueDay = 20).rescheduled(old, listOf(entry), emptyList(), LocalDate.of(2026, 5, 25))
        assertTrue(moved.isEmpty())
    }

    @Test
    fun `entry never moves into an invoice that already closed`() {
        val today = LocalDate.of(2026, 3, 20)
        val entry = onCard(card, 1, LocalDate.of(2026, 3, 12)) // closes Mar 25 under the old days
        val changed = card.copy(closingDay = 15, dueDay = 25)
        // Under the new days Mar 12 belongs to the invoice closed on Mar 15: it goes to the open one.
        assertEquals(
            listOf(entry.copy(paidOn = LocalDate.of(2026, 4, 25))),
            changed.rescheduled(card, listOf(entry), emptyList(), today),
        )
    }

    private fun installmentsOf(c: CreditCard, purchasedOn: LocalDate, count: Int): Pair<InstallmentPurchase, List<Entry>> {
        val purchase = InstallmentPurchase(
            id = 7,
            description = "TV",
            totalCents = 3000,
            installmentCount = count,
            purchasedOn = purchasedOn,
            creditCardId = c.id,
        )
        return purchase to purchase.toEntries(purchase.cardPaymentDates(c)).mapIndexed { i, e -> e.copy(id = i + 1L) }
    }

    @Test
    fun `pending installments continue after the last one that stayed, no duplicate invoice`() {
        val old = CreditCard(id = 1, name = "C", closingDay = 1, dueDay = 20)
        val (tv, installments) = installmentsOf(old, LocalDate.of(2026, 3, 5), 3) // Apr 20, May 20, Jun 20
        val moved = old.copy(closingDay = 10).rescheduled(old, installments, listOf(tv), LocalDate.of(2026, 4, 25))
        // Restarting from the purchase would put #2 on Apr 20 next to #1; it stays on May 20.
        assertTrue(moved.isEmpty())
    }

    @Test
    fun `pending installments continue after the last one that stayed, no gap`() {
        val old = CreditCard(id = 1, name = "C", closingDay = 10, dueDay = 20)
        val (tv, installments) = installmentsOf(old, LocalDate.of(2026, 3, 5), 3) // Mar 20, Apr 20, May 20
        val moved = old.copy(closingDay = 1).rescheduled(old, installments, listOf(tv), LocalDate.of(2026, 4, 25))
        // Restarting from the purchase would push #3 to Jun 20, leaving May empty; it stays on May 20.
        assertTrue(moved.isEmpty())
    }

    @Test
    fun `rescheduled installments keep accruing on their new due date`() {
        val (tv, installments) = installmentsOf(card, LocalDate.of(2026, 3, 1), 3) // Apr 5, May 5, Jun 5
        val moved = card.copy(dueDay = 15).rescheduled(card, installments, listOf(tv), LocalDate.of(2026, 4, 6))

        // #1 (invoice closed Mar 25) stays; #2 and #3 follow in consecutive invoices.
        assertEquals(listOf(2L, 3L), moved.map { it.id })
        assertEquals(listOf(LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15)), moved.map { it.paidOn })
        assertEquals(moved.map { it.paidOn }, moved.map { it.occurredOn })
    }

    @Test
    fun `unchanged days move nothing`() {
        val entries = listOf(purchase(1, 1000, LocalDate.of(2026, 3, 8)))
        assertTrue(card.rescheduled(card, entries, emptyList(), LocalDate.of(2026, 3, 1)).isEmpty())
    }
}
