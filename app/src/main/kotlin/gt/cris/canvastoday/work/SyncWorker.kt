package gt.cris.canvastoday.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import gt.cris.canvastoday.data.CanvasException
import gt.cris.canvastoday.data.Repository
import java.util.concurrent.TimeUnit

/** Sincroniza cada 15 minutos (el mínimo de Android para trabajo periódico) cuando hay red. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!Repository.isConfigured(applicationContext)) return Result.success()
        val snapshot = Repository.sync(applicationContext)
        return when (snapshot.error) {
            CanvasException.Kind.NETWORK, CanvasException.Kind.TIMEOUT -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val NAME = "sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
