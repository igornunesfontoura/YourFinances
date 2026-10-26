package io.github.igornunes.yourfinances

import android.app.Application
import io.github.igornunes.yourfinances.di.dataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class YourFinancesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@YourFinancesApp)
            modules(dataModule)
        }
    }
}
