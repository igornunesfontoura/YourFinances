package io.github.igornunes.yourfinances.di

import io.github.igornunes.yourfinances.ui.entry.EntryFormViewModel
import io.github.igornunes.yourfinances.ui.month.MonthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock

val uiModule = module {
    single<Clock> { Clock.systemDefaultZone() }
    viewModel { MonthViewModel(get(), get()) }
    viewModel { params -> EntryFormViewModel(params.getOrNull(), get(), get()) }
}
