package io.github.igornunes.yourfinances.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.igornunes.yourfinances.data.repository.RoomImportRepository
import io.github.igornunes.yourfinances.domain.importing.StatementLine
import io.github.igornunes.yourfinances.domain.importing.importKeys
import io.github.igornunes.yourfinances.domain.importing.planImport
import io.github.igornunes.yourfinances.domain.importing.toEntry
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ImportRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var imports: RoomImportRepository

    private val day = LocalDate.of(2026, 3, 5)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        imports = RoomImportRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun importedKeysAreFoundAgainEvenInLargeFiles() = runTest {
        // More lines than one query may bind.
        val lines = (1..1200).map { StatementLine(day, -it.toLong(), "linha $it") }
        val plan = planImport(lines, emptySet(), emptyList(), card = null)
        imports.importAll(plan.map { it.toEntry(null) })

        assertEquals(importKeys(lines).toSet(), imports.existingKeys(importKeys(lines)))
        val again = planImport(lines, imports.existingKeys(importKeys(lines)), imports.entriesOccurredBetween(day, day), card = null)
        assertTrue(again.none { it.selected })
    }

    @Test
    fun entriesBetweenDatesAreInclusive() = runTest {
        val entry = Entry(type = EntryType.INCOME, amountCents = 1, description = "", occurredOn = day, paidOn = day)
        imports.importAll(listOf(entry, entry.copy(occurredOn = day.plusDays(2), paidOn = day.plusDays(2))))
        assertEquals(2, imports.entriesOccurredBetween(day, day.plusDays(2)).size)
        assertEquals(1, imports.entriesOccurredBetween(day.plusDays(1), day.plusDays(5)).size)
    }
}
