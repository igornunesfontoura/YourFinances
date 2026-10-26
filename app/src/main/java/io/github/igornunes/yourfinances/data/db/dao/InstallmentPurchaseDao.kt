package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunes.yourfinances.data.db.entity.PurchaseWithInstallments
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentPurchaseDao {
    @Insert
    suspend fun insert(purchase: InstallmentPurchaseEntity): Long

    @Delete
    suspend fun delete(purchase: InstallmentPurchaseEntity)

    @Query("SELECT * FROM installment_purchases WHERE id = :id")
    suspend fun getById(id: Long): InstallmentPurchaseEntity?

    @Query("DELETE FROM installment_purchases WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM installment_purchases ORDER BY purchased_on DESC, id DESC")
    fun observeAllWithInstallments(): Flow<List<PurchaseWithInstallments>>
}
