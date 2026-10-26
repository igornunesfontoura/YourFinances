package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import io.github.igornunes.yourfinances.domain.investment.RateObservation
import java.math.BigDecimal
import java.time.LocalDate

/** Local cache of BCB SGS series values. */
@Entity(tableName = "rates", primaryKeys = ["series_code", "date"])
data class RateEntity(
    @ColumnInfo(name = "series_code") val seriesCode: Int,
    val date: LocalDate,
    @ColumnInfo(name = "value_percent") val valuePercent: BigDecimal,
)

fun RateEntity.toDomain() = RateObservation(date = date, valuePercent = valuePercent)
