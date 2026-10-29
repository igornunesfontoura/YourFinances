package io.github.igornunes.yourfinances.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.igornunes.yourfinances.data.backup.BackupCrypto
import io.github.igornunes.yourfinances.data.backup.BackupFormat
import io.github.igornunes.yourfinances.data.backup.BackupTables
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class BackupDaoTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private val march5 = LocalDate.of(2026, 3, 5)

    private fun entry(id: Long, cardId: Long?, purchaseId: Long? = null, recurrenceId: Long? = null) = EntryEntity(
        id = id, type = EntryType.FIXED_EXPENSE, amountCents = 1000 * id, description = "e$id", occurredOn = march5,
        paidOn = march5, status = EntryStatus.CONFIRMED, categoryId = 1, creditCardId = cardId,
        recurrenceId = recurrenceId, scheduledFor = recurrenceId?.let { march5 }, installmentPurchaseId = purchaseId,
        installmentNumber = purchaseId?.let { 1 }, installmentCount = purchaseId?.let { 2 },
    )

    private val tables = BackupTables(
        categories = listOf(CategoryEntity(1, "Casa", EntryType.FIXED_EXPENSE)),
        creditCards = listOf(CreditCardEntity(2, "Nubank", 25, 5)),
        recurrences = listOf(RecurrenceEntity(3, EntryType.FIXED_EXPENSE, 5990, "Internet", Frequency.MONTHLY, 5, march5, null, 1)),
        recurrenceSkips = listOf(RecurrenceSkipEntity(3, LocalDate.of(2026, 4, 5))),
        installmentPurchases = listOf(InstallmentPurchaseEntity(4, "TV", 2000, 2, march5, EntryType.FIXED_EXPENSE, 1, 2)),
        entries = listOf(entry(10, cardId = 2, purchaseId = 4), entry(11, cardId = null, recurrenceId = 3), entry(12, cardId = null)),
        invoicePayments = listOf(InvoicePaymentEntity(2, LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 1))),
        investments = listOf(InvestmentEntity(5, "CDB", Indexer.CDI, BigDecimal("110.5"), march5, 100000)),
        investmentGoals = listOf(InvestmentGoalEntity(YearMonth.of(2026, 3), 100000)),
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun backupRestoresIntoAnotherDatabaseReplacingWhatWasThere() = runTest {
        source.backupDao().replaceAll(tables)
        target.backupDao().replaceAll(tables.copy(entries = listOf(entry(99, cardId = 2)), investmentGoals = emptyList()))

        val password = "segredo123".toCharArray()
        val file = BackupCrypto.encrypt(BackupFormat.encode(source.backupDao().readAll(), march5), password, iterations = 1_000)
        target.backupDao().replaceAll(BackupFormat.decode(BackupCrypto.decrypt(file, password)))

        assertEquals(tables, target.backupDao().readAll())
        // New rows never reuse a restored id.
        assertTrue(target.entryDao().insert(entry(0, cardId = null)) > 12L)
    }

    @Test
    fun brokenReferenceRollsEverythingBack() = runTest {
        target.backupDao().replaceAll(tables)
        val dangling = tables.copy(entries = tables.entries + entry(20, cardId = 404))

        val failed = runCatching { target.backupDao().replaceAll(dangling) }

        assertTrue(failed.isFailure)
        assertEquals(tables, target.backupDao().readAll())
    }
}
