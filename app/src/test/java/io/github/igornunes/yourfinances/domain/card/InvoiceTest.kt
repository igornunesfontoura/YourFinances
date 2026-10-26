package io.github.igornunes.yourfinances.domain.card

import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class InvoiceTest {
    // Closes on the 3rd, due on the 10th of the same month.
    private val sameMonthDue = CreditCard(id = 1, name = "A", closingDay = 3, dueDay = 10)

    // Closes on the 25th, due on the 5th of the following month.
    private val nextMonthDue = CreditCard(id = 2, name = "B", closingDay = 25, dueDay = 5)

    @Test
    fun `purchase before closing goes to the current invoice`() {
        val invoice = nextMonthDue.invoiceFor(LocalDate.of(2026, 3, 10))
        assertEquals(YearMonth.of(2026, 3), invoice.closingMonth)
        assertEquals(LocalDate.of(2026, 3, 25), invoice.closingDate)
        assertEquals(LocalDate.of(2026, 4, 5), invoice.dueDate)
    }

    @Test
    fun `purchase on the closing day stays in the current invoice`() {
        val invoice = nextMonthDue.invoiceFor(LocalDate.of(2026, 3, 25))
        assertEquals(YearMonth.of(2026, 3), invoice.closingMonth)
    }

    @Test
    fun `purchase after closing goes to the next invoice`() {
        val invoice = nextMonthDue.invoiceFor(LocalDate.of(2026, 3, 26))
        assertEquals(YearMonth.of(2026, 4), invoice.closingMonth)
        assertEquals(LocalDate.of(2026, 5, 5), invoice.dueDate)
    }

    @Test
    fun `due day after closing day is due in the closing month`() {
        val invoice = sameMonthDue.invoiceFor(LocalDate.of(2026, 6, 2))
        assertEquals(LocalDate.of(2026, 6, 3), invoice.closingDate)
        assertEquals(LocalDate.of(2026, 6, 10), invoice.dueDate)
    }

    @Test
    fun `purchase after December closing goes to January of the next year`() {
        val invoice = nextMonthDue.invoiceFor(LocalDate.of(2026, 12, 28))
        assertEquals(YearMonth.of(2027, 1), invoice.closingMonth)
        assertEquals(LocalDate.of(2027, 1, 25), invoice.closingDate)
        assertEquals(LocalDate.of(2027, 2, 5), invoice.dueDate)
    }

    @Test
    fun `December invoice is due in January of the next year`() {
        val invoice = nextMonthDue.invoiceFor(LocalDate.of(2026, 12, 20))
        assertEquals(LocalDate.of(2027, 1, 5), invoice.dueDate)
    }

    @Test
    fun `period starts the day after the previous closing, across the year`() {
        val invoice = nextMonthDue.invoiceFor(LocalDate.of(2027, 1, 10))
        assertEquals(LocalDate.of(2026, 12, 26), invoice.periodStart)
        assertEquals(LocalDate.of(2027, 1, 25), invoice.closingDate)
    }

    @Test
    fun `closing day 31 uses the last day of short months`() {
        val card = CreditCard(id = 3, name = "C", closingDay = 31, dueDay = 8)
        val february = card.invoiceFor(LocalDate.of(2026, 2, 28))
        assertEquals(LocalDate.of(2026, 2, 28), february.closingDate)
        assertEquals(LocalDate.of(2026, 2, 1), february.periodStart)
        assertEquals(LocalDate.of(2026, 3, 8), february.dueDate)

        val march = card.invoiceFor(LocalDate.of(2026, 3, 1))
        assertEquals(LocalDate.of(2026, 3, 1), march.periodStart)
        assertEquals(LocalDate.of(2026, 3, 31), march.closingDate)
    }

    @Test
    fun `leap year February closes on the 29th`() {
        val card = CreditCard(id = 3, name = "C", closingDay = 30, dueDay = 10)
        val invoice = card.invoiceFor(LocalDate.of(2028, 2, 29))
        assertEquals(LocalDate.of(2028, 2, 29), invoice.closingDate)
        assertEquals(LocalDate.of(2028, 3, 10), invoice.dueDate)
    }

    @Test
    fun `consecutive due dates cross the year`() {
        val dates = nextMonthDue.dueDatesFrom(LocalDate.of(2026, 11, 26), 3)
        assertEquals(
            listOf(LocalDate.of(2027, 1, 5), LocalDate.of(2027, 2, 5), LocalDate.of(2027, 3, 5)),
            dates,
        )
    }

    @Test
    fun `due day 31 is clamped every month without drifting`() {
        val card = CreditCard(id = 4, name = "D", closingDay = 20, dueDay = 31)
        val dates = card.dueDatesFrom(LocalDate.of(2026, 1, 5), 3)
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
            dates,
        )
    }

    @Test
    fun `invoice total sums only this card's entries due on the invoice date`() {
        val invoice = nextMonthDue.invoiceFor(LocalDate.of(2026, 3, 10))
        fun entry(amount: Long, cardId: Long?, paidOn: LocalDate) = Entry(
            type = EntryType.FLEXIBLE_EXPENSE,
            amountCents = amount,
            description = "x",
            occurredOn = LocalDate.of(2026, 3, 10),
            paidOn = paidOn,
            creditCardId = cardId,
        )
        val entries = listOf(
            entry(1000, 2, invoice.dueDate),
            entry(2550, 2, invoice.dueDate),
            entry(9999, 1, invoice.dueDate),
            entry(7777, 2, invoice.dueDate.plusMonths(1)),
            entry(5555, null, invoice.dueDate),
        )
        assertEquals(3550, nextMonthDue.invoiceTotalCents(invoice, entries))
    }

    @Test
    fun `nominal days decide the due month, even when February clamps both to the same day`() {
        // Comparing clamped dates would push February's due date to March 31 and collide with
        // the March invoice. Each invoice keeps its own due month instead.
        val card = CreditCard(id = 5, name = "E", closingDay = 30, dueDay = 31)
        val dueDates = card.dueDatesFrom(LocalDate.of(2026, 1, 10), 3)
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
            dueDates,
        )
        assertEquals(dueDates.size, dueDates.toSet().size)
    }
}
