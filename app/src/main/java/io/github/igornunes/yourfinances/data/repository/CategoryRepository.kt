package io.github.igornunes.yourfinances.data.repository

import io.github.igornunes.yourfinances.data.db.dao.CategoryDao
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val dao: CategoryDao) {
    fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(category: Category): Long = dao.insert(category.toEntity())

    suspend fun update(category: Category) = dao.update(category.toEntity())

    suspend fun delete(category: Category) = dao.delete(category.toEntity())
}
