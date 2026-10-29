package io.github.igornunes.yourfinances.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.igornunes.yourfinances.data.repository.RoomEntryRepository
import io.github.igornunes.yourfinances.data.repository.RoomRecurrenceRepository
import io.github.igornunes.yourfinances.domain.entry.confirmed
import io.github.igornunes.yourfinances.domain.entry.edited
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import io.github.igornunes.yourfinances.domain.recurrence.predictionWindow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.model.CreditCard
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class RecurrenceRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var entries: RoomEntryRepository
    private lateinit var recurrences: RoomRecurrenceRepository

    private val today = LocalDate.of(2026, 3, 15)
    private val window = predictionWindow(today) // March 1 .. May 31

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        entries = RoomEntryRepository(database)
        recurrences = RoomRecurrenceRepository(database)
    }

    @After
    fun tearDown() = database.close()

    private val streaming = Recurrence(
        type = EntryType.FIXED_EXPENSE,
        amountCents = 4990,
        description = "Streaming",
        frequency = Frequency.MONTHLY,
        dayOfMonth = 31,
        startsOn = LocalDate.of(2026, 1, 1),
    )

    private suspend fun all(): List<Entry> =
        (3..5).flatMap { entries.observeMonth(YearMonth.of(2026, it), DateBasis.CASH).first() }

    @Test
    fun addingPredictsTheWindowWithoutBackfilling() = runTest {
        recurrences.add(streaming, window)
        assertEquals(
            listOf(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30), LocalDate.of(2026, 5, 31)),
            all().map { it.paidOn },
        )
        assertEquals(0, entries.observeMonth(YearMonth.of(2026, 2), DateBasis.CASH).first().size)
    }

    @Test
    fun predictAllIsIdempotent() = runTest {
        recurrences.add(streaming, window)
        assertEquals(0, recurrences.predictAll(window))
        assertEquals(3, all().size)
    }

    @Test
    fun deletedPredictedEntryIsNotPredictedAgain() = runTest {
        recurrences.add(streaming, window)
        entries.delete(all().first())

        assertEquals(0, recurrences.predictAll(window))
        assertEquals(2, all().size)
    }

    @Test
    fun updatingRebuildsPredictedAndKeepsConfirmed() = runTest {
        val id = recurrences.add(streaming, window)
        val march = all().first()
        entries.update(march.confirmed())

        recurrences.update(streaming.copy(id = id, amountCents = 5990, dayOfMonth = 10), window)

        val result = all()
        assertEquals(3, result.size)
        assertEquals(march.confirmed(), result.first())
        assertEquals(listOf(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 10)), result.drop(1).map { it.paidOn })
        assertEquals(listOf(5990L, 5990L), result.drop(1).map { it.amountCents })
    }

    @Test
    fun applyToUpcomingUpdatesTemplateAndFollowingOccurrences() = runTest {
        val id = recurrences.add(streaming, window)
        val march = all().first()

        val edited = march.edited(EntryType.FIXED_EXPENSE, 5490, "Streaming 4K", march.occurredOn, card = null)
        recurrences.applyToUpcoming(edited, window)

        val result = all()
        assertEquals(EntryStatus.CONFIRMED, result.first().status)
        assertEquals(listOf(5490L, 5490L, 5490L), result.map { it.amountCents })
        assertEquals(listOf("Streaming 4K", "Streaming 4K"), result.drop(1).map { it.description })
        assertEquals(5490L, recurrences.getById(id)!!.amountCents)
    }

    @Test
    fun deletingKeepsHistoryAndRemovesUpcomingPredictions() = runTest {
        val id = recurrences.add(streaming, window)
        val march = all().first()
        entries.update(march.confirmed())

        recurrences.delete(streaming.copy(id = id), window)

        val result = all()
        assertEquals(1, result.size)
        assertNull(result.single().recurrenceId)
        assertNull(result.single().scheduledFor)
    }

    @Test
    fun movingTheStartForwardDropsPastPredictions() = runTest {
        val id = recurrences.add(streaming, window)
        val laterWindow = predictionWindow(LocalDate.of(2026, 4, 2)) // April .. June; March is past

        recurrences.update(streaming.copy(id = id, startsOn = LocalDate.of(2026, 5, 1)), laterWindow)

        assertEquals(listOf(LocalDate.of(2026, 5, 31)), all().map { it.paidOn })
    }

    @Test
    fun applyToUpcomingAfterTemplateDeletionStillSavesTheEntry() = runTest {
        val id = recurrences.add(streaming, window)
        val march = all().first()
        val edited = march.edited(EntryType.FIXED_EXPENSE, 5490, "Streaming", march.occurredOn, card = null)
        recurrences.delete(streaming.copy(id = id), predictionWindow(LocalDate.of(2026, 4, 1)))

        recurrences.applyToUpcoming(edited.copy(), window)

        val stored = entries.getById(march.id)!!
        assertEquals(5490L, stored.amountCents)
        assertEquals(EntryStatus.CONFIRMED, stored.status)
    }

    @Test
    fun endDateStopsPredictions() = runTest {
        val id = recurrences.add(streaming, window)
        recurrences.update(streaming.copy(id = id, endsOn = LocalDate.of(2026, 4, 30)), window)
        assertEquals(listOf(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)), all().map { it.paidOn })
    }

    @Test
    fun recurrenceOnACardIsPaidOnTheInvoiceDueDate() = runTest {
        val cardId = database.creditCardDao().insert(CreditCard(name = "Nubank", closingDay = 25, dueDay = 5).toEntity())
        recurrences.add(streaming.copy(creditCardId = cardId), window)
        val predicted = all()
        assertEquals(setOf(cardId), predicted.map { it.creditCardId }.toSet())
        assertTrue(predicted.all { it.paidOn.dayOfMonth == 5 && it.paidOn.isAfter(it.occurredOn) })
    }
}
