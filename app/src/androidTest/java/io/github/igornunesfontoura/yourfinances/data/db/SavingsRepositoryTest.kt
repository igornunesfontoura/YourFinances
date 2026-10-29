package io.github.igornunesfontoura.yourfinances.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.igornunesfontoura.yourfinances.data.db.entity.toEntity
import io.github.igornunesfontoura.yourfinances.data.repository.RoomEntryRepository
import io.github.igornunesfontoura.yourfinances.data.repository.RoomSavingsRepository
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.domain.model.Frequency
import io.github.igornunesfontoura.yourfinances.domain.model.Recurrence
import io.github.igornunesfontoura.yourfinances.domain.model.SavingsPocket
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
class SavingsRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var savings: RoomSavingsRepository
    private lateinit var entries: RoomEntryRepository

    private val day = LocalDate.of(2026, 3, 5)

    private fun entry(type: EntryType, pocketId: Long? = null) =
        Entry(type = type, amountCents = 1000, description = "", occurredOn = day, paidOn = day, savingsPocketId = pocketId)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        savings = RoomSavingsRepository(database)
        entries = RoomEntryRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun movementsAreContributionsAndWithdrawalsOnly() = runTest {
        val pocket = savings.add(SavingsPocket(name = "Reserva"))
        entries.add(entry(EntryType.INVESTMENT, pocket))
        entries.add(entry(EntryType.WITHDRAWAL))
        entries.add(entry(EntryType.INCOME))
        entries.add(entry(EntryType.FLEXIBLE_EXPENSE))

        val movements = savings.observeMovements().first()
        assertEquals(listOf(EntryType.INVESTMENT, EntryType.WITHDRAWAL), movements.map { it.type })
        assertEquals(pocket, movements.first().savingsPocketId)
    }

    @Test
    fun pocketWithMovementsIsNotDeleted() = runTest {
        val used = savings.add(SavingsPocket(name = "Reserva", targetCents = 500000))
        val empty = savings.add(SavingsPocket(name = "Vazia"))
        entries.add(entry(EntryType.INVESTMENT, used))

        assertFalse(savings.delete(savings.getPocket(used)!!))
        assertTrue(savings.delete(savings.getPocket(empty)!!))
        assertEquals(listOf(SavingsPocket(used, "Reserva", 500000)), savings.observePockets().first())
    }

    @Test
    fun pocketUsedByARecurrenceIsNotDeleted() = runTest {
        val pocket = savings.add(SavingsPocket(name = "Reserva"))
        database.recurrenceDao().insert(
            Recurrence(
                type = EntryType.INVESTMENT, amountCents = 50000, description = "", frequency = Frequency.MONTHLY,
                dayOfMonth = 5, startsOn = day, savingsPocketId = pocket,
            ).toEntity(),
        )
        assertFalse(savings.delete(savings.getPocket(pocket)!!))
    }
}
