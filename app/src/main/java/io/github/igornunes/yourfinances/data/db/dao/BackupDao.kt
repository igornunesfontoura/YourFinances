package io.github.igornunes.yourfinances.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.igornunes.yourfinances.data.backup.BackupTables
import io.github.igornunes.yourfinances.data.db.entity.CategoryEntity
import io.github.igornunes.yourfinances.data.db.entity.CreditCardEntity
import io.github.igornunes.yourfinances.data.db.entity.EntryEntity
import io.github.igornunes.yourfinances.data.db.entity.InstallmentPurchaseEntity
import io.github.igornunes.yourfinances.data.db.entity.InvestmentGoalEntity
import io.github.igornunes.yourfinances.data.db.entity.InvoicePaymentEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceEntity
import io.github.igornunes.yourfinances.data.db.entity.RecurrenceSkipEntity
import io.github.igornunes.yourfinances.data.db.entity.SavingsPocketEntity

/** Whole-database reads and the all-or-nothing replace used by backup restore. */
@Dao
abstract class BackupDao {
    @Query("SELECT * FROM categories ORDER BY id") abstract suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM credit_cards ORDER BY id") abstract suspend fun creditCards(): List<CreditCardEntity>

    @Query("SELECT * FROM recurrences ORDER BY id") abstract suspend fun recurrences(): List<RecurrenceEntity>

    @Query("SELECT * FROM recurrence_skips ORDER BY recurrence_id, slot")
    abstract suspend fun recurrenceSkips(): List<RecurrenceSkipEntity>

    @Query("SELECT * FROM installment_purchases ORDER BY id")
    abstract suspend fun installmentPurchases(): List<InstallmentPurchaseEntity>

    @Query("SELECT * FROM entries ORDER BY id") abstract suspend fun entries(): List<EntryEntity>

    @Query("SELECT * FROM invoice_payments ORDER BY card_id, due_date")
    abstract suspend fun invoicePayments(): List<InvoicePaymentEntity>

    @Query("SELECT * FROM savings_pockets ORDER BY id") abstract suspend fun savingsPockets(): List<SavingsPocketEntity>

    @Query("SELECT * FROM investment_goals ORDER BY starts_in")
    abstract suspend fun investmentGoals(): List<InvestmentGoalEntity>

    /** One transaction, so the copy is consistent even if something is saved meanwhile. */
    @Transaction
    open suspend fun readAll() = BackupTables(
        categories = categories(),
        creditCards = creditCards(),
        recurrences = recurrences(),
        recurrenceSkips = recurrenceSkips(),
        installmentPurchases = installmentPurchases(),
        entries = entries(),
        invoicePayments = invoicePayments(),
        investmentGoals = investmentGoals(),
        savingsPockets = savingsPockets(),
    )

    /**
     * Deletes the user's data and inserts [tables] keeping their ids. Children go first on delete
     * and last on insert; any broken reference or duplicate id rolls everything back.
     */
    @Transaction
    open suspend fun replaceAll(tables: BackupTables) {
        deleteEntries()
        deleteRecurrenceSkips()
        deleteInvoicePayments()
        deleteInstallmentPurchases()
        deleteRecurrences()
        deleteCreditCards()
        deleteCategories()
        deleteSavingsPockets()
        deleteInvestmentGoals()

        insertCategories(tables.categories)
        insertCreditCards(tables.creditCards)
        insertSavingsPockets(tables.savingsPockets)
        insertRecurrences(tables.recurrences)
        insertRecurrenceSkips(tables.recurrenceSkips)
        insertInstallmentPurchases(tables.installmentPurchases)
        insertEntries(tables.entries)
        insertInvoicePayments(tables.invoicePayments)
        insertInvestmentGoals(tables.investmentGoals)
    }

    @Query("DELETE FROM entries") protected abstract suspend fun deleteEntries()

    @Query("DELETE FROM recurrence_skips") protected abstract suspend fun deleteRecurrenceSkips()

    @Query("DELETE FROM invoice_payments") protected abstract suspend fun deleteInvoicePayments()

    @Query("DELETE FROM installment_purchases") protected abstract suspend fun deleteInstallmentPurchases()

    @Query("DELETE FROM recurrences") protected abstract suspend fun deleteRecurrences()

    @Query("DELETE FROM credit_cards") protected abstract suspend fun deleteCreditCards()

    @Query("DELETE FROM categories") protected abstract suspend fun deleteCategories()

    @Query("DELETE FROM savings_pockets") protected abstract suspend fun deleteSavingsPockets()

    @Query("DELETE FROM investment_goals") protected abstract suspend fun deleteInvestmentGoals()

    @Insert protected abstract suspend fun insertCategories(rows: List<CategoryEntity>)

    @Insert protected abstract suspend fun insertCreditCards(rows: List<CreditCardEntity>)

    @Insert protected abstract suspend fun insertRecurrences(rows: List<RecurrenceEntity>)

    @Insert protected abstract suspend fun insertRecurrenceSkips(rows: List<RecurrenceSkipEntity>)

    @Insert protected abstract suspend fun insertInstallmentPurchases(rows: List<InstallmentPurchaseEntity>)

    @Insert protected abstract suspend fun insertEntries(rows: List<EntryEntity>)

    @Insert protected abstract suspend fun insertInvoicePayments(rows: List<InvoicePaymentEntity>)

    @Insert protected abstract suspend fun insertSavingsPockets(rows: List<SavingsPocketEntity>)

    @Insert protected abstract suspend fun insertInvestmentGoals(rows: List<InvestmentGoalEntity>)
}
