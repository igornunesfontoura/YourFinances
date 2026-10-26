package io.github.igornunes.yourfinances.data.db

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(AppDatabase.NAME)
    }

    @Test
    fun databaseFileIsNotPlainSqlite() = runTest {
        val passphrase = ByteArray(32) { it.toByte() }
        val database = AppDatabase.buildEncrypted(context, passphrase.copyOf())
        database.categoryDao().insert(Category(name = "Mercado", type = EntryType.FLEXIBLE_EXPENSE).toEntity())
        assertEquals(1, database.categoryDao().observeAll().first().size)
        database.close()

        val header = context.getDatabasePath(AppDatabase.NAME).inputStream().use { stream -> ByteArray(16).also { stream.read(it) } }
        assertFalse(String(header, Charsets.US_ASCII).startsWith("SQLite format 3"))

        val reopened = AppDatabase.buildEncrypted(context, passphrase.copyOf())
        assertEquals("Mercado", reopened.categoryDao().observeAll().first().single().name)
        reopened.close()
    }
}
