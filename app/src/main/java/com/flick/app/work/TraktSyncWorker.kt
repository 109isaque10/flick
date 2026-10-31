package com.flick.app.work

import android.app.ActivityManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flick.app.data.local.SettingsDataStore
import com.flick.app.data.local.FlickDatabase
import com.flick.app.data.repository.TraktRepository
import java.util.concurrent.TimeUnit

/**
 * Reconciles the Trakt watchlist and playback history, which is what refreshes
 * the "Continue Watching" rail. It used to run as a network call launched while
 * the first frame was composing; as a worker it runs off the launch path under
 * explicit constraints.
 */
class TraktSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = FlickDatabase.getDatabase(applicationContext)
        val trakt = TraktRepository(
            settingsDataStore = SettingsDataStore(applicationContext),
            watchlistDao = db.watchlistDao(),
            watchProgressDao = db.watchProgressDao()
        )
        return try {
            trakt.syncWatchlist()
            trakt.syncPlayback()
            Result.success()
        } catch (t: Throwable) {
            // Offline is expected on a TV; back off and let the constraints retry.
            Result.retry()
        }
    }
}

object TraktSyncScheduler {

    private const val INITIAL_WORK = "trakt-sync-initial"
    private const val PERIODIC_WORK = "trakt-sync-periodic"

    fun schedule(context: Context) {
        val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice

        // Launch reconciliation keeps only the constraints it genuinely needs, so
        // the Continue Watching rail is fresh shortly after start.
        WorkManager.getInstance(context).enqueueUniqueWork(
            INITIAL_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<TraktSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .build()
        )

        // Background reconciliation. The TV memory guide asks low-RAM devices to
        // require device-idle so this never competes with playback; unconstrained
        // devices can reconcile on a plain network schedule.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<TraktSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .setRequiresDeviceIdle(lowRam)
                        .build()
                )
                .build()
        )
    }
}
