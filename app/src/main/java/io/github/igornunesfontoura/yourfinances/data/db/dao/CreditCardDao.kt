package io.github.igornunesfontoura.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.igornunesfontoura.yourfinances.data.db.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Insert
    suspend fun insert(card: CreditCardEntity): Long

    @Update
    suspend fun update(card: CreditCardEntity)

    @Delete
    suspend fun delete(card: CreditCardEntity)

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCardEntity?

    @Query("SELECT * FROM credit_cards ORDER BY name")
    fun observeAll(): Flow<List<CreditCardEntity>>
}
