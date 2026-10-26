package io.github.igornunes.yourfinances.di

import io.github.igornunes.yourfinances.ui.entry.EntryFormViewModel
import io.github.igornunes.yourfinances.ui.installment.InstallmentFormViewModel
import io.github.igornunes.yourfinances.ui.installment.InstallmentListViewModel
import io.github.igornunes.yourfinances.ui.month.MonthViewModel
import io.github.igornunes.yourfinances.ui.recurrence.RecurrenceFormViewModel
import io.github.igornunes.yourfinances.ui.recurrence.RecurrenceListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { MonthViewModel(get(), get()) }
    viewModel { params -> EntryFormViewModel(params.getOrNull(), get(), get(), get(), get()) }
    viewModel { RecurrenceListViewModel(get()) }
    viewModel { params -> RecurrenceFormViewModel(params.getOrNull(), get(), get()) }
    viewModel { InstallmentListViewModel(get(), get()) }
    viewModel { InstallmentFormViewModel(get(), get()) }
}
