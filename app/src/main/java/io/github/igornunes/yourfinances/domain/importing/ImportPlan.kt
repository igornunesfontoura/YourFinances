package io.github.igornunes.yourfinances.domain.importing

import io.github.igornunes.yourfinances.domain.entry.newEntry
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import kotlin.math.absoluteValue

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
)

/**
 * Stable identity of each line, to recognize it on a later import: the bank's id when the file has
 * one, otherwise date, amount and description, numbered so identical lines in one file (two
 * coffees on the same day) stay apart.
 */
fun importKeys(lines: List<StatementLine>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return lines.map { line ->
        line.externalId?.let { "id:$it" } ?: run {
            val base = "line:${line.date}|${line.amountCents}|${line.description.trim().lowercase()}"
            val n = seen.merge(base, 1, Int::plus)!!
            "$base#$n"
        }
    }
}

/**
 * Suggests how to import [lines]: money out as a flexible expense, money in as income. Into a
 * [card], money in (the invoice payment, refunds) starts unselected, since the payment is not an
 * expense or income of its own. Lines whose key is in [importedKeys], or matching an [existing]
 * entry of the same dates that was not imported, start unselected too.
 */
fun planImport(
    lines: List<StatementLine>,
    importedKeys: Set<String>,
    existing: List<Entry>,
    card: CreditCard?,
): List<ImportCandidate> {
    val manual = existing.filter { it.importKey == null }
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
        ImportCandidate(
            line = line,
            key = key,
            type = if (inflow) EntryType.INCOME else EntryType.FLEXIBLE_EXPENSE,
            selected = duplicate == null && line.amountCents != 0L && !(card != null && inflow),
            duplicate = duplicate,
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
