package io.github.igornunes.yourfinances.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.igornunes.yourfinances.data.db.dao.CategoryDao
import io.github.igornunes.yourfinances.data.db.dao.CreditCardDao
import io.github.igornunes.yourfinances.data.db.dao.EntryDao
import io.github.igornunes.yourfinances.data.db.dao.InstallmentPurchaseDao
import io.github.igornunes.yourfinances.data.db.dao.InvestmentDao
import io.github.igornunes.yourfinances.data.db.dao.RateDao
import io.github.igornunes.yourfinances.data.db.dao.RecurrenceDao
import io.github.igornunes.yourfinances.data.db.entity.CategoryEntity
import io.github.igornunes.yourfinances.data.db.entity.CreditCardEntity
import io.github.igornunes.yourfinances.data.db.entity.EntryEntity
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentEntity
import io.github.igornunes.yourfinances.data.db.entity.RateEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceSkipEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        CategoryEntity::class,
        CreditCardEntity::class,
        RecurrenceEntity::class,
        RecurrenceSkipEntity::class,
        InstallmentPurchaseEntity::class,
        EntryEntity::class,
        InvestmentEntity::class,
        RateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun recurrenceDao(): RecurrenceDao
    abstract fun installmentPurchaseDao(): InstallmentPurchaseDao
    abstract fun entryDao(): EntryDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun rateDao(): RateDao

    companion object {
        const val NAME = "your_finances.db"

        /** Opens the SQLCipher-encrypted database. Schema changes always need a written Migration. */
        fun buildEncrypted(context: Context, passphrase: ByteArray): AppDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .build()
        }
    }
}
