package io.github.igornunes.yourfinances.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore

/** Uses its own Keystore alias and files, so the app's real key is never touched. */
@RunWith(AndroidJUnit4::class)
class DatabaseKeyProviderTest {
    private val alias = "database_key_provider_test"
    private lateinit var dir: File
    private lateinit var keyFile: File
    private lateinit var databaseFile: File

    private fun provider() = DatabaseKeyProvider(keyFile, databaseFile, alias)

    private fun assertUnavailable() {
        val result = runCatching { provider().getOrCreatePassphrase() }
        assertEquals(DatabaseKeyUnavailableException::class.java, result.exceptionOrNull()?.javaClass)
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dir = File(context.cacheDir, "key-test").apply { deleteRecursively(); mkdirs() }
        keyFile = File(dir, "database.key")
        databaseFile = File(dir, "test.db")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test
    fun keyIsCreatedOnceAndReadBack() {
        val created = provider().getOrCreatePassphrase()
        assertEquals(32, created.size)
        assertArrayEquals(created, provider().getOrCreatePassphrase())
    }

    @Test
    fun databaseWithoutKeyFileIsNotGivenANewKey() {
        databaseFile.writeText("encrypted")
        assertUnavailable()
        assertFalse(keyFile.exists())
    }

    @Test
    fun keyFileWithoutKeystoreKeyIsUnavailable() {
        provider().getOrCreatePassphrase()
        databaseFile.writeText("encrypted")
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        assertUnavailable()
    }

    @Test
    fun damagedKeyFileIsUnavailable() {
        provider().getOrCreatePassphrase()
        databaseFile.writeText("encrypted")
        keyFile.writeBytes(keyFile.readBytes().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() })
        assertUnavailable()
        keyFile.writeBytes(byteArrayOf(0))
        assertUnavailable()
    }

    @Test
    fun staleKeyWithoutDatabaseIsReplaced() {
        provider().getOrCreatePassphrase()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        assertEquals(32, provider().getOrCreatePassphrase().size)
        databaseFile.writeText("encrypted")
        assertEquals(32, provider().getOrCreatePassphrase().size)
    }

    @Test
    fun deleteAllowsAFreshStart() {
        databaseFile.writeText("encrypted")
        val provider = provider()
        runCatching { provider.getOrCreatePassphrase() }
        databaseFile.delete()
        provider.delete()
        assertEquals(32, provider.getOrCreatePassphrase().size)
    }
}
