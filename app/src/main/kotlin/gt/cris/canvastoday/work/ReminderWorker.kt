package gt.cris.canvastoday.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Instant

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val due = Instant.ofEpochMilli(inputData.getLong(DUE, 0))
        // Si ya venció (el teléfono estuvo apagado), avisar no sirve de nada.
        if (due <= Instant.now()) return Result.success()
        Notifications.showDue(
            applicationContext,
            id = inputData.getString(ID) ?: return Result.success(),
            title = inputData.getString(TITLE),
            course = inputData.getString(COURSE),
            due = due,
            url = inputData.getString(URL),
        )
        return Result.success()
    }

    companion object {
        const val ID = "id"
        const val TITLE = "title"
        const val COURSE = "course"
        const val DUE = "due"
        const val URL = "url"
    }
}
