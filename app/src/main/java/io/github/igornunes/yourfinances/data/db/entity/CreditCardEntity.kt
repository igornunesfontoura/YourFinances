package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.CreditCard

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "closing_day") val closingDay: Int,
    @ColumnInfo(name = "due_day") val dueDay: Int,
)

fun CreditCardEntity.toDomain() = CreditCard(id = id, name = name, closingDay = closingDay, dueDay = dueDay)

fun CreditCard.toEntity() = CreditCardEntity(id = id, name = name, closingDay = closingDay, dueDay = dueDay)
