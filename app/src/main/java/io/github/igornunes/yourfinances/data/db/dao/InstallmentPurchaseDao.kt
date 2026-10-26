package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity

@Dao
interface InstallmentPurchaseDao {
    @Insert
    suspend fun insert(purchase: InstallmentPurchaseEntity): Long

    @Delete
    suspend fun delete(purchase: InstallmentPurchaseEntity)

    @Query("SELECT * FROM installment_purchases WHERE id = :id")
    suspend fun getById(id: Long): InstallmentPurchaseEntity?
}
