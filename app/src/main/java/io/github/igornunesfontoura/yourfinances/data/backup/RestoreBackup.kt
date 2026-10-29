package io.github.igornunesfontoura.yourfinances.data.backup

import io.github.igornunesfontoura.yourfinances.data.files.Documents
import kotlinx.coroutines.CancellationException

enum class RestoreResult { RESTORED, WRONG_PASSWORD, INVALID_BACKUP, FAILED }

/**
 * Reads the backup at [uri] and replaces the user's data with it. The file is fully decrypted and
 * validated before [beforeReplace] runs and anything is deleted. Nothing is logged.
 */
suspend fun restoreBackup(
    documents: Documents,
    backups: BackupService,
    uri: String,
    password: CharArray,
    beforeReplace: suspend () -> Unit = {},
): RestoreResult = try {
    val tables = backups.read(documents.read(uri), password)
    beforeReplace()
    backups.restore(tables)
    RestoreResult.RESTORED
} catch (e: CancellationException) {
    throw e
} catch (_: BackupUnreadableException) {
    RestoreResult.WRONG_PASSWORD
} catch (_: BackupInvalidException) {
    RestoreResult.INVALID_BACKUP
} catch (_: Exception) {
    RestoreResult.FAILED
}
