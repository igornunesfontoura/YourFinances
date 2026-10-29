package io.github.igornunes.yourfinances.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.backup.BackupService
import io.github.igornunes.yourfinances.data.backup.RestoreResult
import io.github.igornunes.yourfinances.data.backup.restoreBackup
import io.github.igornunes.yourfinances.data.files.Documents
import io.github.igornunes.yourfinances.data.settings.Settings
import io.github.igornunes.yourfinances.ui.lock.DeviceLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

enum class SettingsMessage {
    BACKUP_SAVED,
    BACKUP_FAILED,
    RESTORED,
    WRONG_PASSWORD,
    INVALID_BACKUP,
    RESTORE_FAILED,
    CSV_SAVED,
    CSV_FAILED,
}

data class SettingsUiState(
    val lockEnabled: Boolean,
    val deviceSecure: Boolean,
    val busy: Boolean = false,
    val message: SettingsMessage? = null,
)

private data class Work(val busy: Boolean = false, val message: SettingsMessage? = null)

class SettingsViewModel(
    private val settings: Settings,
    deviceLock: DeviceLock,
    private val backups: BackupService,
    private val documents: Documents,
    private val clock: Clock,
) : ViewModel() {
    private val deviceSecure = deviceLock.isDeviceSecure()
    private val work = MutableStateFlow(Work())

    val state: StateFlow<SettingsUiState> = combine(settings.lockEnabled, work) { lock, work ->
        SettingsUiState(lockEnabled = lock, deviceSecure = deviceSecure, busy = work.busy, message = work.message)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(settings.lockEnabled.value, deviceSecure),
    )

    fun setLockEnabled(enabled: Boolean) = settings.setLockEnabled(enabled)

    fun backupFileName(): String = "yourfinances-${LocalDate.now(clock)}.yfbackup"

    fun csvFileName(): String = "yourfinances-lancamentos-${LocalDate.now(clock)}.csv"

    /**
     * Kept here while the system file picker is open: it survives configuration changes and,
     * unlike saved state, is never written to disk. Lost on process death, which then fails.
     */
    private var backupPassword: CharArray? = null

    fun setBackupPassword(password: CharArray) {
        backupPassword?.fill('\u0000')
        backupPassword = password
    }

    /** [uri] is null when the user left the picker without choosing a file. */
    fun onBackupDestination(uri: String?) {
        val password = backupPassword
        backupPassword = null
        when {
            uri == null -> password?.fill('\u0000')
            // The picker already created an empty file: remove it rather than leave a fake backup.
            password == null -> run(SettingsMessage.BACKUP_FAILED, SettingsMessage.BACKUP_FAILED) { documents.delete(uri) }
            else -> run(SettingsMessage.BACKUP_SAVED, SettingsMessage.BACKUP_FAILED) {
                try {
                    documents.write(uri, backups.create(password))
                } finally {
                    password.fill('\u0000')
                }
            }
        }
    }

    override fun onCleared() {
        backupPassword?.fill('\u0000')
    }

    fun exportCsv(uri: String) = run(SettingsMessage.CSV_SAVED, SettingsMessage.CSV_FAILED) {
        documents.write(uri, backups.entriesCsv())
    }

    fun restore(uri: String, password: CharArray) {
        if (!start()) return
        viewModelScope.launch {
            val result = try {
                restoreBackup(documents, backups, uri, password)
            } finally {
                password.fill('\u0000')
            }
            val message = when (result) {
                RestoreResult.RESTORED -> SettingsMessage.RESTORED
                RestoreResult.WRONG_PASSWORD -> SettingsMessage.WRONG_PASSWORD
                RestoreResult.INVALID_BACKUP -> SettingsMessage.INVALID_BACKUP
                RestoreResult.FAILED -> SettingsMessage.RESTORE_FAILED
            }
            work.value = Work(message = message)
        }
    }

    fun dismissMessage() = work.update { it.copy(message = null) }

    /** One operation at a time: a second tap while busy is ignored. */
    private fun start(): Boolean {
        if (work.value.busy) return false
        work.value = Work(busy = true)
        return true
    }

    private fun run(success: SettingsMessage, failure: SettingsMessage, block: suspend () -> Unit) {
        if (!start()) return
        viewModelScope.launch {
            val message = try {
                block()
                success
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failure
            }
            work.value = Work(message = message)
        }
    }
}

/** Backup passwords protect a file that may end up anywhere, so short ones are refused. */
const val MIN_BACKUP_PASSWORD_LENGTH = 8

enum class PasswordProblem { TOO_SHORT, MISMATCH }

fun backupPasswordProblem(password: String, confirmation: String): PasswordProblem? = when {
    password.length < MIN_BACKUP_PASSWORD_LENGTH -> PasswordProblem.TOO_SHORT
    password != confirmation -> PasswordProblem.MISMATCH
    else -> null
}
