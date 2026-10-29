package io.github.igornunes.yourfinances.data.backup

import io.github.igornunes.yourfinances.data.db.entity.CategoryEntity
import io.github.igornunes.yourfinances.data.db.entity.CreditCardEntity
import io.github.igornunes.yourfinances.data.db.entity.EntryEntity
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentGoalEntity
import io.github.igornunes.yourfinances.data.db.entity.InvoicePaymentEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceSkipEntity
import io.github.igornunes.yourfinances.data.db.entity.SavingsPocketEntity
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.domain.model.CategoryKind
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

/** Every table the user owns, as stored in the database. */
data class BackupTables(
    val categories: List<CategoryEntity>,
    val creditCards: List<CreditCardEntity>,
    val recurrences: List<RecurrenceEntity>,
    val recurrenceSkips: List<RecurrenceSkipEntity>,
    val installmentPurchases: List<InstallmentPurchaseEntity>,
    val entries: List<EntryEntity>,
    val invoicePayments: List<InvoicePaymentEntity>,
    val investmentGoals: List<InvestmentGoalEntity>,
    val savingsPockets: List<SavingsPocketEntity>,
)

/**
 * The decrypted backup content: versioned JSON, independent of the Room schema. Dates are ISO
 * strings and money is in cents.
 *
 * Versions: 1 had an (always empty) `investments` list; 2 drops it and adds savings pockets; 3
 * adds category limits and cards on recurrences, and categories are `INCOME` or `EXPENSE` (older
 * files hold an entry type there, read as its kind); 4 adds the import key of imported entries.
 */
object BackupFormat {
    const val VERSION = 4

    private val json = Json { encodeDefaults = true }

    /** Version 1 only differs by the `investments` list, which no version ever filled. */
    private val jsonV1 = Json { ignoreUnknownKeys = true }

    fun encode(tables: BackupTables, exportedOn: LocalDate): ByteArray =
        json.encodeToString(BackupSnapshot.serializer(), tables.toSnapshot(exportedOn)).toByteArray(Charsets.UTF_8)

    /**
     * Parses and checks every row against the domain rules, so nothing invalid reaches the
     * database. Foreign keys are checked by the database itself when restoring.
     *
     * @throws BackupInvalidException when the content is malformed, breaks a rule or comes from a
     *   newer version of the app.
     */
    fun decode(bytes: ByteArray): BackupTables {
        val snapshot = try {
            val text = bytes.toString(Charsets.UTF_8)
            val format = json.parseToJsonElement(text).jsonObject["format"]?.jsonPrimitive?.int
            val parser = when (format) {
                VERSION, 2, 3 -> json
                1 -> jsonV1
                else -> throw BackupInvalidException()
            }
            parser.decodeFromString(BackupSnapshot.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupInvalidException(e)
        } catch (e: IllegalArgumentException) {
            throw BackupInvalidException(e)
        } catch (e: DateTimeException) {
            throw BackupInvalidException(e)
        }

        return try {
            snapshot.toTables().also { it.validate() }
        } catch (e: IllegalArgumentException) {
            throw BackupInvalidException(e)
        }
    }

    private fun BackupTables.validate() {
        categories.forEach { it.toDomain() }
        // References are checked here so a bad file reads as invalid, not as a failed restore.
        val pocketIds = savingsPockets.map { it.id }.toSet()
        require((entries.mapNotNull { it.savingsPocketId } + recurrences.mapNotNull { it.savingsPocketId }).all { it in pocketIds }) {
            "unknown savings pocket"
        }
        val cardIds = creditCards.map { it.id }.toSet()
        val cardRefs = entries.mapNotNull { it.creditCardId } + recurrences.mapNotNull { it.creditCardId } +
            installmentPurchases.mapNotNull { it.creditCardId } + invoicePayments.map { it.cardId }
        require(cardRefs.all { it in cardIds }) { "unknown credit card" }
        val categoryIds = categories.map { it.id }.toSet()
        val categoryRefs = entries.mapNotNull { it.categoryId } + recurrences.mapNotNull { it.categoryId } +
            installmentPurchases.mapNotNull { it.categoryId }
        require(categoryRefs.all { it in categoryIds }) { "unknown category" }
        installmentPurchases.forEach { it.toDomain() }
        recurrences.forEach { it.toDomain() }
        creditCards.forEach { it.toDomain() }
        entries.forEach { it.toDomain() }
        investmentGoals.forEach { it.toDomain() }
        savingsPockets.forEach { it.toDomain() }
    }
}

/** The backup content is not something this version can restore. */
class BackupInvalidException(cause: Throwable? = null) : Exception("backup content is invalid", cause)

@Serializable
private data class BackupSnapshot(
    val format: Int,
    @Serializable(LocalDateSerializer::class) val exportedOn: LocalDate,
    val categories: List<CategoryRow>,
    val creditCards: List<CreditCardRow>,
    val recurrences: List<RecurrenceRow>,
    val recurrenceSkips: List<RecurrenceSkipRow>,
    val installmentPurchases: List<InstallmentPurchaseRow>,
    val entries: List<EntryRow>,
    val invoicePayments: List<InvoicePaymentRow>,
    val investmentGoals: List<InvestmentGoalRow>,
    val savingsPockets: List<SavingsPocketRow> = emptyList(),
)

@Serializable
private data class CategoryRow(val id: Long, val name: String, val type: String, val monthlyLimitCents: Long? = null) {
    val kind: CategoryKind
        get() = when (type) {
            "INCOME" -> CategoryKind.INCOME
            // Formats before 3 stored any entry type here; the v5 migration reads them the same way.
            "EXPENSE", "FIXED_EXPENSE", "FLEXIBLE_EXPENSE", "INVESTMENT", "WITHDRAWAL" -> CategoryKind.EXPENSE
            else -> throw IllegalArgumentException("unknown category kind")
        }
}

@Serializable
private data class CreditCardRow(val id: Long, val name: String, val closingDay: Int, val dueDay: Int)

@Serializable
private data class RecurrenceRow(
    val id: Long,
    val type: EntryType,
    val amountCents: Long,
    val description: String,
    val frequency: Frequency,
    val dayOfMonth: Int,
    @Serializable(LocalDateSerializer::class) val startsOn: LocalDate,
    @Serializable(LocalDateSerializer::class) val endsOn: LocalDate?,
    val categoryId: Long?,
    val savingsPocketId: Long? = null,
    val creditCardId: Long? = null,
)

@Serializable
private data class RecurrenceSkipRow(val recurrenceId: Long, @Serializable(LocalDateSerializer::class) val slot: LocalDate)

@Serializable
private data class InstallmentPurchaseRow(
    val id: Long,
    val description: String,
    val totalCents: Long,
    val installmentCount: Int,
    @Serializable(LocalDateSerializer::class) val purchasedOn: LocalDate,
    val type: EntryType,
    val categoryId: Long?,
    val creditCardId: Long?,
)

@Serializable
private data class EntryRow(
    val id: Long,
    val type: EntryType,
    val amountCents: Long,
    val description: String,
    @Serializable(LocalDateSerializer::class) val occurredOn: LocalDate,
    @Serializable(LocalDateSerializer::class) val paidOn: LocalDate,
    val status: EntryStatus,
    val categoryId: Long?,
    val creditCardId: Long?,
    val recurrenceId: Long?,
    @Serializable(LocalDateSerializer::class) val scheduledFor: LocalDate?,
    val installmentPurchaseId: Long?,
    val installmentNumber: Int?,
    val installmentCount: Int?,
    val savingsPocketId: Long? = null,
    val importKey: String? = null,
)

@Serializable
private data class InvoicePaymentRow(
    val cardId: Long,
    @Serializable(LocalDateSerializer::class) val dueDate: LocalDate,
    @Serializable(LocalDateSerializer::class) val paidOn: LocalDate,
)

@Serializable
private data class InvestmentGoalRow(
    @Serializable(YearMonthSerializer::class) val startsIn: YearMonth,
    val amountCents: Long,
)

@Serializable
private data class SavingsPocketRow(val id: Long, val name: String, val targetCents: Long?)

private fun BackupTables.toSnapshot(exportedOn: LocalDate) = BackupSnapshot(
    format = BackupFormat.VERSION,
    exportedOn = exportedOn,
    categories = categories.map { CategoryRow(it.id, it.name, it.kind.name, it.monthlyLimitCents) },
    creditCards = creditCards.map { CreditCardRow(it.id, it.name, it.closingDay, it.dueDay) },
    recurrences = recurrences.map {
        RecurrenceRow(
            it.id, it.type, it.amountCents, it.description, it.frequency, it.dayOfMonth, it.startsOn, it.endsOn,
            it.categoryId, it.savingsPocketId, it.creditCardId,
        )
    },
    recurrenceSkips = recurrenceSkips.map { RecurrenceSkipRow(it.recurrenceId, it.slot) },
    installmentPurchases = installmentPurchases.map {
        InstallmentPurchaseRow(
            it.id, it.description, it.totalCents, it.installmentCount, it.purchasedOn, it.type, it.categoryId, it.creditCardId,
        )
    },
    entries = entries.map {
        EntryRow(
            it.id, it.type, it.amountCents, it.description, it.occurredOn, it.paidOn, it.status, it.categoryId,
            it.creditCardId, it.recurrenceId, it.scheduledFor, it.installmentPurchaseId, it.installmentNumber,
            it.installmentCount, it.savingsPocketId, it.importKey,
        )
    },
    invoicePayments = invoicePayments.map { InvoicePaymentRow(it.cardId, it.dueDate, it.paidOn) },
    investmentGoals = investmentGoals.map { InvestmentGoalRow(it.startsIn, it.amountCents) },
    savingsPockets = savingsPockets.map { SavingsPocketRow(it.id, it.name, it.targetCents) },
)

private fun BackupSnapshot.toTables() = BackupTables(
    categories = categories.map { CategoryEntity(it.id, it.name, it.kind, it.monthlyLimitCents) },
    creditCards = creditCards.map { CreditCardEntity(it.id, it.name, it.closingDay, it.dueDay) },
    recurrences = recurrences.map {
        RecurrenceEntity(
            it.id, it.type, it.amountCents, it.description, it.frequency, it.dayOfMonth, it.startsOn, it.endsOn,
            it.categoryId, it.savingsPocketId, it.creditCardId,
        )
    },
    recurrenceSkips = recurrenceSkips.map { RecurrenceSkipEntity(it.recurrenceId, it.slot) },
    installmentPurchases = installmentPurchases.map {
        InstallmentPurchaseEntity(
            it.id, it.description, it.totalCents, it.installmentCount, it.purchasedOn, it.type, it.categoryId, it.creditCardId,
        )
    },
    entries = entries.map {
        EntryEntity(
            it.id, it.type, it.amountCents, it.description, it.occurredOn, it.paidOn, it.status, it.categoryId,
            it.creditCardId, it.recurrenceId, it.scheduledFor, it.installmentPurchaseId, it.installmentNumber,
            it.installmentCount, it.savingsPocketId, it.importKey,
        )
    },
    invoicePayments = invoicePayments.map { InvoicePaymentEntity(it.cardId, it.dueDate, it.paidOn) },
    investmentGoals = investmentGoals.map { InvestmentGoalEntity(it.startsIn, it.amountCents) },
    savingsPockets = savingsPockets.map { SavingsPocketEntity(it.id, it.name, it.targetCents) },
)

private abstract class StringSerializer<T : Any>(name: String, private val parse: (String) -> T) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): T = parse(decoder.decodeString())
}

/** "2026-03-05". */
private object LocalDateSerializer : StringSerializer<LocalDate>("LocalDate", { LocalDate.parse(it) })

/** "2026-03". */
private object YearMonthSerializer : StringSerializer<YearMonth>("YearMonth", { YearMonth.parse(it) })
