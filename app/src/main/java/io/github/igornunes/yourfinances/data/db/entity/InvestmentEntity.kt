package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.Indexer
import io.github.igornunes.yourfinances.domain.model.Investment
import java.math.BigDecimal
import java.time.LocalDate

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val product: String,
    val indexer: Indexer,
    val rate: BigDecimal,
    @ColumnInfo(name = "invested_on") val investedOn: LocalDate,
    @ColumnInfo(name = "principal_cents") val principalCents: Long,
)

fun InvestmentEntity.toDomain() = Investment(
    id = id,
    product = product,
    indexer = indexer,
    rate = rate,
    investedOn = investedOn,
    principalCents = principalCents,
)

fun Investment.toEntity() = InvestmentEntity(
    id = id,
    product = product,
    indexer = indexer,
    rate = rate,
    investedOn = investedOn,
    principalCents = principalCents,
)
