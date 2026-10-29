package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Recurrence
import java.time.LocalDate

@Entity(
    tableName = "recurrences",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SavingsPocketEntity::class,
            parentColumns = ["id"],
            childColumns = ["savings_pocket_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_id"), Index("savings_pocket_id")],
)
data class RecurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val description: String,
    val frequency: Frequency,
    @ColumnInfo(name = "day_of_month") val dayOfMonth: Int,
    @ColumnInfo(name = "starts_on") val startsOn: LocalDate,
    @ColumnInfo(name = "ends_on") val endsOn: LocalDate?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "savings_pocket_id") val savingsPocketId: Long? = null,
)

fun RecurrenceEntity.toDomain() = Recurrence(
    id = id,
    type = type,
    amountCents = amountCents,
    description = description,
    frequency = frequency,
    dayOfMonth = dayOfMonth,
    startsOn = startsOn,
    endsOn = endsOn,
    categoryId = categoryId,
    savingsPocketId = savingsPocketId,
)

fun Recurrence.toEntity() = RecurrenceEntity(
    id = id,
    type = type,
    amountCents = amountCents,
    description = description,
    frequency = frequency,
    dayOfMonth = dayOfMonth,
    startsOn = startsOn,
    endsOn = endsOn,
    categoryId = categoryId,
    savingsPocketId = savingsPocketId,
)
