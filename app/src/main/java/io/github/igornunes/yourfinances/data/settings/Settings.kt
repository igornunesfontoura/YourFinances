package io.github.igornunes.yourfinances.data.settings

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App preferences. They hold no financial data, so plain SharedPreferences are enough, and they
 * stay readable while the database cannot be opened.
 */
interface Settings {
    /** On by default: the lock only works when the device has a screen lock, though. */
    val lockEnabled: StateFlow<Boolean>

    fun setLockEnabled(enabled: Boolean)
}

class SharedPreferencesSettings(private val preferences: SharedPreferences) : Settings {
    private val _lockEnabled = MutableStateFlow(preferences.getBoolean(LOCK_ENABLED, true))
    override val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    override fun setLockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(LOCK_ENABLED, enabled).apply()
        _lockEnabled.value = enabled
    }

    private companion object {
        const val LOCK_ENABLED = "lock_enabled"
    }
}
