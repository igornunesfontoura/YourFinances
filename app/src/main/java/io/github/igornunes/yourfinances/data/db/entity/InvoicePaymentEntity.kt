package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import io.github.igornunes.yourfinances.domain.card.InvoicePayment
import java.time.LocalDate

/** An invoice the user marked as paid, identified by its card and due date. */
@Entity(
    tableName = "invoice_payments",
    primaryKeys = ["card_id", "due_date"],
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InvoicePaymentEntity(
    @ColumnInfo(name = "card_id") val cardId: Long,
    @ColumnInfo(name = "due_date") val dueDate: LocalDate,
    @ColumnInfo(name = "paid_on") val paidOn: LocalDate,
)

fun InvoicePaymentEntity.toDomain() = InvoicePayment(cardId = cardId, dueDate = dueDate, paidOn = paidOn)

fun InvoicePayment.toEntity() = InvoicePaymentEntity(cardId = cardId, dueDate = dueDate, paidOn = paidOn)
