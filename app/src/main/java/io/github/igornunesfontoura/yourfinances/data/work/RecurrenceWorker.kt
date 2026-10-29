package io.github.igornunesfontoura.yourfinances.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.igornunesfontoura.yourfinances.data.repository.RecurrenceRepository
import io.github.igornunesfontoura.yourfinances.domain.recurrence.predictionWindow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Keeps the predicted entries of every recurrence filled up to the end of the prediction window. */
class RecurrenceWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val recurrences: RecurrenceRepository by inject()
    private val clock: Clock by inject()

    override suspend fun doWork(): Result {
        recurrences.predictAll(predictionWindow(LocalDate.now(clock)))
        return Result.success()
    }

    companion object {
        private const val DAILY = "recurrence-prediction-daily"
        private const val ON_START = "recurrence-prediction-on-start"

        /**
         * Runs once now (the month may have turned while the app was closed) and then daily.
         * KEEP: when WorkManager itself starts the process for the daily run, no extra run is queued.
         */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                ON_START,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<RecurrenceWorker>().build(),
            )
            workManager.enqueueUniquePeriodicWork(
                DAILY,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RecurrenceWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
