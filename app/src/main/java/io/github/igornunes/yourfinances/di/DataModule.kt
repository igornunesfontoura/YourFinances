package io.github.igornunes.yourfinances.di

import io.github.igornunes.yourfinances.data.db.AppDatabase
import io.github.igornunes.yourfinances.data.repository.CategoryRepository
import io.github.igornunes.yourfinances.data.repository.CreditCardRepository
import io.github.igornunes.yourfinances.data.repository.EntryRepository
import io.github.igornunes.yourfinances.data.security.DatabaseKeyProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val dataModule = module {
    // noBackupFilesDir: the wrapped key must never be copied off the device.
    single { DatabaseKeyProvider(File(androidContext().noBackupFilesDir, "database.key")) }
    single { AppDatabase.buildEncrypted(androidContext(), get<DatabaseKeyProvider>().getOrCreatePassphrase()) }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().creditCardDao() }
    single { EntryRepository(get()) }
    single { CategoryRepository(get()) }
    single { CreditCardRepository(get()) }
}
