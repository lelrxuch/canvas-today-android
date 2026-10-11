package gt.cris.canvastoday.core

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Reglas del planner: qué se muestra, cómo se agrupa y cómo se describe la fecha. Igual que la app de escritorio. */
object Planner {
    const val TYPE_NOTE = "planner_note"

    private val SUPPORTED = setOf(
        "assignment",
        "sub_assignment",
        "quiz",
        "discussion_topic",
        "wiki_page",
        TYPE_NOTE,
        "assessment_request",
    )

    /** Por qué un elemento del planner no se muestra, o null si se muestra. */
    fun exclusionReason(raw: JSONObject): String? {
        if (raw.str("plannable_type") !in SUPPORTED) return "unsupported_type"
        if (dueOf(raw) == null) return "no_date"
        // En notas y páginas, "submissions" es false en lugar de un objeto.
        val s = raw.optJSONObject("submissions")
        if (s != null) {
            if (s.optBoolean("submitted")) return "submitted"
            if (s.optBoolean("graded")) return "graded"
            if (s.optBoolean("excused")) return "excused"
        }
        if (raw.optJSONObject("planner_override")?.optBoolean("marked_complete") == true) return "marked_complete"
        return null
    }

    private fun dueOf(raw: JSONObject): Instant? {
        val p = raw.optJSONObject("plannable") ?: JSONObject()
        val s = p.str("due_at") ?: p.str("todo_date") ?: raw.str("plannable_date") ?: return null
        return parseInstant(s)
    }

    fun parse(raw: JSONObject, baseUrl: String): PlannerItem? {
        if (exclusionReason(raw) != null) return null
        val type = raw.str("plannable_type") ?: return null
        val p = raw.optJSONObject("plannable") ?: JSONObject()
        val due = dueOf(raw) ?: return null
        val plannableId = raw.long("plannable_id")
        val isNote = type == TYPE_NOTE
        val url = raw.str("html_url")?.let { href ->
            try {
                URL(URL(baseUrl), href).toString()
            } catch (_: Exception) {
                null
            }
        }
        return PlannerItem(
            id = "$type-${plannableId ?: raw.str("plannable_id")}",
            type = type,
            plannableId = plannableId,
            courseId = raw.long("course_id") ?: if (isNote) p.long("course_id") else null,
            title = p.str("title") ?: p.str("name"),
            course = raw.str("context_name"),
            due = due,
            url = url,
            points = p.double("points_possible"),
            details = if (isNote) p.str("details") ?: "" else null,
        )
    }

    fun parseAll(items: JSONArray, baseUrl: String): List<PlannerItem> =
        (0 until items.length())
            .mapNotNull { items.optJSONObject(it)?.let { raw -> parse(raw, baseUrl) } }
            .sortedBy { it.due }

    enum class Group { OVERDUE, TODAY, TOMORROW, WEEK, LATER }

    fun groupOf(due: Instant, now: ZonedDateTime): Group {
        if (due < now.toInstant()) return Group.OVERDUE
        return when (dayDiff(due, now)) {
            0L -> Group.TODAY
            1L -> Group.TOMORROW
            in 2L..6L -> Group.WEEK
            else -> Group.LATER
        }
    }

    /** Grupos en orden, sin los vacíos. */
    fun grouped(items: List<PlannerItem>, now: ZonedDateTime): List<Pair<Group, List<PlannerItem>>> {
        val byGroup = items.sortedBy { it.due }.groupBy { groupOf(it.due, now) }
        return Group.entries.mapNotNull { g -> byGroup[g]?.let { g to it } }
    }

    private fun dayDiff(due: Instant, now: ZonedDateTime): Long =
        ChronoUnit.DAYS.between(now.toLocalDate(), due.atZone(now.zone).toLocalDate())

    /** Texto relativo de vencimiento, como "En 20 min" o "Hace 2 d"; la vista lo traduce. */
    sealed interface DueLabel {
        data class OverdueToday(val time: LocalTime) : DueLabel
        data object Yesterday : DueLabel
        data class DaysAgo(val days: Long) : DueLabel
        data class InMinutes(val minutes: Long) : DueLabel
        data class InHours(val hours: Long) : DueLabel
        data class At(val time: LocalTime) : DueLabel
        data class WeekdayAt(val day: DayOfWeek, val time: LocalTime) : DueLabel
        data class On(val date: LocalDate) : DueLabel

        val isLate: Boolean get() = this is OverdueToday || this is Yesterday || this is DaysAgo
        val isSoon: Boolean get() = this is InMinutes || this is InHours
    }

    fun dueLabel(due: Instant, now: ZonedDateTime): DueLabel {
        val left = Duration.between(now.toInstant(), due)
        val local = due.atZone(now.zone)
        val days = dayDiff(due, now)
        if (left.isNegative) {
            return when (-days) {
                0L -> DueLabel.OverdueToday(local.toLocalTime())
                1L -> DueLabel.Yesterday
                else -> DueLabel.DaysAgo(-days)
            }
        }
        // Duration.getSeconds (Java 8): toSeconds() no existe en todas las versiones de Android.
        if (left < Duration.ofHours(1)) return DueLabel.InMinutes(maxOf(1L, Math.round(left.seconds / 60.0)))
        if (left < Duration.ofHours(6)) return DueLabel.InHours(Math.round(left.seconds / 3600.0))
        if (days <= 1) return DueLabel.At(local.toLocalTime())
        if (days < 7) return DueLabel.WeekdayAt(local.dayOfWeek, local.toLocalTime())
        return DueLabel.On(local.toLocalDate())
    }
}
