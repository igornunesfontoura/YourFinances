package io.github.igornunes.yourfinances

import android.app.Application
import androidx.work.Configuration
import io.github.igornunes.yourfinances.data.work.RecurrenceWorker
import io.github.igornunes.yourfinances.di.dataModule
import io.github.igornunes.yourfinances.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * WorkManager starts on demand (its startup initializer is removed in the manifest), so no worker
 * can run before Koin is ready.
 */
class YourFinancesApp : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@YourFinancesApp)
            modules(dataModule, uiModule)
        }
        RecurrenceWorker.schedule(this)
    }
}
