package io.github.igornunes.yourfinances.testing

import io.github.igornunes.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.Recurrence
import io.github.igornunes.yourfinances.domain.recurrence.PredictionWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Stores templates and records the windows used; prediction itself is covered by the DAO tests. */
class FakeRecurrenceRepository(initial: List<Recurrence> = emptyList()) : RecurrenceRepository {
    val recurrences = MutableStateFlow(initial)
    val windows = mutableListOf<PredictionWindow>()
    val appliedEntries = mutableListOf<Entry>()
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeAll(): Flow<List<Recurrence>> = recurrences

    override suspend fun getById(id: Long): Recurrence? = recurrences.value.find { it.id == id }

    override suspend fun add(recurrence: Recurrence, window: PredictionWindow): Long {
        checkWritable()
        val id = nextId++
        recurrences.update { it + recurrence.copy(id = id) }
        windows += window
        return id
    }

    override suspend fun update(recurrence: Recurrence, window: PredictionWindow) {
        checkWritable()
        recurrences.update { list -> list.map { if (it.id == recurrence.id) recurrence else it } }
        windows += window
    }

    override suspend fun delete(recurrence: Recurrence, window: PredictionWindow) {
        windows += window
        checkWritable()
        recurrences.update { list -> list.filterNot { it.id == recurrence.id } }
    }

    override suspend fun applyToUpcoming(entry: Entry, window: PredictionWindow) {
        checkWritable()
        appliedEntries += entry
        windows += window
    }

    override suspend fun predictAll(window: PredictionWindow): Int {
        windows += window
        return 0
    }

    private fun checkWritable() = check(!failWrites) { "write failed" }
}
