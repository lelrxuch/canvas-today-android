package gt.cris.canvastoday.core

import org.json.JSONObject
import java.time.Instant

/** Un pendiente de la lista, ya filtrado y normalizado desde /api/v1/planner/items. */
data class PlannerItem(
    val id: String,
    val type: String,
    val plannableId: Long?,
    val courseId: Long?,
    val title: String?,
    /** Nombre del curso; null en notas personales sin curso. */
    val course: String?,
    val due: Instant,
    val url: String?,
    val points: Double?,
    /** Solo en notas personales. */
    val details: String?,
) {
    val isNote: Boolean get() = type == Planner.TYPE_NOTE

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("type", type)
        .put("plannableId", plannableId ?: JSONObject.NULL)
        .put("courseId", courseId ?: JSONObject.NULL)
        .put("title", title ?: JSONObject.NULL)
        .put("course", course ?: JSONObject.NULL)
        .put("due", due.toString())
        .put("url", url ?: JSONObject.NULL)
        .put("points", points ?: JSONObject.NULL)
        .put("details", details ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject): PlannerItem? {
            val due = o.str("due")?.let(::parseInstant) ?: return null
            return PlannerItem(
                id = o.str("id") ?: return null,
                type = o.str("type") ?: return null,
                plannableId = o.long("plannableId"),
                courseId = o.long("courseId"),
                title = o.str("title"),
                course = o.str("course"),
                due = due,
                url = o.str("url"),
                points = o.double("points"),
                details = o.str("details"),
            )
        }
    }
}

/** `optString` devuelve "null" para JSONObject.NULL; esto devuelve null y descarta cadenas vacías. */
internal fun JSONObject.str(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }

internal fun JSONObject.long(name: String): Long? =
    if (isNull(name)) null else optLong(name, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }

internal fun JSONObject.double(name: String): Double? =
    if (isNull(name)) null else optDouble(name).takeIf { !it.isNaN() }

internal fun parseInstant(s: String): Instant? = try {
    Instant.parse(s)
} catch (_: Exception) {
    null
}
