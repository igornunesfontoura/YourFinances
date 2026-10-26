package io.github.igornunes.yourfinances.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.igornunes.yourfinances.data.repository.RoomCreditCardRepository
import io.github.igornunes.yourfinances.data.repository.RoomEntryRepository
import io.github.igornunes.yourfinances.data.repository.RoomInstallmentRepository
import io.github.igornunes.yourfinances.domain.card.InvoicePayment
import io.github.igornunes.yourfinances.domain.card.InvoiceStatus
import io.github.igornunes.yourfinances.domain.card.invoices
import io.github.igornunes.yourfinances.domain.entry.newEntry
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CreditCardRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var cards: RoomCreditCardRepository
    private lateinit var entries: RoomEntryRepository
    private lateinit var installments: RoomInstallmentRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        cards = RoomCreditCardRepository(database)
        entries = RoomEntryRepository(database)
        installments = RoomInstallmentRepository(database)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun storedCard(): CreditCard {
        val card = CreditCard(name = "Nubank", closingDay = 25, dueDay = 5)
        return card.copy(id = cards.add(card))
    }

    @Test
    fun activityGroupsEntriesIntoInvoices() = runTest {
        val card = storedCard()
        entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 1000, "a", LocalDate.of(2026, 3, 10), card))
        entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 2000, "b", LocalDate.of(2026, 3, 20), card))
        entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 4000, "c", LocalDate.of(2026, 3, 26), card))
        entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 9999, "cash", LocalDate.of(2026, 3, 20), card = null))

        val activity = cards.observeActivity().first().single()
        val invoices = activity.invoices(LocalDate.of(2026, 3, 27))
        assertEquals(listOf(LocalDate.of(2026, 5, 5), LocalDate.of(2026, 4, 5)), invoices.map { it.dueDate })
        assertEquals(listOf(4000L, 3000L), invoices.map { it.totalCents })
    }

    @Test
    fun changingDaysReschedulesPendingEntriesAndInstallments() = runTest {
        val card = storedCard()
        val pastId = entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 1000, "past", LocalDate.of(2026, 2, 10), card)) // due Mar 5
        val openId = entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 2000, "open", LocalDate.of(2026, 3, 20), card)) // due Apr 5
        val tv = InstallmentPurchase(
            description = "TV",
            totalCents = 3000,
            installmentCount = 2,
            purchasedOn = LocalDate.of(2026, 3, 20),
            creditCardId = card.id,
        )
        installments.addOnCard(tv, card) // due Apr 5 and May 5

        cards.update(card.copy(dueDay = 15), today = LocalDate.of(2026, 3, 21))

        assertEquals(LocalDate.of(2026, 3, 5), entries.getById(pastId)!!.paidOn)
        assertEquals(LocalDate.of(2026, 4, 15), entries.getById(openId)!!.paidOn)
        val plan = installments.observePlans().first().single()
        assertEquals(listOf(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 5, 15)), plan.installments.map { it.paidOn })
    }

    @Test
    fun cardWithEntriesIsNotDeleted() = runTest {
        val card = storedCard()
        entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 1000, "a", LocalDate.of(2026, 3, 10), card))
        assertFalse(cards.delete(card))
        assertEquals(1, cards.observeAll().first().size)
    }

    @Test
    fun paymentsAreStoredAndRemovedWithTheCard() = runTest {
        val card = storedCard()
        entries.add(newEntry(EntryType.FLEXIBLE_EXPENSE, 1000, "a", LocalDate.of(2026, 3, 10), card))
        val due = LocalDate.of(2026, 4, 5)
        cards.markPaid(InvoicePayment(card.id, due, LocalDate.of(2026, 4, 1)))

        val invoice = cards.observeActivity().first().single().invoices(LocalDate.of(2026, 4, 10)).first { it.dueDate == due }
        assertEquals(InvoiceStatus.PAID, invoice.status)

        cards.unmarkPaid(card.id, due)
        assertTrue(cards.observeActivity().first().single().payments.isEmpty())

        val empty = storedCard()
        cards.markPaid(InvoicePayment(empty.id, due, due))
        assertTrue(cards.delete(empty))
        assertTrue(database.invoicePaymentDao().observeAll().first().none { it.cardId == empty.id })
    }
}
