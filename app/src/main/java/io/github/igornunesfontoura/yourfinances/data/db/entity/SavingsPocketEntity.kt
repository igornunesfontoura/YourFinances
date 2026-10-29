package io.github.igornunesfontoura.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.igornunesfontoura.yourfinances.domain.model.SavingsPocket

@Entity(tableName = "savings_pockets")
data class SavingsPocketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "target_cents") val targetCents: Long?,
)

fun SavingsPocketEntity.toDomain() = SavingsPocket(id = id, name = name, targetCents = targetCents)

fun SavingsPocket.toEntity() = SavingsPocketEntity(id = id, name = name, targetCents = targetCents)
