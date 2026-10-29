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

/**
 * v4: savings pockets replace yield estimates. Contributions and withdrawals (and their
 * recurrences) may name a pocket;
 * the unused `investments` and `rates` (BCB cache) tables go away, so the app makes no network
 * request at all.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `savings_pockets` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `target_cents` INTEGER)",
        )
        db.execSQL(
            "ALTER TABLE `entries` ADD COLUMN `savings_pocket_id` INTEGER DEFAULT NULL " +
                "REFERENCES `savings_pockets`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_savings_pocket_id` ON `entries` (`savings_pocket_id`)")
        db.execSQL(
            "ALTER TABLE `recurrences` ADD COLUMN `savings_pocket_id` INTEGER DEFAULT NULL " +
                "REFERENCES `savings_pockets`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrences_savings_pocket_id` ON `recurrences` (`savings_pocket_id`)")
        db.execSQL("DROP TABLE IF EXISTS `investments`")
        db.execSQL("DROP TABLE IF EXISTS `rates`")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
