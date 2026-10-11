package gt.cris.canvastoday.ui

import android.content.Context
import gt.cris.canvastoday.R
import gt.cris.canvastoday.core.Planner
import gt.cris.canvastoday.core.PlannerItem
import gt.cris.canvastoday.data.CanvasException
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Textos derivados de los datos; los comparten la app, el widget y los avisos. */
object Labels {
    private val time: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val shortDate: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val fullDate: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)

    private fun cap(s: String) = s.replaceFirstChar { it.titlecase(Locale.getDefault()) }

    fun due(context: Context, label: Planner.DueLabel): String = when (label) {
        is Planner.DueLabel.OverdueToday -> context.getString(R.string.due_overdue_today, time.format(label.time))
        Planner.DueLabel.Yesterday -> context.getString(R.string.due_yesterday)
        is Planner.DueLabel.DaysAgo -> context.getString(R.string.due_days_ago, label.days.toInt())
        is Planner.DueLabel.InMinutes -> context.getString(R.string.due_in_minutes, label.minutes.toInt())
        is Planner.DueLabel.InHours -> context.getString(R.string.due_in_hours, label.hours.toInt())
        is Planner.DueLabel.At -> time.format(label.time)
        is Planner.DueLabel.WeekdayAt ->
            "${cap(label.day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))} ${time.format(label.time)}"
        is Planner.DueLabel.On -> cap(shortDate.format(label.date))
    }

    fun fullDue(item: PlannerItem): String =
        cap(fullDate.format(item.due.atZone(java.time.ZoneId.systemDefault())))

    fun course(context: Context, item: PlannerItem): String =
        item.course ?: context.getString(if (item.isNote) R.string.personal_note else R.string.default_course)

    fun type(context: Context, type: String): String = context.getString(
        when (type) {
            "quiz" -> R.string.type_quiz
            "discussion_topic" -> R.string.type_discussion_topic
            "wiki_page" -> R.string.type_wiki_page
            "planner_note" -> R.string.type_planner_note
            "assessment_request" -> R.string.type_assessment_request
            else -> R.string.type_assignment
        },
    )

    fun meta(context: Context, item: PlannerItem): String {
        val parts = mutableListOf(course(context, item), type(context, item.type))
        item.points?.let { parts += context.getString(R.string.points, DecimalFormat("#.##").format(it)) }
        return parts.joinToString(" · ")
    }

    fun group(group: Planner.Group): Int = when (group) {
        Planner.Group.OVERDUE -> R.string.group_overdue
        Planner.Group.TODAY -> R.string.group_today
        Planner.Group.TOMORROW -> R.string.group_tomorrow
        Planner.Group.WEEK -> R.string.group_week
        Planner.Group.LATER -> R.string.group_later
    }

    fun error(kind: CanvasException.Kind): Int = when (kind) {
        CanvasException.Kind.AUTH -> R.string.error_auth
        CanvasException.Kind.FORBIDDEN -> R.string.error_forbidden
        CanvasException.Kind.NETWORK -> R.string.error_network
        CanvasException.Kind.TIMEOUT -> R.string.error_timeout
        CanvasException.Kind.HTTP -> R.string.error_http
        CanvasException.Kind.NOT_CONFIGURED -> R.string.error_not_configured
    }

    fun timeOf(instant: java.time.Instant): String = time.format(instant.atZone(java.time.ZoneId.systemDefault()))
}
