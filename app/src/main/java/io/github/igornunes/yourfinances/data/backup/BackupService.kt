package io.github.igornunes.yourfinances.data.backup

import io.github.igornunes.yourfinances.data.db.dao.BackupDao
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.domain.export.entriesCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate

/** Encrypted backup, restore and CSV export of everything the user owns. */
interface BackupService {
    /** The whole database as an encrypted backup file. */
    suspend fun create(password: CharArray): ByteArray

    /**
     * Decrypts and validates a backup without touching the database.
     *
     * @throws BackupUnreadableException for a wrong password or a file that is not a backup.
     * @throws BackupInvalidException for content this version cannot restore.
     */
    suspend fun read(file: ByteArray, password: CharArray): BackupTables

    /** Replaces all the user's data with [tables], or changes nothing if any row is rejected. */
    suspend fun restore(tables: BackupTables)

    /** Every entry as UTF-8 CSV (see [entriesCsv]). */
    suspend fun entriesCsv(): ByteArray
}

/** [dao] is resolved on use, so a backup can be read while the database cannot be opened yet. */
class RoomBackupService(private val dao: () -> BackupDao, private val clock: Clock) : BackupService {
    override suspend fun create(password: CharArray): ByteArray {
        val tables = dao().readAll()
        return withContext(Dispatchers.Default) {
            BackupCrypto.encrypt(BackupFormat.encode(tables, LocalDate.now(clock)), password)
        }
    }

    override suspend fun read(file: ByteArray, password: CharArray): BackupTables = withContext(Dispatchers.Default) {
        BackupFormat.decode(BackupCrypto.decrypt(file, password))
    }

    override suspend fun restore(tables: BackupTables) = dao().replaceAll(tables)

    override suspend fun entriesCsv(): ByteArray {
        val tables = dao().readAll()
        return withContext(Dispatchers.Default) {
            entriesCsv(
                entries = tables.entries.map { it.toDomain() },
                cardNames = tables.creditCards.associate { it.id to it.name },
                categoryNames = tables.categories.associate { it.id to it.name },
            ).toByteArray(Charsets.UTF_8)
        }
    }
}
