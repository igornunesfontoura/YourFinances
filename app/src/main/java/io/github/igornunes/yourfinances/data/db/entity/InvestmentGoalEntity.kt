package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.InvestmentGoal
import java.time.YearMonth

/** One goal per starting month: setting it again for the same month replaces it. */
@Entity(tableName = "investment_goals")
data class InvestmentGoalEntity(
    @PrimaryKey @ColumnInfo(name = "starts_in") val startsIn: YearMonth,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
)

fun InvestmentGoalEntity.toDomain() = InvestmentGoal(startsIn = startsIn, amountCents = amountCents)

fun InvestmentGoal.toEntity() = InvestmentGoalEntity(startsIn = startsIn, amountCents = amountCents)
