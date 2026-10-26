package io.github.igornunes.yourfinances.data.db

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.LocalDate

/** Dates are stored as epoch days (sortable); decimals as plain strings (no precision loss). */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)
}
