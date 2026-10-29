package io.github.igornunes.yourfinances.domain.importing

import java.time.LocalDate

/**
 * One transaction read from a bank statement. [amountCents] is signed from the account's point of
 * view: negative when money left. [externalId] is the bank's own id (OFX FITID), when there is one.
 */
data class StatementLine(
    val date: LocalDate,
    val amountCents: Long,
    val description: String,
    val externalId: String? = null,
)
