package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.Category
import io.github.igornunes.yourfinances.domain.model.EntryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: EntryType,
)

fun CategoryEntity.toDomain() = Category(id = id, name = name, type = type)

fun Category.toEntity() = CategoryEntity(id = id, name = name, type = type)
