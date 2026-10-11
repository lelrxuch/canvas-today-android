package gt.cris.canvastoday.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PlannerTest {
    private val base = "https://canvas.example.edu"
    private val zone = ZoneId.of("America/Guatemala")
    private val now = ZonedDateTime.of(2026, 10, 14, 10, 0, 0, 0, zone)

    private fun raw(
        type: String = "assignment",
        due: String? = "2026-10-15T05:59:00Z",
        submissions: Any = JSONObject(),
        override: JSONObject? = null,
        plannable: JSONObject = JSONObject().put("title", "Diagrama ER").put("points_possible", 10),
    ): JSONObject {
        if (due != null) plannable.put("due_at", due)
        return JSONObject()
            .put("plannable_type", type)
            .put("plannable_id", 42)
            .put("course_id", 7)
            .put("context_name", "Bases de Datos I")
            .put("html_url", "/courses/7/assignments/42")
            .put("plannable", plannable)
            .put("submissions", submissions)
            .apply { if (override != null) put("planner_override", override) }
    }

    @Test
    fun parsesAnAssignment() {
        val item = Planner.parse(raw(), base)!!
        assertEquals("assignment-42", item.id)
        assertEquals(42L, item.plannableId)
        assertEquals(7L, item.courseId)
        assertEquals("Diagrama ER", item.title)
        assertEquals("Bases de Datos I", item.course)
        assertEquals(Instant.parse("2026-10-15T05:59:00Z"), item.due)
        assertEquals("$base/courses/7/assignments/42", item.url)
        assertEquals(10.0, item.points!!, 0.0)
        assertNull(item.details)
    }

    @Test
    fun explainsEachExclusion() {
        assertNull(Planner.exclusionReason(raw()))
        assertEquals("unsupported_type", Planner.exclusionReason(raw(type = "announcement")))
        assertEquals("no_date", Planner.exclusionReason(raw(due = null)))
        assertEquals("submitted", Planner.exclusionReason(raw(submissions = JSONObject().put("submitted", true))))
        assertEquals("graded", Planner.exclusionReason(raw(submissions = JSONObject().put("graded", true))))
        assertEquals("excused", Planner.exclusionReason(raw(submissions = JSONObject().put("excused", true))))
        assertEquals(
            "marked_complete",
            Planner.exclusionReason(raw(override = JSONObject().put("marked_complete", true))),
        )
    }

    @Test
    fun notesUseTodoDateAndKeepDetails() {
        val note = raw(
            type = "planner_note",
            due = null,
            submissions = false,
            plannable = JSONObject()
                .put("title", "Repasar")
                .put("details", "Cap. 3")
                .put("todo_date", "2026-10-14T20:00:00Z"),
        ).apply {
            remove("course_id")
            remove("context_name")
            put("html_url", JSONObject.NULL)
        }
        val item = Planner.parse(note, base)!!
        assertTrue(item.isNote)
        assertEquals("Cap. 3", item.details)
        assertNull(item.course)
        assertNull(item.url)
    }

    @Test
    fun parseAllSortsAndSkipsExcluded() {
        val arr = JSONArray()
            .put(raw(due = "2026-10-20T05:59:00Z"))
            .put(raw(due = "2026-10-15T05:59:00Z").put("plannable_id", 43))
            .put(raw(type = "announcement"))
        val items = Planner.parseAll(arr, base)
        assertEquals(listOf("assignment-43", "assignment-42"), items.map { it.id })
    }

    @Test
    fun groupsByLocalDay() {
        fun at(h: Int, plusDays: Long = 0) = now.plusDays(plusDays).withHour(h).toInstant()
        assertEquals(Planner.Group.OVERDUE, Planner.groupOf(at(9), now))
        assertEquals(Planner.Group.TODAY, Planner.groupOf(at(23), now))
        assertEquals(Planner.Group.TOMORROW, Planner.groupOf(at(1, 1), now))
        assertEquals(Planner.Group.WEEK, Planner.groupOf(at(12, 6), now))
        assertEquals(Planner.Group.LATER, Planner.groupOf(at(12, 7), now))
    }

    @Test
    fun dueLabels() {
        fun label(minutes: Long) = Planner.dueLabel(now.plusMinutes(minutes).toInstant(), now)
        assertEquals(Planner.DueLabel.InMinutes(20), label(20))
        assertEquals(Planner.DueLabel.InMinutes(1), label(0))
        assertEquals(Planner.DueLabel.InHours(3), label(170))
        assertEquals(Planner.DueLabel.At(LocalTime.of(23, 0)), label(13 * 60))
        assertEquals(Planner.DueLabel.OverdueToday(LocalTime.of(9, 0)), label(-60))
        assertEquals(Planner.DueLabel.Yesterday, label(-24 * 60))
        assertEquals(Planner.DueLabel.DaysAgo(3), label(-3 * 24 * 60))
        assertTrue(label(-60).isLate)
        assertTrue(label(30).isSoon)
    }

    @Test
    fun itemRoundTripsThroughCacheJson() {
        val item = Planner.parse(raw(), base)!!
        assertEquals(item, PlannerItem.fromJson(JSONObject(item.toJson().toString())))
    }
}
