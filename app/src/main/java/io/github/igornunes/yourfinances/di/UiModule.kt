package io.github.igornunes.yourfinances.di

import android.os.SystemClock
import io.github.igornunes.yourfinances.ui.card.CardDetailViewModel
import io.github.igornunes.yourfinances.ui.card.CardFormViewModel
import io.github.igornunes.yourfinances.ui.card.CardListViewModel
import io.github.igornunes.yourfinances.ui.category.CategoryFormViewModel
import io.github.igornunes.yourfinances.ui.category.CategoryListViewModel
import io.github.igornunes.yourfinances.ui.entry.EntryFormViewModel
import io.github.igornunes.yourfinances.ui.goal.InvestmentGoalFormViewModel
import io.github.igornunes.yourfinances.ui.home.HomeViewModel
import io.github.igornunes.yourfinances.ui.installment.InstallmentFormViewModel
import io.github.igornunes.yourfinances.ui.installment.InstallmentListViewModel
import io.github.igornunes.yourfinances.ui.lock.AppLock
import io.github.igornunes.yourfinances.ui.lock.DeviceLock
import io.github.igornunes.yourfinances.ui.lock.KeyguardDeviceLock
import io.github.igornunes.yourfinances.ui.month.MonthViewModel
import io.github.igornunes.yourfinances.ui.recurrence.RecurrenceFormViewModel
import io.github.igornunes.yourfinances.ui.recovery.RecoveryViewModel
import io.github.igornunes.yourfinances.ui.recurrence.RecurrenceListViewModel
import io.github.igornunes.yourfinances.ui.report.ReportsViewModel
import io.github.igornunes.yourfinances.ui.savings.PocketFormViewModel
import io.github.igornunes.yourfinances.ui.subscription.SubscriptionsViewModel
import io.github.igornunes.yourfinances.ui.savings.SavingsViewModel
import io.github.igornunes.yourfinances.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { AppLock(SystemClock::elapsedRealtime) }
    single<DeviceLock> { KeyguardDeviceLock(androidContext()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { RecoveryViewModel(get(), get(), get()) }
    viewModel { SavingsViewModel(get(), get()) }
    viewModel { ReportsViewModel(get(), get(), get()) }
    viewModel { SubscriptionsViewModel(get(), get(), get()) }
    viewModel { CategoryListViewModel(get()) }
    viewModel { params -> CategoryFormViewModel(params.getOrNull(), get()) }
    viewModel { params -> PocketFormViewModel(params.getOrNull(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> InvestmentGoalFormViewModel(params.get(), get()) }
    viewModel { params -> MonthViewModel(params.get(), get(), get()) }
    viewModel { params -> EntryFormViewModel(params.getOrNull(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { RecurrenceListViewModel(get()) }
    viewModel { params -> RecurrenceFormViewModel(params.getOrNull(), get(), get(), get(), get(), get()) }
    viewModel { InstallmentListViewModel(get(), get()) }
    viewModel { InstallmentFormViewModel(get(), get(), get(), get()) }
    viewModel { CardListViewModel(get(), get()) }
    viewModel { params -> CardDetailViewModel(params.get(), get(), get()) }
    viewModel { params -> CardFormViewModel(params.getOrNull(), get(), get()) }
}
