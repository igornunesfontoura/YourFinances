package io.github.igornunes.yourfinances.data.db

import io.github.igornunes.yourfinances.domain.model.CategoryKind
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.model.Category
import io.github.igornunes.yourfinances.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Not the app's database: its background worker may open that one meanwhile. */
    private val name = "encrypted-test.db"

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(name)
    }

    @Test
    fun databaseFileIsNotPlainSqlite() = runTest {
        val passphrase = ByteArray(32) { it.toByte() }
        val database = AppDatabase.buildEncrypted(context, passphrase.copyOf(), name)
        // A new database starts with the default categories.
        val defaults = database.categoryDao().observeAll().first()
        assertEquals(setOf(CategoryKind.EXPENSE, CategoryKind.INCOME), defaults.map { it.kind }.toSet())
        database.categoryDao().insert(Category(name = "Pets", kind = CategoryKind.EXPENSE).toEntity())
        database.close()
        // Seeding happens on creation only: reopening adds nothing.
        val again = AppDatabase.buildEncrypted(context, passphrase.copyOf(), name)
        assertEquals(defaults.size + 1, again.categoryDao().observeAll().first().size)
        again.close()

        val header = context.getDatabasePath(name).inputStream().use { stream -> ByteArray(16).also { stream.read(it) } }
        assertFalse(String(header, Charsets.US_ASCII).startsWith("SQLite format 3"))

        val reopened = AppDatabase.buildEncrypted(context, passphrase.copyOf(), name)
        assertTrue(reopened.categoryDao().observeAll().first().any { it.name == "Pets" })
        reopened.close()
    }
}
