package io.github.igornunesfontoura.yourfinances.domain.importing

import io.github.igornunesfontoura.yourfinances.domain.entry.newEntry
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import kotlin.math.absoluteValue

/** Lines that usually should not become entries, so they start unselected. */
enum class ImportHint {
    /** "Saldo anterior", "Saldo do dia": a balance, not a transaction. */
    BALANCE,

    /** Paying the card bill from the account: its purchases are already counted on the card. */
    CARD_BILL_PAYMENT,
}

enum class DuplicateKind {
    /** The same statement line was imported before (same key). */
    ALREADY_IMPORTED,

    /** An entry with the same date, amount and direction exists, perhaps typed by hand. */
    POSSIBLE,
}

/** A statement line as it would be imported; the user may change [type], [categoryId] and [selected]. */
data class ImportCandidate(
    val line: StatementLine,
    val key: String,
    val type: EntryType,
    val categoryId: Long? = null,
    val selected: Boolean,
    val duplicate: DuplicateKind? = null,
    val hint: ImportHint? = null,
)

/**
 * Stable identity of each line, to recognize it on a later import: the bank's id when the file has
 * one, otherwise date, amount and description, numbered so identical lines in one file (two
 * coffees on the same day) stay apart.
 */
fun importKeys(lines: List<StatementLine>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return lines.map { line ->
        val base = line.externalId?.let { "id:$it" }
            ?: "line:${line.date}|${line.amountCents}|${line.description.trim().lowercase()}"
        val n = seen.merge(base, 1, Int::plus)!!
        // Some banks repeat a FITID within one file; the first keeps the plain id.
        if (line.externalId != null && n == 1) base else "$base#$n"
    }
}

private val BALANCE = Regex("^(saldo\\b|s a l d o)", RegexOption.IGNORE_CASE)
private val CARD_BILL = Regex("(pag(amento|to)?|pgto|deb(ito)?\\.?\\s*aut).*fatura|fatura.*(cart|pag)", RegexOption.IGNORE_CASE)

private fun hintFor(line: StatementLine, card: CreditCard?): ImportHint? = when {
    BALANCE.containsMatchIn(line.description.trim()) -> ImportHint.BALANCE
    card == null && line.amountCents < 0 && CARD_BILL.containsMatchIn(line.description) -> ImportHint.CARD_BILL_PAYMENT
    else -> null
}

/**
 * Suggests how to import [lines]: money out as a flexible expense, money in as income. Into a
 * [card], money in (the invoice payment, refunds) starts unselected, since the payment is not an
 * expense or income of its own. Lines whose key is in [importedKeys], or matching an [existing]
 * entry of the same date and amount (typed by hand, or imported from another file), start
 * unselected too, as do balances and card bill payments.
 */
fun planImport(
    lines: List<StatementLine>,
    importedKeys: Set<String>,
    existing: List<Entry>,
    card: CreditCard?,
): List<ImportCandidate> {
    val fileKeys = importKeys(lines).toSet()
    val manual = existing.filter { it.importKey == null || it.importKey !in fileKeys }
        .map { Triple(it.occurredOn, it.amountCents, it.type.isInflow) }
        .groupingBy { it }.eachCount().toMutableMap()
    return lines.zip(importKeys(lines)) { line, key ->
        val inflow = line.amountCents > 0
        val duplicate = when {
            key in importedKeys -> DuplicateKind.ALREADY_IMPORTED
            else -> {
                // Each existing entry explains one statement line at most.
                val match = Triple(line.date, line.amountCents.absoluteValue, inflow)
                val left = manual[match] ?: 0
                if (left > 0) {
                    manual[match] = left - 1
                    DuplicateKind.POSSIBLE
                } else {
                    null
                }
            }
        }
        val hint = hintFor(line, card)
        ImportCandidate(
            line = line,
            key = key,
            type = if (inflow) EntryType.INCOME else EntryType.FLEXIBLE_EXPENSE,
            selected = duplicate == null && hint == null && line.amountCents != 0L && !(card != null && inflow),
            duplicate = duplicate,
            hint = hint,
        )
    }
}

/**
 * The confirmed entry for a candidate. Expenses go on [card] when importing a card statement; the
 * sign of the line is dropped, since the type carries the direction.
 */
fun ImportCandidate.toEntry(card: CreditCard?): Entry =
    newEntry(
        type = type,
        amountCents = line.amountCents.absoluteValue,
        description = line.description,
        occurredOn = line.date,
        card = card?.takeIf { type.canUseCard },
        categoryId = categoryId,
    ).copy(importKey = key)
