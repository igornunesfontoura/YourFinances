package io.github.igornunesfontoura.yourfinances.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Save/delete lifecycle shared by the forms. */
data class FormStatus(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val finished: Boolean = false,
) {
    val isBusy: Boolean get() = loading || saving || finished
}

/**
 * Runs one write: marks the form as saving, finishes it on success and flags the failure
 * otherwise. Nothing is logged, since the data is financial.
 */
fun ViewModel.persist(update: ((FormStatus) -> FormStatus) -> Unit, write: suspend () -> Unit) {
    update { it.copy(saving = true, failed = false) }
    viewModelScope.launch {
        try {
            write()
            update { it.copy(saving = false, finished = true) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            update { it.copy(saving = false, failed = true) }
        }
    }
}

/** Loads with the same error policy: a failure closes the form. */
fun <T : Any> ViewModel.load(
    update: ((FormStatus) -> FormStatus) -> Unit,
    read: suspend () -> T?,
    onLoaded: (T) -> Unit,
) {
    update { it.copy(loading = true) }
    viewModelScope.launch {
        val value = try {
            read()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (value == null) {
            update { it.copy(loading = false, finished = true) }
        } else {
            onLoaded(value)
            update { it.copy(loading = false) }
        }
    }
}
