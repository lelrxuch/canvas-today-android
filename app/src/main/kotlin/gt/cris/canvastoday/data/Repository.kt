package gt.cris.canvastoday.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import gt.cris.canvastoday.core.NoteDraft
import gt.cris.canvastoday.core.PlannerItem
import gt.cris.canvastoday.widget.TodayWidget
import gt.cris.canvastoday.work.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** Punto único de sincronización: lo usan la app, el trabajo periódico y las acciones sobre notas. */
object Repository {
    private val mutex = Mutex()

    private fun client(context: Context): CanvasClient {
        val base = Settings(context).baseUrl ?: throw CanvasException(CanvasException.Kind.NOT_CONFIGURED)
        val token = TokenStore.load(context) ?: throw CanvasException(CanvasException.Kind.NOT_CONFIGURED)
        return CanvasClient(base, token)
    }

    fun isConfigured(context: Context): Boolean =
        Settings(context).baseUrl != null && TokenStore.load(context) != null

    /** Descarga, guarda en caché, reprograma avisos y actualiza el widget. Si falla, conserva la lista anterior. */
    suspend fun sync(context: Context): Snapshot = mutex.withLock {
        val snapshot = withContext(Dispatchers.IO) {
            val settings = Settings(context)
            val previous = Cache.load(context)
            val next = try {
                val items = client(context).planner(Instant.now(), settings.overdueDays, settings.daysAhead)
                Snapshot(items = items, fetchedAt = Instant.now(), error = null)
            } catch (e: CanvasException) {
                previous.copy(error = e.kind)
            }
            Cache.save(context, next)
            if (next.error == null) Reminders.schedule(context, next.items, settings.notifyHours)
            next
        }
        TodayWidget().updateAll(context)
        snapshot
    }

    /** Crea la nota en el planner de Canvas y recarga. Lanza [CanvasException] si Canvas la rechaza. */
    suspend fun createNote(context: Context, draft: NoteDraft): Snapshot {
        withContext(Dispatchers.IO) { client(context).createNote(draft, ZoneId.systemDefault()) }
        return sync(context)
    }

    suspend fun deleteNote(context: Context, item: PlannerItem): Snapshot {
        val id = item.plannableId ?: throw CanvasException(CanvasException.Kind.HTTP)
        withContext(Dispatchers.IO) { client(context).deleteNote(id) }
        return sync(context)
    }

    suspend fun signOut(context: Context) {
        withContext(Dispatchers.IO) {
            TokenStore.clear(context)
            Cache.clear(context)
            Reminders.cancelAll(context)
        }
        TodayWidget().updateAll(context)
    }
}
