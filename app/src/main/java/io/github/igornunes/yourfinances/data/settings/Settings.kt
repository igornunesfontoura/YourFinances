package io.github.igornunes.yourfinances.data.settings

import android.content.SharedPreferences
import io.github.igornunes.yourfinances.domain.importing.CsvMapping
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

    /** The CSV column mapping last used for files with this [header], to skip mapping it again. */
    fun csvMapping(header: List<String>): CsvMapping?

    fun saveCsvMapping(header: List<String>, mapping: CsvMapping)
}

class SharedPreferencesSettings(private val preferences: SharedPreferences) : Settings {
    private val _lockEnabled = MutableStateFlow(preferences.getBoolean(LOCK_ENABLED, true))
    override val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    override fun setLockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(LOCK_ENABLED, enabled).apply()
        _lockEnabled.value = enabled
    }

    override fun csvMapping(header: List<String>): CsvMapping? {
        val parts = preferences.getString(csvKey(header), null)?.split(',') ?: return null
        val numbers = parts.take(3).map { it.toIntOrNull() ?: return null }
        if (numbers.any { it !in header.indices }) return null
        return CsvMapping(numbers[0], numbers[1], numbers[2], positiveIsExpense = parts.getOrNull(3) == "1")
    }

    override fun saveCsvMapping(header: List<String>, mapping: CsvMapping) {
        val value = with(mapping) { "$dateColumn,$descriptionColumn,$amountColumn,${if (positiveIsExpense) 1 else 0}" }
        preferences.edit().putString(csvKey(header), value).apply()
    }

    /** Keyed by a hash of the column names: the names themselves are not stored. */
    private fun csvKey(header: List<String>) = CSV_MAPPING + header.joinToString("\u0000").hashCode()

    private companion object {
        const val LOCK_ENABLED = "lock_enabled"
        const val CSV_MAPPING = "csv_mapping_"
    }
}
