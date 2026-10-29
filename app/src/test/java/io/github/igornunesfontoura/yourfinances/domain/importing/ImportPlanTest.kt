package io.github.igornunesfontoura.yourfinances.domain.importing

import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ImportPlanTest {
    private val march5 = LocalDate.of(2026, 3, 5)
    private val coffee = StatementLine(march5, -800, "Café")
    private val salary = StatementLine(march5, 500000, "Salário", externalId = "F1")
    private val card = CreditCard(id = 3, name = "Nubank", closingDay = 25, dueDay = 5)

    @Test
    fun `keys use the bank id or number identical lines apart`() {
        val keys = importKeys(listOf(coffee, coffee, salary))
        assertEquals("id:F1", keys[2])
        assertNotEquals(keys[0], keys[1])
        assertEquals(keys, importKeys(listOf(coffee, coffee, salary)))
    }

    @Test
    fun `money out is a flexible expense, money in is income`() {
        val plan = planImport(listOf(coffee, salary), emptySet(), emptyList(), card = null)
        assertEquals(listOf(EntryType.FLEXIBLE_EXPENSE, EntryType.INCOME), plan.map { it.type })
        assertEquals(listOf(true, true), plan.map { it.selected })
    }

    @Test
    fun `into a card, money in starts unselected and expenses go to its invoice`() {
        val plan = planImport(listOf(coffee, salary), emptySet(), emptyList(), card)
        assertEquals(listOf(true, false), plan.map { it.selected })
        val entry = plan[0].toEntry(card)
        assertEquals(3L, entry.creditCardId)
        assertEquals(LocalDate.of(2026, 4, 5), entry.paidOn)
        assertEquals(800L, entry.amountCents)
        assertEquals(EntryStatus.CONFIRMED, entry.status)
        assertEquals(plan[0].key, entry.importKey)
        // Income never goes on a card, even when imported from its statement.
        assertEquals(null, plan[1].copy(selected = true).toEntry(card).creditCardId)
    }

    @Test
    fun `already imported and possible duplicates start unselected`() {
        val keys = importKeys(listOf(coffee, coffee, salary))
        val typedByHand = Entry(type = EntryType.FLEXIBLE_EXPENSE, amountCents = 800, description = "cafezinho", occurredOn = march5, paidOn = march5)
        val plan = planImport(listOf(coffee, coffee, salary), setOf(keys[2]), listOf(typedByHand), card = null)
        assertEquals(listOf(DuplicateKind.POSSIBLE, null, DuplicateKind.ALREADY_IMPORTED), plan.map { it.duplicate })
        assertEquals(listOf(false, true, false), plan.map { it.selected })
    }

    @Test
    fun `a line from the same file is already imported, not a possible duplicate`() {
        val imported = plan(coffee).toEntry(null)
        val again = planImport(listOf(coffee), setOf(imported.importKey!!), listOf(imported), card = null).single()
        assertEquals(DuplicateKind.ALREADY_IMPORTED, again.duplicate)
    }

    @Test
    fun `balances and card bill payments start unselected`() {
        val lines = listOf(
            StatementLine(march5, 120000, "SALDO ANTERIOR"),
            StatementLine(march5, -80000, "PAGTO FATURA NUBANK"),
            StatementLine(march5, -80000, "Pagamento de boleto"),
        )
        val plan = planImport(lines, emptySet(), emptyList(), card = null)
        assertEquals(listOf(ImportHint.BALANCE, ImportHint.CARD_BILL_PAYMENT, null), plan.map { it.hint })
        assertEquals(listOf(false, false, true), plan.map { it.selected })
    }

    @Test
    fun `entries imported from another file count as possible duplicates`() {
        val fromOfx = coffee.copy(externalId = "F9")
        val imported = plan(fromOfx).toEntry(null)
        val again = planImport(listOf(coffee), emptySet(), listOf(imported), card = null).single()
        assertEquals(DuplicateKind.POSSIBLE, again.duplicate)
    }

    private fun plan(line: StatementLine) = planImport(listOf(line), emptySet(), emptyList(), card = null).single()
}
