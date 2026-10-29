package io.github.igornunesfontoura.yourfinances.testing

import io.github.igornunesfontoura.yourfinances.data.repository.CategoryRepository
import io.github.igornunesfontoura.yourfinances.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    val categories = MutableStateFlow(initial)
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeAll(): Flow<List<Category>> = categories.map { list -> list.sortedBy { it.name } }

    override suspend fun getById(id: Long) = categories.value.find { it.id == id }

    override suspend fun add(category: Category): Long {
        check(!failWrites) { "write failed" }
        val id = nextId++
        categories.update { it + category.copy(id = id) }
        return id
    }

    override suspend fun update(category: Category) {
        check(!failWrites) { "write failed" }
        categories.update { list -> list.map { if (it.id == category.id) category else it } }
    }

    override suspend fun delete(category: Category) {
        check(!failWrites) { "write failed" }
        categories.update { list -> list.filterNot { it.id == category.id } }
    }
}
