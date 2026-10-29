package io.github.igornunes.yourfinances.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.igornunes.yourfinances.domain.model.CategoryKind

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

/**
 * v5: categories are income or expense (the `type` column now holds a [CategoryKind] name) and
 * expense categories may have a monthly limit; recurring expenses may be charged to a card.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE `categories` SET `type` = 'EXPENSE' WHERE `type` <> 'INCOME'")
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `monthly_limit_cents` INTEGER DEFAULT NULL")
        db.execSQL(
            "ALTER TABLE `recurrences` ADD COLUMN `credit_card_id` INTEGER DEFAULT NULL " +
                "REFERENCES `credit_cards`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrences_credit_card_id` ON `recurrences` (`credit_card_id`)")
    }
}

/** v6: imported entries remember the statement line they came from, so it is not imported twice. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `entries` ADD COLUMN `import_key` TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_import_key` ON `entries` (`import_key`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
