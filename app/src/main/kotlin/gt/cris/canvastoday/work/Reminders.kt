package gt.cris.canvastoday.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import gt.cris.canvastoday.core.PlannerItem
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Avisos programados en el propio teléfono, `notifyHours` antes de cada vencimiento.
 * No dependen de las notificaciones de Canvas. Cada aviso es un trabajo único cuyo nombre incluye
 * el id, la fecha y la anticipación: si cambia cualquiera, el aviso viejo se cancela y se crea otro.
 */
object Reminders {
    private const val PREFS = "reminders"
    private const val KEY_SCHEDULED = "scheduled"

    fun key(item: PlannerItem, hours: Int) = "remind-${item.id}-${item.due.epochSecond}-$hours"

    fun schedule(context: Context, items: List<PlannerItem>, hours: Int, now: Instant = Instant.now()) {
        val wm = WorkManager.getInstance(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val scheduled = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
        val wanted = items.filter { it.due > now }.associateBy { key(it, hours) }

        for (stale in scheduled - wanted.keys) wm.cancelUniqueWork(stale)
        for ((name, item) in wanted) {
            if (name in scheduled) continue
            val fireAt = item.due.minus(Duration.ofHours(hours.toLong()))
            val delayMs = Duration.between(now, fireAt).toMillis().coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.ID to item.id,
                        ReminderWorker.TITLE to item.title,
                        ReminderWorker.COURSE to item.course,
                        ReminderWorker.DUE to item.due.toEpochMilli(),
                        ReminderWorker.URL to item.url,
                    ),
                )
                .build()
            wm.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)
        }
        prefs.edit().putStringSet(KEY_SCHEDULED, wanted.keys).apply()
    }

    fun cancelAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wm = WorkManager.getInstance(context)
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().forEach(wm::cancelUniqueWork)
        prefs.edit().clear().apply()
    }
}
