package io.github.igornunes.yourfinances.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.igornunes.yourfinances.data.db.dao.BackupDao
import io.github.igornunes.yourfinances.data.db.dao.CategoryDao
import io.github.igornunes.yourfinances.data.db.dao.CreditCardDao
import io.github.igornunes.yourfinances.data.db.dao.EntryDao
import io.github.igornunes.yourfinances.data.db.dao.InstallmentPurchaseDao
import io.github.igornunes.yourfinances.data.db.dao.InvestmentGoalDao
import io.github.igornunes.yourfinances.data.db.dao.InvoicePaymentDao
import io.github.igornunes.yourfinances.data.db.dao.RecurrenceDao
import io.github.igornunes.yourfinances.data.db.dao.SavingsPocketDao
import io.github.igornunes.yourfinances.data.db.entity.CategoryEntity
import io.github.igornunes.yourfinances.data.db.entity.CreditCardEntity
import io.github.igornunes.yourfinances.data.db.entity.EntryEntity
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentGoalEntity
import io.github.igornunes.yourfinances.data.db.entity.InvoicePaymentEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceSkipEntity
import io.github.igornunes.yourfinances.data.db.entity.SavingsPocketEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        CategoryEntity::class,
        CreditCardEntity::class,
        RecurrenceEntity::class,
        RecurrenceSkipEntity::class,
        InstallmentPurchaseEntity::class,
        EntryEntity::class,
        InvoicePaymentEntity::class,
        InvestmentGoalEntity::class,
        SavingsPocketEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun recurrenceDao(): RecurrenceDao
    abstract fun installmentPurchaseDao(): InstallmentPurchaseDao
    abstract fun entryDao(): EntryDao
    abstract fun invoicePaymentDao(): InvoicePaymentDao
    abstract fun investmentGoalDao(): InvestmentGoalDao
    abstract fun backupDao(): BackupDao
    abstract fun savingsPocketDao(): SavingsPocketDao

    companion object {
        const val NAME = "your_finances.db"

        /** Opens the SQLCipher-encrypted database. Schema changes always need a written Migration. */
        fun buildEncrypted(context: Context, passphrase: ByteArray, name: String = NAME): AppDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, AppDatabase::class.java, name)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }
    }
}
