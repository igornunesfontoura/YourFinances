package io.github.igornunes.yourfinances.di

import io.github.igornunes.yourfinances.data.db.AppDatabase
import io.github.igornunes.yourfinances.data.repository.CategoryRepository
import io.github.igornunes.yourfinances.data.repository.CreditCardRepository
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.data.repository.InstallmentRepository
import io.github.igornunes.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunes.yourfinances.data.repository.RoomCreditCardRepository
import io.github.igornunes.yourfinances.data.repository.RoomEntryRepository
import io.github.igornunes.yourfinances.data.repository.RoomInstallmentRepository
import io.github.igornunes.yourfinances.data.repository.RoomRecurrenceRepository
import io.github.igornunes.yourfinances.data.security.DatabaseKeyProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File
import java.time.Clock

val dataModule = module {
    // noBackupFilesDir: the wrapped key must never be copied off the device.
    single { DatabaseKeyProvider(File(androidContext().noBackupFilesDir, "database.key")) }
    single { AppDatabase.buildEncrypted(androidContext(), get<DatabaseKeyProvider>().getOrCreatePassphrase()) }
    single { get<AppDatabase>().categoryDao() }
    single<Clock> { Clock.systemDefaultZone() }
    single<EntryRepository> { RoomEntryRepository(get()) }
    single<RecurrenceRepository> { RoomRecurrenceRepository(get()) }
    single<InstallmentRepository> { RoomInstallmentRepository(get()) }
    single { CategoryRepository(get()) }
    single<CreditCardRepository> { RoomCreditCardRepository(get()) }
}
