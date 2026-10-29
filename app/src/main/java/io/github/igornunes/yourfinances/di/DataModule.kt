package io.github.igornunes.yourfinances.di

import android.content.Context
import io.github.igornunes.yourfinances.data.backup.BackupService
import io.github.igornunes.yourfinances.data.backup.RoomBackupService
import io.github.igornunes.yourfinances.data.db.AppDatabase
import io.github.igornunes.yourfinances.data.files.ContentResolverDocuments
import io.github.igornunes.yourfinances.data.files.Documents
import io.github.igornunes.yourfinances.data.repository.CategoryRepository
import io.github.igornunes.yourfinances.data.repository.CreditCardRepository
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.data.repository.InstallmentRepository
import io.github.igornunes.yourfinances.data.repository.InvestmentGoalRepository
import io.github.igornunes.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunes.yourfinances.data.repository.RoomCreditCardRepository
import io.github.igornunes.yourfinances.data.repository.RoomEntryRepository
import io.github.igornunes.yourfinances.data.repository.RoomInstallmentRepository
import io.github.igornunes.yourfinances.data.repository.RoomInvestmentGoalRepository
import io.github.igornunes.yourfinances.data.repository.RoomRecurrenceRepository
import io.github.igornunes.yourfinances.data.repository.RoomSavingsRepository
import io.github.igornunes.yourfinances.data.repository.SavingsRepository
import io.github.igornunes.yourfinances.data.security.DatabaseKeyProvider
import io.github.igornunes.yourfinances.data.security.DatabaseRecovery
import io.github.igornunes.yourfinances.data.security.KeystoreDatabaseRecovery
import io.github.igornunes.yourfinances.data.settings.Settings
import io.github.igornunes.yourfinances.data.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File
import java.time.Clock

val dataModule = module {
    // noBackupFilesDir: the wrapped key must never be copied off the device.
    single {
        DatabaseKeyProvider(
            keyFile = File(androidContext().noBackupFilesDir, "database.key"),
            databaseFile = androidContext().getDatabasePath(AppDatabase.NAME),
        )
    }
    // A copy: the passphrase stays cached in the provider whatever SQLCipher does with its array.
    single { AppDatabase.buildEncrypted(androidContext(), get<DatabaseKeyProvider>().getOrCreatePassphrase().copyOf()) }
    single<DatabaseRecovery> { KeystoreDatabaseRecovery(androidContext(), get()) }
    single<BackupService> { RoomBackupService({ get<AppDatabase>().backupDao() }, get()) }
    single<Documents> { ContentResolverDocuments(androidContext().contentResolver) }
    single<Settings> { SharedPreferencesSettings(androidContext().getSharedPreferences("settings", Context.MODE_PRIVATE)) }
    single { get<AppDatabase>().categoryDao() }
    single<Clock> { Clock.systemDefaultZone() }
    single<EntryRepository> { RoomEntryRepository(get()) }
    single<RecurrenceRepository> { RoomRecurrenceRepository(get()) }
    single<InstallmentRepository> { RoomInstallmentRepository(get()) }
    single { CategoryRepository(get()) }
    single<CreditCardRepository> { RoomCreditCardRepository(get()) }
    single<SavingsRepository> { RoomSavingsRepository(get()) }
    single<InvestmentGoalRepository> { RoomInvestmentGoalRepository(get<AppDatabase>().investmentGoalDao()) }
}
