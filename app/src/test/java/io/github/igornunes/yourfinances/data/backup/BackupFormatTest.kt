package io.github.igornunes.yourfinances.data.backup

import io.github.igornunes.yourfinances.data.db.entity.CategoryEntity
import io.github.igornunes.yourfinances.data.db.entity.CreditCardEntity
import io.github.igornunes.yourfinances.data.db.entity.EntryEntity
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentGoalEntity
import io.github.igornunes.yourfinances.data.db.entity.InvoicePaymentEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceSkipEntity
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Indexer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BackupFormatTest {
    private val march5 = LocalDate.of(2026, 3, 5)

    private val entry = EntryEntity(
        id = 7, type = EntryType.FLEXIBLE_EXPENSE, amountCents = 4590, description = "Jantar \"especial\"",
        occurredOn = march5, paidOn = LocalDate.of(2026, 4, 5), status = EntryStatus.CONFIRMED, categoryId = 1,
        creditCardId = 2, recurrenceId = null, scheduledFor = null, installmentPurchaseId = 4, installmentNumber = 1,
        installmentCount = 3,
    )

    private val tables = BackupTables(
        categories = listOf(CategoryEntity(1, "Lazer", EntryType.FLEXIBLE_EXPENSE)),
        creditCards = listOf(CreditCardEntity(2, "Nubank", 25, 5)),
        recurrences = listOf(
            RecurrenceEntity(3, EntryType.FIXED_EXPENSE, 5990, "Internet", Frequency.MONTHLY, 31, march5, null, null),
        ),
        recurrenceSkips = listOf(RecurrenceSkipEntity(3, LocalDate.of(2026, 4, 30))),
        installmentPurchases = listOf(
            InstallmentPurchaseEntity(4, "TV", 300000, 3, march5, EntryType.FLEXIBLE_EXPENSE, 1, 2),
        ),
        entries = listOf(entry),
        invoicePayments = listOf(InvoicePaymentEntity(2, LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 4))),
        investments = listOf(InvestmentEntity(5, "CDB", Indexer.CDI, BigDecimal("110.50"), march5, 100000)),
        investmentGoals = listOf(InvestmentGoalEntity(YearMonth.of(2026, 3), 100000)),
    )

    private fun json(tables: BackupTables = this.tables) =
        BackupFormat.encode(tables, LocalDate.of(2026, 10, 28)).toString(Charsets.UTF_8)

    private fun decode(json: String) = BackupFormat.decode(json.toByteArray())

    @Test
    fun `every table survives the round trip`() {
        assertEquals(tables, decode(json()))
    }

    @Test
    fun `the format is plain and stable`() {
        val json = json()
        listOf(
            "\"format\":1",
            "\"exportedOn\":\"2026-10-28\"",
            "\"occurredOn\":\"2026-03-05\"",
            "\"amountCents\":4590",
            "\"startsIn\":\"2026-03\"",
            "\"rate\":\"110.50\"",
            "\"type\":\"FLEXIBLE_EXPENSE\"",
            "\"endsOn\":null",
        ).forEach { assertTrue(json.contains(it), it) }
    }

    @Test
    fun `newer formats are refused`() {
        assertThrows<BackupInvalidException> { decode(json().replace("\"format\":1", "\"format\":2")) }
    }

    @Test
    fun `rows breaking a domain rule are refused`() {
        val negative = tables.copy(entries = listOf(entry.copy(amountCents = -1)))
        assertThrows<BackupInvalidException> { decode(json(negative)) }
        val badGoal = json().replace("\"amountCents\":100000}", "\"amountCents\":-5}")
        assertThrows<BackupInvalidException> { decode(badGoal) }
    }

    @Test
    fun `malformed content is refused`() {
        assertThrows<BackupInvalidException> { decode("not json") }
        assertThrows<BackupInvalidException> { decode(json().replace("2026-03-05", "2026-13-05")) }
        assertThrows<BackupInvalidException> { decode(json().replace("\"FLEXIBLE_EXPENSE\"", "\"GIFT\"")) }
        assertThrows<BackupInvalidException> { decode(json().replace("{\"format\":1,", "{\"format\":1,\"extra\":true,")) }
    }
}
