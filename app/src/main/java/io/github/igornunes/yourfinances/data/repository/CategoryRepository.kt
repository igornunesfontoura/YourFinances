package io.github.igornunes.yourfinances.data.repository

import io.github.igornunes.yourfinances.data.db.dao.CategoryDao
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.data.db.entity.toEntity
import io.github.igornunes.yourfinances.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CategoryRepository {
    /** Sorted by name. */
    fun observeAll(): Flow<List<Category>>

    suspend fun getById(id: Long): Category?

    suspend fun add(category: Category): Long

    suspend fun update(category: Category)

    /** Entries, recurrences and purchases that used it keep existing, without a category. */
    suspend fun delete(category: Category)
}

class RoomCategoryRepository(private val dao: CategoryDao) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? = dao.getById(id)?.toDomain()

    override suspend fun add(category: Category): Long = dao.insert(category.toEntity())

    override suspend fun update(category: Category) = dao.update(category.toEntity())

    override suspend fun delete(category: Category) = dao.delete(category.toEntity())
}
