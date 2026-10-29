package io.github.igornunesfontoura.yourfinances.ui.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * When the app asks for the device credential: always on a fresh start, and again after it spent
 * [timeoutMillis] or more in the background. A quick switch to another app does not lock it.
 *
 * [now] must be a monotonic clock (elapsed realtime), so changing the device time cannot skip it.
 */
class AppLock(private val now: () -> Long, private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS) {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundSince: Long? = null

    /** The credential is asked for on its own only once per lock, not again after a cancel. */
    private var autoPromptPending = true

    fun onBackground() {
        backgroundSince = now()
    }

    /**
     * [lockAvailable] is false when the lock is off or the device has no screen lock to ask for.
     * Removing the screen lock already takes the device credential, so opening then is fine.
     */
    fun onForeground(lockAvailable: Boolean) {
        val since = backgroundSince
        backgroundSince = null
        when {
            !lockAvailable -> _locked.value = false
            since != null && now() - since >= timeoutMillis -> {
                _locked.value = true
                autoPromptPending = true
            }
        }
    }

    /** True the first time it is asked while locked: the lock screen then opens the prompt. */
    fun consumeAutoPrompt(): Boolean {
        val pending = autoPromptPending && _locked.value
        autoPromptPending = false
        return pending
    }

    fun unlock() {
        _locked.value = false
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 60_000L
    }
}
