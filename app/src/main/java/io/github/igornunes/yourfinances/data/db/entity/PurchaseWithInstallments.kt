package io.github.igornunes.yourfinances.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import io.github.igornunes.yourfinances.domain.installment.InstallmentPlan

data class PurchaseWithInstallments(
    @Embedded val purchase: InstallmentPurchaseEntity,
    @Relation(parentColumn = "id", entityColumn = "installment_purchase_id")
    val installments: List<EntryEntity>,
)

fun PurchaseWithInstallments.toDomain() = InstallmentPlan(
    purchase = purchase.toDomain(),
    installments = installments.map { it.toDomain() }.sortedBy { it.installmentNumber },
)
