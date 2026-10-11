package gt.cris.canvastoday.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Siguiente página según el header Link de Canvas, o null si no hay. */
fun nextLink(header: String?): String? {
    if (header.isNullOrBlank()) return null
    val re = Regex("""<([^>]+)>\s*;\s*rel="next"""")
    return header.split(',').firstNotNullOfOrNull { re.find(it)?.groupValues?.get(1) }
}

/**
 * "umg.instructure.com", "https://umg.instructure.com/courses" o similares → "https://umg.instructure.com".
 * Siempre https; null si no parece un dominio.
 */
fun normalizeBaseUrl(input: String): String? {
    val host = input.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
        .substringBefore('?')
        .lowercase()
    val valid = Regex("""^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$""")
    return if (valid.matches(host)) "https://$host" else null
}

/** Los tokens de Canvas tienen la forma "número~letras". */
fun isTokenFormat(token: String): Boolean = Regex("""^\d+~[A-Za-z0-9]+$""").matches(token.trim())

/** Color estable por curso, con el mismo hash que la app de escritorio: índice en la paleta. */
fun courseColorIndex(name: String, paletteSize: Int): Int {
    var h = 0L
    name.codePoints().forEach { cp -> h = (h * 31 + cp) and 0xFFFFFFFFL }
    return (h % paletteSize).toInt()
}

/** Nota personal tal como la arma el formulario. */
data class NoteDraft(
    val title: String,
    val details: String,
    val date: LocalDate,
    /** Sin hora, vence al final del día, como las tareas de Canvas. */
    val time: LocalTime?,
    val courseId: Long?,
) {
    enum class Problem { TITLE_EMPTY, TITLE_LONG, DETAILS_LONG }

    fun problem(): Problem? = when {
        title.isBlank() -> Problem.TITLE_EMPTY
        title.trim().length > TITLE_MAX -> Problem.TITLE_LONG
        details.trim().length > DETAILS_MAX -> Problem.DETAILS_LONG
        else -> null
    }

    fun todoDate(zone: ZoneId): Instant = date.atTime(time ?: END_OF_DAY).atZone(zone).toInstant()

    companion object {
        const val TITLE_MAX = 255
        const val DETAILS_MAX = 2000
        val END_OF_DAY: LocalTime = LocalTime.of(23, 59)
    }
}
