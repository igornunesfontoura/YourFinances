package io.github.igornunes.yourfinances.testing

import io.github.igornunes.yourfinances.data.backup.BackupInvalidException
import io.github.igornunes.yourfinances.data.backup.BackupService
import io.github.igornunes.yourfinances.data.backup.BackupTables
import io.github.igornunes.yourfinances.data.backup.BackupUnreadableException
import io.github.igornunes.yourfinances.data.files.Documents
import io.github.igornunes.yourfinances.data.security.DatabaseRecovery
import io.github.igornunes.yourfinances.data.security.KeyStatus
import io.github.igornunes.yourfinances.data.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

val EMPTY_TABLES = BackupTables(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

/** "Backups" are the password followed by the content marker; enough to check what goes where. */
class FakeBackupService : BackupService {
    var current: BackupTables = EMPTY_TABLES
    var invalidContent = false
    var failRestore = false
    val events = mutableListOf<String>()

    override suspend fun create(password: CharArray) = "backup:${String(password)}".toByteArray()

    override suspend fun read(file: ByteArray, password: CharArray): BackupTables {
        if (file.toString(Charsets.UTF_8) != "backup:${String(password)}") throw BackupUnreadableException()
        if (invalidContent) throw BackupInvalidException()
        return EMPTY_TABLES
    }

    override suspend fun restore(tables: BackupTables) {
        check(!failRestore) { "constraint failed" }
        events += "restore"
        current = tables
    }

    override suspend fun entriesCsv() = "occurred_on\r\n".toByteArray()
}

class FakeDocuments : Documents {
    val files = mutableMapOf<String, ByteArray>()
    var failWrites = false

    override suspend fun write(uri: String, bytes: ByteArray) {
        if (failWrites) throw IOException("disk full")
        files[uri] = bytes
    }

    override suspend fun read(uri: String): ByteArray = files[uri] ?: throw IOException("missing")

    override suspend fun delete(uri: String) {
        files.remove(uri)
    }
}

class FakeSettings(lockEnabled: Boolean = true) : Settings {
    override val lockEnabled = MutableStateFlow(lockEnabled)

    override fun setLockEnabled(enabled: Boolean) {
        lockEnabled.value = enabled
    }
}

class FakeDatabaseRecovery(private val events: MutableList<String>) : DatabaseRecovery {
    var status = KeyStatus.LOST

    override fun keyStatus() = status

    override fun eraseUnreadableDatabase() {
        check(status != KeyStatus.AVAILABLE) { "the database can be opened" }
        events += "erase"
        // As in the real database: the next use creates a new key and an empty database.
        status = KeyStatus.AVAILABLE
    }
}
