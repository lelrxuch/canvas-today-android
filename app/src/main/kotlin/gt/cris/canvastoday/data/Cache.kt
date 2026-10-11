package gt.cris.canvastoday.data

import android.content.Context
import gt.cris.canvastoday.core.PlannerItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

/** Última lista descargada: la app y el widget la muestran al instante, antes de que termine la red. */
data class Snapshot(
    val items: List<PlannerItem> = emptyList(),
    val fetchedAt: Instant? = null,
    val error: CanvasException.Kind? = null,
)

object Cache {
    private const val FILE = "planner-cache.json"

    fun load(context: Context): Snapshot = try {
        val o = JSONObject(File(context.filesDir, FILE).readText())
        val arr = o.optJSONArray("items") ?: JSONArray()
        Snapshot(
            items = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(PlannerItem::fromJson) },
            fetchedAt = o.optString("fetchedAt").takeIf { it.isNotEmpty() }?.let { Instant.parse(it) },
            error = o.optString("error").takeIf { it.isNotEmpty() }?.let { CanvasException.Kind.valueOf(it) },
        )
    } catch (_: Exception) {
        Snapshot()
    }

    fun save(context: Context, snapshot: Snapshot) {
        val o = JSONObject()
            .put("items", JSONArray(snapshot.items.map { it.toJson() }))
            .put("fetchedAt", snapshot.fetchedAt?.toString() ?: "")
            .put("error", snapshot.error?.name ?: "")
        // Escribir y renombrar: el widget nunca lee un archivo a medias.
        val tmp = File(context.filesDir, "$FILE.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(File(context.filesDir, FILE))
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
    }
}
