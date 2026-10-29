package io.github.igornunes.yourfinances.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2: invoices can be marked as paid. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `invoice_payments` (" +
                "`card_id` INTEGER NOT NULL, `due_date` INTEGER NOT NULL, `paid_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`card_id`, `due_date`), " +
                "FOREIGN KEY(`card_id`) REFERENCES `credit_cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }
}

/** v3: monthly investment goals, each in force from its starting month. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `investment_goals` (" +
                "`starts_in` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, PRIMARY KEY(`starts_in`))",
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
