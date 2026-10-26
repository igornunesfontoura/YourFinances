package io.github.igornunes.yourfinances.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.domain.installment.monthlyPaymentDates
import io.github.igornunes.yourfinances.domain.model.Category
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import io.github.igornunes.yourfinances.domain.model.Recurrence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class EntryDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: EntryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = EntryRepository(database)
    }

    @After
    fun tearDown() = database.close()

    private fun entry(occurredOn: LocalDate, paidOn: LocalDate = occurredOn, categoryId: Long? = null) = Entry(
        type = EntryType.FLEXIBLE_EXPENSE,
        amountCents = 1000,
        description = "x",
        occurredOn = occurredOn,
        paidOn = paidOn,
        categoryId = categoryId,
    )

    @Test
    fun observesMonthByCashOrAccrualDate() = runTest {
        repository.add(entry(LocalDate.of(2026, 3, 28), paidOn = LocalDate.of(2026, 4, 5)))
        repository.add(entry(LocalDate.of(2026, 4, 1)))

        val march = YearMonth.of(2026, 3)
        repository.observeMonth(march, DateBasis.ACCRUAL).test {
            assertEquals(1, awaitItem().size)
        }
        repository.observeMonth(march, DateBasis.CASH).test {
            assertEquals(0, awaitItem().size)
        }
        repository.observeMonth(YearMonth.of(2026, 4), DateBasis.CASH).test {
            assertEquals(2, awaitItem().size)
        }
    }

    @Test
    fun installmentPurchaseIsStoredWithItsInstallments() = runTest {
        val purchase = InstallmentPurchase(
            description = "TV",
            totalCents = 100000,
            installmentCount = 3,
            purchasedOn = LocalDate.of(2026, 1, 20),
        )
        val purchaseId = repository.addInstallmentPurchase(purchase, monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))

        database.entryDao().observeInstallments(purchaseId).test {
            val installments = awaitItem().map { it.toDomain() }
            assertEquals(listOf(33334L, 33333L, 33333L), installments.map { it.amountCents })
            assertEquals(listOf(1, 2, 3), installments.map { it.installmentNumber })
        }
    }

    @Test
    fun deletingPurchaseDeletesItsInstallments() = runTest {
        val purchase = InstallmentPurchase(
            description = "TV",
            totalCents = 3000,
            installmentCount = 3,
            purchasedOn = LocalDate.of(2026, 1, 20),
        )
        val purchaseId = repository.addInstallmentPurchase(purchase, monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))
        val dao = database.installmentPurchaseDao()
        dao.delete(dao.getById(purchaseId)!!)

        database.entryDao().observeInstallments(purchaseId).test {
            assertEquals(0, awaitItem().size)
        }
    }

    @Test
    fun recurrenceGenerationIsIdempotent() = runTest {
        val recurrence = Recurrence(
            type = EntryType.FIXED_EXPENSE,
            amountCents = 4990,
            description = "Streaming",
            frequency = Frequency.MONTHLY,
            dayOfMonth = 31,
            startsOn = LocalDate.of(2026, 1, 1),
        )
        val id = database.recurrenceDao().insert(recurrence.toEntity())
        val stored = recurrence.copy(id = id)
        val from = LocalDate.of(2026, 1, 1)
        val to = LocalDate.of(2026, 3, 31)

        assertEquals(3, repository.generatePredicted(stored, from, to))
        assertEquals(0, repository.generatePredicted(stored, from, to))
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
            database.entryDao().scheduledDates(id).sorted(),
        )
    }

    @Test
    fun deletingCategoryKeepsEntriesWithoutCategory() = runTest {
        val categoryId = database.categoryDao().insert(Category(name = "Lazer", type = EntryType.FLEXIBLE_EXPENSE).toEntity())
        val entryId = repository.add(entry(LocalDate.of(2026, 3, 1), categoryId = categoryId))

        database.categoryDao().delete(Category(id = categoryId, name = "Lazer", type = EntryType.FLEXIBLE_EXPENSE).toEntity())

        assertNull(database.entryDao().getById(entryId)!!.categoryId)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun entryWithUnknownCardIsRejected() = runTest {
        database.entryDao().insert(entry(LocalDate.of(2026, 3, 1)).copy(creditCardId = 999).toEntity())
    }

    @Test
    fun deletedPredictedEntryIsNotGeneratedAgain() = runTest {
        val recurrence = Recurrence(
            type = EntryType.FIXED_EXPENSE,
            amountCents = 4990,
            description = "Streaming",
            frequency = Frequency.MONTHLY,
            dayOfMonth = 5,
            startsOn = LocalDate.of(2026, 1, 1),
        )
        val stored = recurrence.copy(id = database.recurrenceDao().insert(recurrence.toEntity()))
        val from = LocalDate.of(2026, 1, 1)
        val to = LocalDate.of(2026, 2, 28)
        repository.generatePredicted(stored, from, to)

        val january = repository.observeMonth(YearMonth.of(2026, 1), DateBasis.CASH).first().single()
        repository.delete(january)

        assertEquals(0, repository.generatePredicted(stored, from, to))
        assertEquals(0, repository.observeMonth(YearMonth.of(2026, 1), DateBasis.CASH).first().size)
    }

    @Test
    fun cardPurchaseInstallmentsFollowInvoiceDueDates() = runTest {
        val card = CreditCard(name = "Card", closingDay = 25, dueDay = 5)
        val stored = card.copy(id = database.creditCardDao().insert(card.toEntity()))
        val purchase = InstallmentPurchase(
            description = "Phone",
            totalCents = 3000,
            installmentCount = 2,
            purchasedOn = LocalDate.of(2026, 12, 26),
            creditCardId = stored.id,
        )
        val purchaseId = repository.addCardInstallmentPurchase(purchase, stored)

        val installments = database.entryDao().observeInstallments(purchaseId).first().map { it.toDomain() }
        assertEquals(listOf(LocalDate.of(2027, 2, 5), LocalDate.of(2027, 3, 5)), installments.map { it.paidOn })
    }
}
