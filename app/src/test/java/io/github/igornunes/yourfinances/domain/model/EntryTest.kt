package io.github.igornunes.yourfinances.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class EntryTest {
    private val date = LocalDate.of(2026, 3, 1)

    private fun entry(
        amount: Long = 100,
        recurrenceId: Long? = null,
        scheduledFor: LocalDate? = null,
        purchaseId: Long? = null,
        number: Int? = null,
        count: Int? = null,
    ) = Entry(
        type = EntryType.FIXED_EXPENSE,
        amountCents = amount,
        description = "x",
        occurredOn = date,
        paidOn = date,
        recurrenceId = recurrenceId,
        scheduledFor = scheduledFor,
        installmentPurchaseId = purchaseId,
        installmentNumber = number,
        installmentCount = count,
    )

    @Test
    fun `valid entries are accepted`() {
        entry()
        entry(recurrenceId = 1, scheduledFor = date)
        entry(purchaseId = 1, number = 3, count = 3)
    }

    @Test
    fun `negative amount is rejected`() {
        assertThrows<IllegalArgumentException> { entry(amount = -1) }
    }

    @Test
    fun `recurrence link needs its slot`() {
        assertThrows<IllegalArgumentException> { entry(recurrenceId = 1) }
        assertThrows<IllegalArgumentException> { entry(scheduledFor = date) }
    }

    @Test
    fun `installment fields go together and stay in range`() {
        assertThrows<IllegalArgumentException> { entry(purchaseId = 1, number = 1) }
        assertThrows<IllegalArgumentException> { entry(purchaseId = 1, number = 0, count = 3) }
        assertThrows<IllegalArgumentException> { entry(purchaseId = 1, number = 4, count = 3) }
    }
}
