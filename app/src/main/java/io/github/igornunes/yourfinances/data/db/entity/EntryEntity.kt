package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryStatus
import io.github.igornunes.yourfinances.domain.model.EntryType
import java.time.LocalDate

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["credit_card_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        // Deleting a recurrence keeps the history it generated.
        ForeignKey(
            entity = RecurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrence_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        // Installments only exist as part of their purchase.
        ForeignKey(
            entity = InstallmentPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["installment_purchase_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("occurred_on"),
        Index("paid_on"),
        Index("category_id"),
        Index("credit_card_id"),
        Index("installment_purchase_id"),
        Index(value = ["recurrence_id", "scheduled_for"], unique = true),
    ],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val description: String,
    @ColumnInfo(name = "occurred_on") val occurredOn: LocalDate,
    @ColumnInfo(name = "paid_on") val paidOn: LocalDate,
    val status: EntryStatus,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "credit_card_id") val creditCardId: Long?,
    @ColumnInfo(name = "recurrence_id") val recurrenceId: Long?,
    @ColumnInfo(name = "scheduled_for") val scheduledFor: LocalDate?,
    @ColumnInfo(name = "installment_purchase_id") val installmentPurchaseId: Long?,
    @ColumnInfo(name = "installment_number") val installmentNumber: Int?,
    @ColumnInfo(name = "installment_count") val installmentCount: Int?,
)

fun EntryEntity.toDomain() = Entry(
    id = id,
    type = type,
    amountCents = amountCents,
    description = description,
    occurredOn = occurredOn,
    paidOn = paidOn,
    status = status,
    categoryId = categoryId,
    creditCardId = creditCardId,
    recurrenceId = recurrenceId,
    // The slot is meaningless once its recurrence was deleted (recurrence_id SET NULL).
    scheduledFor = scheduledFor.takeIf { recurrenceId != null },
    installmentPurchaseId = installmentPurchaseId,
    installmentNumber = installmentNumber,
    installmentCount = installmentCount,
)

fun Entry.toEntity() = EntryEntity(
    id = id,
    type = type,
    amountCents = amountCents,
    description = description,
    occurredOn = occurredOn,
    paidOn = paidOn,
    status = status,
    categoryId = categoryId,
    creditCardId = creditCardId,
    recurrenceId = recurrenceId,
    scheduledFor = scheduledFor,
    installmentPurchaseId = installmentPurchaseId,
    installmentNumber = installmentNumber,
    installmentCount = installmentCount,
)
