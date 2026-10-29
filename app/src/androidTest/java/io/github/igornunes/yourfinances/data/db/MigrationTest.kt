package io.github.igornunes.yourfinances.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.igornunes.yourfinances.data.db.entity.InvestmentGoalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
