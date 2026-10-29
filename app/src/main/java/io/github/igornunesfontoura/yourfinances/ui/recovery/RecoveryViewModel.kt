package io.github.igornunesfontoura.yourfinances.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.backup.BackupService
import io.github.igornunesfontoura.yourfinances.data.backup.RestoreResult
import io.github.igornunesfontoura.yourfinances.data.backup.restoreBackup
import io.github.igornunesfontoura.yourfinances.data.files.Documents
import io.github.igornunesfontoura.yourfinances.data.security.DatabaseRecovery
import io.github.igornunesfontoura.yourfinances.data.security.KeyStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RecoveryProblem {
    WRONG_PASSWORD,
    INVALID_BACKUP,
    RESTORE_FAILED,

    /** The unreadable data was erased, but the backup could not be written into the new database. */
    RESTORE_FAILED_AFTER_ERASE,
    STILL_UNAVAILABLE,
    FAILED,
}

data class RecoveryUiState(
    val busy: Boolean = true,
    /** Only a lost key offers starting over; a failure that may pass is retried first. */
    val keyLost: Boolean = false,
    /**
     * A "temporary" failure that keeps failing may be permanent after all, so restoring a
     * backup is offered too: it only erases once the backup decrypted and validated.
     */
    val failedRetries: Int = 0,
    val problem: RecoveryProblem? = null,
    val recovered: Boolean = false,
) {
    val canRestore: Boolean get() = keyLost || failedRetries >= RETRIES_BEFORE_RESTORE
}

const val RETRIES_BEFORE_RESTORE = 2

/** The database key cannot be read: retry, restore a backup into a new database, or start over. */
class RecoveryViewModel(
    private val recovery: DatabaseRecovery,
    private val backups: BackupService,
    private val documents: Documents,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _state = MutableStateFlow(RecoveryUiState())
    val state: StateFlow<RecoveryUiState> = _state.asStateFlow()

    /** Once erased, later attempts restore into the new, empty database without erasing again. */
    private var erased = false

    private var running = false

    init {
        launchBusy { checked(problem = null) }
    }

    /** The unreadable database is only erased once the backup decrypted and validated. */
    fun restore(uri: String, password: CharArray) = launchBusy {
        val result = try {
            restoreBackup(documents, backups, uri, password, beforeReplace = ::eraseOnce)
        } finally {
            password.fill('\u0000')
        }
        when (result) {
            RestoreResult.RESTORED -> _state.value.copy(recovered = true)
            RestoreResult.WRONG_PASSWORD -> _state.value.copy(problem = RecoveryProblem.WRONG_PASSWORD)
            RestoreResult.INVALID_BACKUP -> _state.value.copy(problem = RecoveryProblem.INVALID_BACKUP)
            RestoreResult.FAILED -> _state.value.copy(
                problem = if (erased) RecoveryProblem.RESTORE_FAILED_AFTER_ERASE else RecoveryProblem.RESTORE_FAILED,
            )
        }
    }

    fun startOver() = launchBusy {
        eraseOnce()
        _state.value.copy(recovered = true)
    }

    /** The Keystore may have been briefly unavailable rather than wiped. */
    fun retry() = launchBusy { checked(problem = RecoveryProblem.STILL_UNAVAILABLE) }

    fun dismissMessage() = _state.update { it.copy(problem = null) }

    /** [problem] is reported when the key is still unreadable. */
    private suspend fun checked(problem: RecoveryProblem?): RecoveryUiState =
        when (withContext(io) { recovery.keyStatus() }) {
            KeyStatus.AVAILABLE -> _state.value.copy(recovered = true)
            KeyStatus.LOST -> _state.value.copy(keyLost = true, problem = problem)
            KeyStatus.TEMPORARILY_UNAVAILABLE -> _state.value.copy(
                keyLost = false,
                problem = problem,
                failedRetries = _state.value.failedRetries + if (problem != null) 1 else 0,
            )
        }

    private suspend fun eraseOnce() {
        if (erased) return
        withContext(io) { recovery.eraseUnreadableDatabase() }
        erased = true
    }

    /** One action at a time; nothing more once recovered. */
    private fun launchBusy(block: suspend () -> RecoveryUiState) {
        if (running || _state.value.recovered) return
        running = true
        _state.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            val next = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value.copy(problem = RecoveryProblem.FAILED)
            }
            _state.value = next.copy(busy = false)
            running = false
        }
    }
}
