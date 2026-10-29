package io.github.igornunes.yourfinances.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.igornunes.yourfinances.data.db.entity.InvestmentGoalEntity
import io.github.igornunes.yourfinances.data.db.entity.SavingsPocketEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import io.github.igornunes.yourfinances.domain.model.CategoryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/** Every schema version must migrate with its data intact; no destructive fallback exists. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2KeepsDataAndAddsInvoicePayments() = runTest {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO credit_cards (id, name, closing_day, due_day) VALUES (1, 'Nubank', 25, 5)")
            execSQL(
                "INSERT INTO entries (id, type, amount_cents, description, occurred_on, paid_on, status, credit_card_id) " +
                    "VALUES (1, 'FLEXIBLE_EXPENSE', 4590, 'Jantar', 20500, 20520, 'CONFIRMED', 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).apply {
            execSQL("INSERT INTO invoice_payments (card_id, due_date, paid_on) VALUES (1, 20520, 20519)")
            close()
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            val payments = database.invoicePaymentDao().observeAll().first()
            assertEquals(1, payments.size)
            assertEquals(4590L, database.entryDao().getById(1)!!.amountCents)
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate2To3KeepsDataAndAddsInvestmentGoals() = runTest {
        helper.createDatabase(name, 2).apply {
            execSQL(
                "INSERT INTO entries (id, type, amount_cents, description, occurred_on, paid_on, status) " +
                    "VALUES (1, 'INCOME', 500000, 'Salário', 20500, 20500, 'CONFIRMED')",
            )
            close()
        }

        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            database.investmentGoalDao().upsert(InvestmentGoalEntity(YearMonth.of(2026, 3), 100000))
            assertEquals(1, database.investmentGoalDao().observeAll().first().size)
            assertEquals(500000L, database.entryDao().getById(1)!!.amountCents)
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate3To4KeepsEntriesAndAddsPockets() = runTest {
        helper.createDatabase(name, 3).apply {
            execSQL(
                "INSERT INTO entries (id, type, amount_cents, description, occurred_on, paid_on, status) " +
                    "VALUES (1, 'INVESTMENT', 100000, 'Aporte', 20500, 20500, 'CONFIRMED')",
            )
            execSQL("INSERT INTO rates (series_code, date, value_percent) VALUES (12, 20500, '0.04')")
            close()
        }

        helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            val pocketId = database.savingsPocketDao().insert(SavingsPocketEntity(name = "Reserva", targetCents = null))
            val entry = database.entryDao().getById(1)!!
            assertEquals(null, entry.savingsPocketId)
            database.entryDao().update(entry.copy(savingsPocketId = pocketId))
            assertEquals(pocketId, database.entryDao().getById(1)!!.savingsPocketId)
            // RESTRICT holds on the migrated table: a pocket in use cannot be deleted.
            val deleted = runCatching { database.savingsPocketDao().delete(SavingsPocketEntity(pocketId, "Reserva", null)) }
            assertTrue(deleted.isFailure)
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate4To5TurnsCategoryTypesIntoKinds() = runTest {
        helper.createDatabase(name, 4).apply {
            execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Mercado', 'FLEXIBLE_EXPENSE')")
            execSQL("INSERT INTO categories (id, name, type) VALUES (2, 'Salário', 'INCOME')")
            execSQL("INSERT INTO credit_cards (id, name, closing_day, due_day) VALUES (1, 'Nubank', 25, 5)")
            close()
        }

        helper.runMigrationsAndValidate(name, 5, true, MIGRATION_4_5).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            val categories = database.categoryDao().observeAll().first().associateBy { it.id }
            assertEquals(CategoryKind.EXPENSE, categories.getValue(1).kind)
            assertEquals(CategoryKind.INCOME, categories.getValue(2).kind)
            assertEquals(null, categories.getValue(1).monthlyLimitCents)
            // Migrated databases are not seeded: only the user's two categories are there.
            assertEquals(2, categories.size)
        } finally {
            database.close()
        }
    }
}
