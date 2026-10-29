package io.github.igornunes.yourfinances.data.backup

import io.github.igornunes.yourfinances.data.db.entity.CategoryEntity
import io.github.igornunes.yourfinances.data.db.entity.CreditCardEntity
import io.github.igornunes.yourfinances.data.db.entity.EntryEntity
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentGoalEntity
import io.github.igornunes.yourfinances.data.db.entity.InvoicePaymentEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceSkipEntity
import io.github.igornunes.yourfinances.data.db.entity.toDomain
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.Frequency
import io.github.igornunes.yourfinances.domain.model.Indexer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

/** Every table the user owns, as stored in the database. The BCB rate cache is left out. */
data class BackupTables(
    val categories: List<CategoryEntity>,
    val creditCards: List<CreditCardEntity>,
    val recurrences: List<RecurrenceEntity>,
    val recurrenceSkips: List<RecurrenceSkipEntity>,
    val installmentPurchases: List<InstallmentPurchaseEntity>,
    val entries: List<EntryEntity>,
    val invoicePayments: List<InvoicePaymentEntity>,
    val investments: List<InvestmentEntity>,
    val investmentGoals: List<InvestmentGoalEntity>,
)

/**
 * The decrypted backup content: versioned JSON, independent of the Room schema. Dates are ISO
 * strings, money is in cents, rates are decimal strings.
 */
object BackupFormat {
    const val VERSION = 1

    private val json = Json { encodeDefaults = true }

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
            json.decodeFromString(BackupSnapshot.serializer(), bytes.toString(Charsets.UTF_8))
        } catch (e: SerializationException) {
            throw BackupInvalidException(e)
        } catch (e: IllegalArgumentException) {
            throw BackupInvalidException(e)
        } catch (e: DateTimeException) {
            throw BackupInvalidException(e)
        }
        if (snapshot.format != VERSION) throw BackupInvalidException()
        return try {
            snapshot.toTables().also { it.validate() }
        } catch (e: IllegalArgumentException) {
            throw BackupInvalidException(e)
        }
    }

    private fun BackupTables.validate() {
        installmentPurchases.forEach { it.toDomain() }
        recurrences.forEach { it.toDomain() }
        creditCards.forEach { it.toDomain() }
        entries.forEach { it.toDomain() }
        investments.forEach { it.toDomain() }
        investmentGoals.forEach { it.toDomain() }
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
    val investments: List<InvestmentRow>,
    val investmentGoals: List<InvestmentGoalRow>,
)

@Serializable
private data class CategoryRow(val id: Long, val name: String, val type: EntryType)

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
)

@Serializable
private data class InvoicePaymentRow(
    val cardId: Long,
    @Serializable(LocalDateSerializer::class) val dueDate: LocalDate,
    @Serializable(LocalDateSerializer::class) val paidOn: LocalDate,
)

@Serializable
private data class InvestmentRow(
    val id: Long,
    val product: String,
    val indexer: Indexer,
    @Serializable(BigDecimalSerializer::class) val rate: BigDecimal,
    @Serializable(LocalDateSerializer::class) val investedOn: LocalDate,
    val principalCents: Long,
)

@Serializable
private data class InvestmentGoalRow(
    @Serializable(YearMonthSerializer::class) val startsIn: YearMonth,
    val amountCents: Long,
)

private fun BackupTables.toSnapshot(exportedOn: LocalDate) = BackupSnapshot(
    format = BackupFormat.VERSION,
    exportedOn = exportedOn,
    categories = categories.map { CategoryRow(it.id, it.name, it.type) },
    creditCards = creditCards.map { CreditCardRow(it.id, it.name, it.closingDay, it.dueDay) },
    recurrences = recurrences.map {
        RecurrenceRow(it.id, it.type, it.amountCents, it.description, it.frequency, it.dayOfMonth, it.startsOn, it.endsOn, it.categoryId)
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
            it.installmentCount,
        )
    },
    invoicePayments = invoicePayments.map { InvoicePaymentRow(it.cardId, it.dueDate, it.paidOn) },
    investments = investments.map { InvestmentRow(it.id, it.product, it.indexer, it.rate, it.investedOn, it.principalCents) },
    investmentGoals = investmentGoals.map { InvestmentGoalRow(it.startsIn, it.amountCents) },
)

private fun BackupSnapshot.toTables() = BackupTables(
    categories = categories.map { CategoryEntity(it.id, it.name, it.type) },
    creditCards = creditCards.map { CreditCardEntity(it.id, it.name, it.closingDay, it.dueDay) },
    recurrences = recurrences.map {
        RecurrenceEntity(it.id, it.type, it.amountCents, it.description, it.frequency, it.dayOfMonth, it.startsOn, it.endsOn, it.categoryId)
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
            it.installmentCount,
        )
    },
    invoicePayments = invoicePayments.map { InvoicePaymentEntity(it.cardId, it.dueDate, it.paidOn) },
    investments = investments.map { InvestmentEntity(it.id, it.product, it.indexer, it.rate, it.investedOn, it.principalCents) },
    investmentGoals = investmentGoals.map { InvestmentGoalEntity(it.startsIn, it.amountCents) },
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

/** Plain decimal string, never scientific notation, so no precision is lost. */
private object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}
