package io.github.igornunesfontoura.yourfinances.di

import android.os.SystemClock
import io.github.igornunesfontoura.yourfinances.ui.card.CardDetailViewModel
import io.github.igornunesfontoura.yourfinances.ui.card.CardFormViewModel
import io.github.igornunesfontoura.yourfinances.ui.card.CardListViewModel
import io.github.igornunesfontoura.yourfinances.ui.category.CategoryFormViewModel
import io.github.igornunesfontoura.yourfinances.ui.category.CategoryListViewModel
import io.github.igornunesfontoura.yourfinances.ui.components.SelectedMonth
import io.github.igornunesfontoura.yourfinances.ui.entry.EntryFormViewModel
import io.github.igornunesfontoura.yourfinances.ui.goal.InvestmentGoalFormViewModel
import io.github.igornunesfontoura.yourfinances.ui.home.HomeViewModel
import io.github.igornunesfontoura.yourfinances.ui.importing.ImportViewModel
import io.github.igornunesfontoura.yourfinances.ui.installment.InstallmentFormViewModel
import io.github.igornunesfontoura.yourfinances.ui.installment.InstallmentListViewModel
import io.github.igornunesfontoura.yourfinances.ui.lock.AppLock
import io.github.igornunesfontoura.yourfinances.ui.lock.DeviceLock
import io.github.igornunesfontoura.yourfinances.ui.lock.KeyguardDeviceLock
import io.github.igornunesfontoura.yourfinances.ui.month.MonthViewModel
import io.github.igornunesfontoura.yourfinances.ui.recurrence.RecurrenceFormViewModel
import io.github.igornunesfontoura.yourfinances.ui.recovery.RecoveryViewModel
import io.github.igornunesfontoura.yourfinances.ui.recurrence.RecurrenceListViewModel
import io.github.igornunesfontoura.yourfinances.ui.report.ReportsViewModel
import io.github.igornunesfontoura.yourfinances.ui.savings.PocketFormViewModel
import io.github.igornunesfontoura.yourfinances.ui.subscription.SubscriptionsViewModel
import io.github.igornunesfontoura.yourfinances.ui.savings.SavingsViewModel
import io.github.igornunesfontoura.yourfinances.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock
import java.time.YearMonth

val uiModule = module {
    single { AppLock(SystemClock::elapsedRealtime) }
    single<DeviceLock> { KeyguardDeviceLock(androidContext()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { RecoveryViewModel(get(), get(), get()) }
    viewModel { SavingsViewModel(get(), get()) }
    viewModel { ReportsViewModel(get(), get(), get()) }
    viewModel { SubscriptionsViewModel(get(), get(), get()) }
    viewModel { CategoryListViewModel(get()) }
    viewModel { ImportViewModel(get(), get(), get(), get(), get()) }
    viewModel { params -> CategoryFormViewModel(params.getOrNull(), get()) }
    viewModel { params -> PocketFormViewModel(params.getOrNull(), get()) }
    single { SelectedMonth(YearMonth.now(get<Clock>())) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { params -> InvestmentGoalFormViewModel(params.get(), get()) }
    viewModel { MonthViewModel(get(), get(), get()) }
    viewModel { params -> EntryFormViewModel(params.getOrNull(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { RecurrenceListViewModel(get()) }
    viewModel { params -> RecurrenceFormViewModel(params.getOrNull(), get(), get(), get(), get(), get()) }
    viewModel { InstallmentListViewModel(get(), get()) }
    viewModel { InstallmentFormViewModel(get(), get(), get(), get()) }
    viewModel { CardListViewModel(get(), get()) }
    viewModel { params -> CardDetailViewModel(params.get(), get(), get()) }
    viewModel { params -> CardFormViewModel(params.getOrNull(), get(), get()) }
}
