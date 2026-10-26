package io.github.igornunes.yourfinances.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.data.repository.RoomEntryRepository
import io.github.igornunes.yourfinances.data.repository.RoomInstallmentRepository
import io.github.igornunes.yourfinances.domain.installment.monthlyPaymentDates
import io.github.igornunes.yourfinances.domain.model.Category
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class EntryDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var entries: RoomEntryRepository
    private lateinit var installments: RoomInstallmentRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        entries = RoomEntryRepository(database)
        installments = RoomInstallmentRepository(database)
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

    private val tv = InstallmentPurchase(
        description = "TV",
        totalCents = 100000,
        installmentCount = 3,
        purchasedOn = LocalDate.of(2026, 1, 20),
    )

    @Test
    fun observesMonthByCashOrAccrualDate() = runTest {
        entries.add(entry(LocalDate.of(2026, 3, 28), paidOn = LocalDate.of(2026, 4, 5)))
        entries.add(entry(LocalDate.of(2026, 4, 1)))

        val march = YearMonth.of(2026, 3)
        entries.observeMonth(march, DateBasis.ACCRUAL).test { assertEquals(1, awaitItem().size) }
        entries.observeMonth(march, DateBasis.CASH).test { assertEquals(0, awaitItem().size) }
        entries.observeMonth(YearMonth.of(2026, 4), DateBasis.CASH).test { assertEquals(2, awaitItem().size) }
    }

    @Test
    fun installmentPurchaseIsStoredWithItsInstallments() = runTest {
        installments.add(tv, monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))

        val plan = installments.observePlans().first().single()
        assertEquals("TV", plan.purchase.description)
        assertEquals(listOf(33334L, 33333L, 33333L), plan.installments.map { it.amountCents })
        assertEquals(listOf(1, 2, 3), plan.installments.map { it.installmentNumber })
    }

    @Test
    fun deletingPurchaseDeletesItsInstallments() = runTest {
        val purchaseId = installments.add(tv, monthlyPaymentDates(LocalDate.of(2026, 2, 10), 3))
        installments.delete(purchaseId)

        assertTrue(installments.observePlans().first().isEmpty())
        assertEquals(0, entries.observeMonth(YearMonth.of(2026, 1), DateBasis.ACCRUAL).first().size)
    }

    @Test
    fun cardPurchaseInstallmentsFollowInvoiceDueDates() = runTest {
        val card = CreditCard(name = "Card", closingDay = 25, dueDay = 5)
        val stored = card.copy(id = database.creditCardDao().insert(card.toEntity()))
        val purchase = tv.copy(installmentCount = 2, purchasedOn = LocalDate.of(2026, 12, 26), creditCardId = stored.id)
        installments.addOnCard(purchase, stored)

        val plan = installments.observePlans().first().single()
        assertEquals(listOf(LocalDate.of(2027, 2, 5), LocalDate.of(2027, 3, 5)), plan.installments.map { it.paidOn })
    }

    @Test
    fun deletingCategoryKeepsEntriesWithoutCategory() = runTest {
        val category = Category(name = "Lazer", type = EntryType.FLEXIBLE_EXPENSE)
        val categoryId = database.categoryDao().insert(category.toEntity())
        val entryId = entries.add(entry(LocalDate.of(2026, 3, 1), categoryId = categoryId))

        database.categoryDao().delete(category.copy(id = categoryId).toEntity())

        assertNull(entries.getById(entryId)!!.categoryId)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun entryWithUnknownCardIsRejected() = runTest {
        entries.add(entry(LocalDate.of(2026, 3, 1)).copy(creditCardId = 999))
    }
}
