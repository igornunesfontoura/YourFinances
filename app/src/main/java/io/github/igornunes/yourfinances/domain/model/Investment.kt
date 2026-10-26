package io.github.igornunes.yourfinances.domain.model

import java.math.BigDecimal
import java.time.LocalDate

enum class Indexer { CDI, SELIC, IPCA, PREFIXADO }

/**
 * A manually registered contribution.
 *
 * Meaning of [rate] by [indexer]:
 * - [Indexer.CDI], [Indexer.SELIC]: percentage of the index (110 = 110% of CDI);
 * - [Indexer.IPCA]: yearly spread over IPCA (6 = IPCA + 6% p.a.);
 * - [Indexer.PREFIXADO]: yearly rate (12 = 12% p.a.).
 */
data class Investment(
    val id: Long = 0,
    val product: String,
    val indexer: Indexer,
    val rate: BigDecimal,
    val investedOn: LocalDate,
    val principalCents: Long,
) {
    init {
        require(principalCents >= 0) { "principalCents must not be negative" }
    }
}
