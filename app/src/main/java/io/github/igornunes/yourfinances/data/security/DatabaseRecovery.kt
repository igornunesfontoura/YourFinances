package io.github.igornunes.yourfinances.data.security

import android.content.Context
import io.github.igornunes.yourfinances.data.db.AppDatabase

enum class KeyStatus {
    AVAILABLE,

    /** Gone for good: only a backup brings the data back. */
    LOST,

    /** The Keystore failed in a way that may pass; the data must not be offered for erasing. */
    TEMPORARILY_UNAVAILABLE,
}

/** What to do when the database key cannot be read. */
interface DatabaseRecovery {
    fun keyStatus(): KeyStatus

    /**
     * Erases the database that cannot be opened and its key, so the next use creates an empty
     * one. Refuses to run while the database can be opened.
     */
    fun eraseUnreadableDatabase()
}

class KeystoreDatabaseRecovery(private val context: Context, private val keys: DatabaseKeyProvider) : DatabaseRecovery {
    override fun keyStatus(): KeyStatus = try {
        keys.getOrCreatePassphrase()
        KeyStatus.AVAILABLE
    } catch (_: DatabaseKeyUnavailableException) {
        KeyStatus.LOST
    } catch (_: Exception) {
        KeyStatus.TEMPORARILY_UNAVAILABLE
    }

    override fun eraseUnreadableDatabase() {
        check(keyStatus() != KeyStatus.AVAILABLE) { "the database can be opened" }
        context.deleteDatabase(AppDatabase.NAME)
        keys.delete()
    }
}
