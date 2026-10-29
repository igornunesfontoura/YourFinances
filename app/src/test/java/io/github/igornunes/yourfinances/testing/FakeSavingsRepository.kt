package io.github.igornunes.yourfinances.testing

import io.github.igornunes.yourfinances.data.repository.SavingsRepository
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.SavingsPocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeSavingsRepository(
    initial: List<SavingsPocket> = emptyList(),
    private val entries: MutableStateFlow<List<Entry>> = MutableStateFlow(emptyList()),
) : SavingsRepository {
    val pockets = MutableStateFlow(initial)
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observePockets(): Flow<List<SavingsPocket>> = pockets

    override fun observeMovements(): Flow<List<Entry>> = entries.map { list -> list.filter { it.type.movesSavings } }

    override suspend fun getPocket(id: Long) = pockets.value.find { it.id == id }

    override suspend fun add(pocket: SavingsPocket): Long {
        check(!failWrites) { "write failed" }
        val id = nextId++
        pockets.update { it + pocket.copy(id = id) }
        return id
    }

    override suspend fun update(pocket: SavingsPocket) {
        check(!failWrites) { "write failed" }
        pockets.update { list -> list.map { if (it.id == pocket.id) pocket else it } }
    }

    override suspend fun delete(pocket: SavingsPocket): Boolean {
        check(!failWrites) { "write failed" }
        if (entries.value.any { it.savingsPocketId == pocket.id }) return false
        pockets.update { list -> list.filterNot { it.id == pocket.id } }
        return true
    }
}
