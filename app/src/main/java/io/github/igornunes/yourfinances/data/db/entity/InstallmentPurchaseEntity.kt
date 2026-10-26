package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import java.time.LocalDate

@Entity(
    tableName = "installment_purchases",
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
    ],
    indices = [Index("category_id"), Index("credit_card_id")],
)
data class InstallmentPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
    @ColumnInfo(name = "installment_count") val installmentCount: Int,
    @ColumnInfo(name = "purchased_on") val purchasedOn: LocalDate,
    val type: EntryType,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "credit_card_id") val creditCardId: Long?,
)

fun InstallmentPurchaseEntity.toDomain() = InstallmentPurchase(
    id = id,
    description = description,
    totalCents = totalCents,
    installmentCount = installmentCount,
    purchasedOn = purchasedOn,
    type = type,
    categoryId = categoryId,
    creditCardId = creditCardId,
)

fun InstallmentPurchase.toEntity() = InstallmentPurchaseEntity(
    id = id,
    description = description,
    totalCents = totalCents,
    installmentCount = installmentCount,
    purchasedOn = purchasedOn,
    type = type,
    categoryId = categoryId,
    creditCardId = creditCardId,
)
