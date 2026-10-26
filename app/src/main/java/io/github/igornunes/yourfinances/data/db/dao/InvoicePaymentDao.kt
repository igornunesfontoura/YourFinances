package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.igornunes.yourfinances.data.db.entity.InvoicePaymentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface InvoicePaymentDao {
    @Upsert
    suspend fun upsert(payment: InvoicePaymentEntity)

    @Query("DELETE FROM invoice_payments WHERE card_id = :cardId AND due_date = :dueDate")
    suspend fun delete(cardId: Long, dueDate: LocalDate)

    @Query("SELECT * FROM invoice_payments")
    fun observeAll(): Flow<List<InvoicePaymentEntity>>
}
