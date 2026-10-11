package gt.cris.canvastoday.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ValidationTest {
    @Test
    fun nextLinkReadsRelNext() {
        val header = "<https://x/api?page=2>; rel=\"next\", <https://x/api?page=1>; rel=\"first\""
        assertEquals("https://x/api?page=2", nextLink(header))
        assertNull(nextLink("<https://x/api?page=1>; rel=\"first\""))
        assertNull(nextLink(null))
    }

    @Test
    fun normalizesCanvasAddress() {
        assertEquals("https://umg.instructure.com", normalizeBaseUrl(" umg.instructure.com "))
        assertEquals("https://umg.instructure.com", normalizeBaseUrl("https://UMG.instructure.com/courses/1"))
        assertEquals("https://umg.instructure.com", normalizeBaseUrl("http://umg.instructure.com"))
        assertNull(normalizeBaseUrl("umg"))
        assertNull(normalizeBaseUrl("evil.com@umg.instructure.com"))
        assertNull(normalizeBaseUrl(""))
    }

    @Test
    fun checksTokenFormat() {
        assertTrue(isTokenFormat("1234~AbCdEf123"))
        assertFalse(isTokenFormat("AbCdEf"))
        assertFalse(isTokenFormat("1234~abc def"))
    }

    @Test
    fun courseColorMatchesDesktopHash() {
        // Mismo cálculo que courseColor() en el renderer de escritorio.
        var h = 0L
        for (ch in "Bases de Datos I") h = (h * 31 + ch.code) and 0xFFFFFFFFL
        assertEquals((h % 10).toInt(), courseColorIndex("Bases de Datos I", 10))
    }

    @Test
    fun noteDraftValidationAndDate() {
        val zone = ZoneId.of("America/Guatemala")
        val day = LocalDate.of(2026, 10, 12)
        val ok = NoteDraft("Estudiar", "", day, LocalTime.of(8, 30), null)
        assertNull(ok.problem())
        assertEquals(ZonedDateTime.of(day, LocalTime.of(8, 30), zone).toInstant(), ok.todoDate(zone))
        assertEquals(
            ZonedDateTime.of(day, LocalTime.of(23, 59), zone).toInstant(),
            ok.copy(time = null).todoDate(zone),
        )
        assertEquals(NoteDraft.Problem.TITLE_EMPTY, ok.copy(title = "  ").problem())
        assertEquals(NoteDraft.Problem.TITLE_LONG, ok.copy(title = "x".repeat(256)).problem())
        assertEquals(NoteDraft.Problem.DETAILS_LONG, ok.copy(details = "x".repeat(2001)).problem())
    }
}
