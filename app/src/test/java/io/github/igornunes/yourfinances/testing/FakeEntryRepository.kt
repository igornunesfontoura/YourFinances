package io.github.igornunes.yourfinances.testing

import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.domain.model.DateBasis
import io.github.igornunes.yourfinances.domain.model.Entry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.YearMonth

class FakeEntryRepository(initial: List<Entry> = emptyList()) : EntryRepository {
    val entries = MutableStateFlow(initial)
    /** When set, every write throws, as a full disk or a broken database would. */
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeMonths(first: YearMonth, last: YearMonth, basis: DateBasis): Flow<List<Entry>> =
        entries.map { list -> list.filter { YearMonth.from(basis.dateOf(it)) in first..last } }

    override suspend fun getById(id: Long): Entry? = entries.value.find { it.id == id }

    override suspend fun add(entry: Entry): Long {
        checkWritable()
        val id = nextId++
        entries.update { it + entry.copy(id = id) }
        return id
    }

    override suspend fun update(entry: Entry) {
        checkWritable()
        entries.update { list -> list.map { if (it.id == entry.id) entry else it } }
    }

    override suspend fun delete(entry: Entry) {
        checkWritable()
        entries.update { list -> list.filterNot { it.id == entry.id } }
    }

    private fun checkWritable() = check(!failWrites) { "write failed" }
}
