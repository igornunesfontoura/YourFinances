package io.github.igornunesfontoura.yourfinances.data.backup

import io.github.igornunesfontoura.yourfinances.domain.model.CategoryKind
import io.github.igornunesfontoura.yourfinances.data.db.entity.CategoryEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.CreditCardEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.EntryEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.InvestmentGoalEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.InvoicePaymentEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.RecurrenceSkipEntity
import io.github.igornunesfontoura.yourfinances.data.db.entity.SavingsPocketEntity
import io.github.igornunesfontoura.yourfinances.domain.model.EntryStatus
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        categories = listOf(CategoryEntity(1, "Lazer", CategoryKind.EXPENSE, 50000)),
        creditCards = listOf(CreditCardEntity(2, "Nubank", 25, 5)),
        recurrences = listOf(
            RecurrenceEntity(3, EntryType.FIXED_EXPENSE, 5990, "Internet", Frequency.MONTHLY, 31, march5, null, null),
        ),
        recurrenceSkips = listOf(RecurrenceSkipEntity(3, LocalDate.of(2026, 4, 30))),
        installmentPurchases = listOf(
            InstallmentPurchaseEntity(4, "TV", 300000, 3, march5, EntryType.FLEXIBLE_EXPENSE, 1, 2),
        ),
        entries = listOf(
            entry,
            entry.copy(
                id = 9, type = EntryType.INVESTMENT, categoryId = null, creditCardId = null, installmentPurchaseId = null,
                installmentNumber = null, installmentCount = null, savingsPocketId = 6,
            ),
        ),
        invoicePayments = listOf(InvoicePaymentEntity(2, LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 4))),
        investmentGoals = listOf(InvestmentGoalEntity(YearMonth.of(2026, 3), 100000)),
        savingsPockets = listOf(SavingsPocketEntity(6, "Reserva", 1000000), SavingsPocketEntity(8, "Viagem", null)),
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
            "\"format\":5",
            "\"exportedOn\":\"2026-10-28\"",
            "\"occurredOn\":\"2026-03-05\"",
            "\"amountCents\":4590",
            "\"startsIn\":\"2026-03\"",
            "\"savingsPocketId\":6",
            "\"targetCents\":null",
            "\"type\":\"EXPENSE\",\"monthlyLimitCents\":50000",
            "\"type\":\"FLEXIBLE_EXPENSE\"",
            "\"endsOn\":null",
        ).forEach { assertTrue(json.contains(it), it) }
    }

    @Test
    fun `newer formats are refused`() {
        assertThrows<BackupInvalidException> { decode(json().replace("\"format\":5", "\"format\":6")) }
    }

    @Test
    fun `rows breaking a domain rule are refused`() {
        val negative = tables.copy(entries = listOf(entry.copy(amountCents = -1)))
        assertThrows<BackupInvalidException> { decode(json(negative)) }
        val badGoal = json().replace("\"amountCents\":100000}", "\"amountCents\":-5}")
        assertThrows<BackupInvalidException> { decode(badGoal) }
    }

    @Test
    fun `a reference to a pocket missing from the file is refused`() {
        val dangling = tables.copy(savingsPockets = tables.savingsPockets.drop(1))
        assertThrows<BackupInvalidException> { decode(json(dangling)) }
    }

    @Test
    fun `references to cards and categories missing from the file are refused`() {
        assertThrows<BackupInvalidException> { decode(json(tables.copy(creditCards = emptyList()))) }
        assertThrows<BackupInvalidException> { decode(json(tables.copy(categories = emptyList()))) }
    }

    @Test
    fun `import keys survive the round trip`() {
        val imported = tables.copy(entries = tables.entries.map { it.copy(importKey = "id:F${it.id}") })
        assertEquals(imported, decode(json(imported)))
    }

    @Test
    fun `malformed content is refused`() {
        assertThrows<BackupInvalidException> { decode("not json") }
        assertThrows<BackupInvalidException> { decode(json().replace("2026-03-05", "2026-13-05")) }
        assertThrows<BackupInvalidException> { decode(json().replace("\"FLEXIBLE_EXPENSE\"", "\"GIFT\"")) }
        assertThrows<BackupInvalidException> { decode(json().replace("{\"format\":5,", "{\"format\":5,\"extra\":true,")) }
    }

    @Test
    fun `version 1 backups, made before pockets, still restore`() {
        val v1 = """{"format":1,"exportedOn":"2026-10-28","categories":[],"creditCards":[],"recurrences":[],""" +
            """"recurrenceSkips":[],"installmentPurchases":[],"entries":[{"id":1,"type":"INVESTMENT","amountCents":5000,""" +
            """"description":"","occurredOn":"2026-10-01","paidOn":"2026-10-01","status":"CONFIRMED","categoryId":null,""" +
            """"creditCardId":null,"recurrenceId":null,"scheduledFor":null,"installmentPurchaseId":null,""" +
            """"installmentNumber":null,"installmentCount":null}],"invoicePayments":[],"investments":[],""" +
            """"investmentGoals":[{"startsIn":"2026-10","amountCents":100000}]}"""
        val tables = decode(v1)
        assertEquals(5000L, tables.entries.single().amountCents)
        assertEquals(null, tables.entries.single().savingsPocketId)
        assertEquals(emptyList<SavingsPocketEntity>(), tables.savingsPockets)
        assertEquals(1, tables.investmentGoals.size)
    }

    @Test
    fun `version 2 categories, typed by entry type, read as their kind`() {
        val v2 = json().replace("\"format\":4", "\"format\":2")
            .replace("\"type\":\"EXPENSE\",\"monthlyLimitCents\":50000", "\"type\":\"FLEXIBLE_EXPENSE\"")
        assertEquals(CategoryKind.EXPENSE, decode(v2).categories.single().kind)
        assertThrows<BackupInvalidException> { decode(json().replace("\"type\":\"EXPENSE\"", "\"type\":\"GIFT\"")) }
    }

    @Test
    fun `twice monthly recurrences keep their second payment`() {
        val salary = RecurrenceEntity(
            10, EntryType.INCOME, 200000, "Salário", Frequency.TWICE_MONTHLY, 16, march5, null, null,
            secondDayOfMonth = 31, secondAmountCents = 300000,
        )
        val withSalary = tables.copy(recurrences = tables.recurrences + salary)
        assertEquals(withSalary, decode(json(withSalary)))
    }

    @Test
    fun `a twice monthly recurrence without its second day is refused`() {
        val broken = RecurrenceEntity(10, EntryType.INCOME, 200000, "Salário", Frequency.TWICE_MONTHLY, 16, march5, null, null)
        assertThrows<BackupInvalidException> { decode(json(tables.copy(recurrences = listOf(broken)))) }
    }

    @Test
    fun `version 4 backups, made before twice monthly recurrences, still restore`() {
        val v4 = json().replace("\"format\":5", "\"format\":4").replace(",\"secondDayOfMonth\":null,\"secondAmountCents\":null", "")
        assertTrue(!v4.contains("secondDayOfMonth"))
        assertEquals(tables, decode(v4))
    }
}
