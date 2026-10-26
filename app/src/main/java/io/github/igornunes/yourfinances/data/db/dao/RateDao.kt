package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.igornunes.yourfinances.data.db.entity.RateEntity
import java.time.LocalDate

@Dao
interface RateDao {
    @Upsert
    suspend fun upsertAll(rates: List<RateEntity>)

    @Query("SELECT * FROM rates WHERE series_code = :seriesCode AND date BETWEEN :from AND :to ORDER BY date")
    suspend fun getBetween(seriesCode: Int, from: LocalDate, to: LocalDate): List<RateEntity>

    @Query("SELECT MAX(date) FROM rates WHERE series_code = :seriesCode")
    suspend fun latestDate(seriesCode: Int): LocalDate?
}
