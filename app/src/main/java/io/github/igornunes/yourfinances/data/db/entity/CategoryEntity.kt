package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.Category
import io.github.igornunes.yourfinances.domain.model.CategoryKind

/** The kind lives in the `type` column, which held an entry type before v5. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "type") val kind: CategoryKind,
    @ColumnInfo(name = "monthly_limit_cents") val monthlyLimitCents: Long? = null,
)

fun CategoryEntity.toDomain() = Category(id = id, name = name, kind = kind, monthlyLimitCents = monthlyLimitCents)

fun Category.toEntity() = CategoryEntity(id = id, name = name, kind = kind, monthlyLimitCents = monthlyLimitCents)
