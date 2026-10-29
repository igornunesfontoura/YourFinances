package io.github.igornunesfontoura.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.LocalDate

/** A slot whose predicted entry the user deleted; it must not be generated again. */
@Entity(
    tableName = "recurrence_skips",
    primaryKeys = ["recurrence_id", "slot"],
    foreignKeys = [
        ForeignKey(
            entity = RecurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrence_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RecurrenceSkipEntity(
    @ColumnInfo(name = "recurrence_id") val recurrenceId: Long,
    val slot: LocalDate,
)
