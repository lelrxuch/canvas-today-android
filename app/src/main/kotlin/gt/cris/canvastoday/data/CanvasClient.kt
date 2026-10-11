package gt.cris.canvastoday.data

import gt.cris.canvastoday.core.NoteDraft
import gt.cris.canvastoday.core.Planner
import gt.cris.canvastoday.core.PlannerItem
import gt.cris.canvastoday.core.nextLink
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class CanvasException(val kind: Kind, val status: Int? = null) : Exception(kind.name) {
    enum class Kind { AUTH, FORBIDDEN, NETWORK, TIMEOUT, HTTP, NOT_CONFIGURED }
}

/**
 * Cliente mínimo de la API de Canvas. Bloqueante: llamar desde Dispatchers.IO.
 * El token solo se envía a `baseUrl` por https; los enlaces de paginación a otro dominio se ignoran.
 */
class CanvasClient(baseUrl: String, private val token: String) {
    private val base = URL(baseUrl)

    private class Response(val body: String, val link: String?)

    private fun request(url: URL, method: String = "GET", body: JSONObject? = null): Response {
        if (url.protocol != "https" || url.host != base.host) throw CanvasException(CanvasException.Kind.HTTP)
        val c = try {
            url.openConnection() as HttpURLConnection
        } catch (_: IOException) {
            throw CanvasException(CanvasException.Kind.NETWORK)
        }
        try {
            c.requestMethod = method
            // Sin redirecciones automáticas: una respuesta 3xx podría llevar el token a otro dominio.
            // Canvas no redirige la API; si pasa, se trata como error.
            c.instanceFollowRedirects = false
            c.connectTimeout = TIMEOUT_MS
            c.readTimeout = TIMEOUT_MS
            c.setRequestProperty("Authorization", "Bearer $token")
            c.setRequestProperty("Accept", "application/json")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            when {
                code == 401 -> throw CanvasException(CanvasException.Kind.AUTH, code)
                code == 403 -> throw CanvasException(CanvasException.Kind.FORBIDDEN, code)
                code !in 200..299 -> throw CanvasException(CanvasException.Kind.HTTP, code)
            }
            val text = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return Response(text, c.getHeaderField("Link"))
        } catch (_: SocketTimeoutException) {
            throw CanvasException(CanvasException.Kind.TIMEOUT)
        } catch (_: IOException) {
            throw CanvasException(CanvasException.Kind.NETWORK)
        } finally {
            c.disconnect()
        }
    }

    /** GET de una lista siguiendo la paginación, con un tope de páginas por si Canvas repite "next". */
    private fun getArray(path: String): JSONArray {
        val out = JSONArray()
        var url: URL? = URL(base, path)
        var page = 0
        while (url != null && page < MAX_PAGES) {
            if (url.host != base.host) break
            val res = request(url)
            val arr = JSONArray(res.body)
            for (i in 0 until arr.length()) out.put(arr.get(i))
            url = nextLink(res.link)?.let { URL(it) }
            page += 1
        }
        return out
    }

    fun planner(now: Instant, overdueDays: Int, daysAhead: Int): List<PlannerItem> {
        val start = now.minus(Duration.ofDays(overdueDays.toLong()))
        val end = now.plus(Duration.ofDays(daysAhead.toLong()))
        val qs = "start_date=${enc(start.toString())}&end_date=${enc(end.toString())}&per_page=$PER_PAGE"
        return Planner.parseAll(getArray("/api/v1/planner/items?$qs"), base.toString())
    }

    fun createNote(draft: NoteDraft, zone: ZoneId) {
        val body = JSONObject()
            .put("title", draft.title.trim())
            .put("details", draft.details.trim())
            .put("todo_date", draft.todoDate(zone).toString())
        draft.courseId?.let { body.put("course_id", it) }
        request(URL(base, "/api/v1/planner_notes"), "POST", body)
    }

    fun deleteNote(noteId: Long) {
        request(URL(base, "/api/v1/planner_notes/$noteId"), "DELETE")
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val TIMEOUT_MS = 20_000
        const val MAX_PAGES = 10
        const val PER_PAGE = 100
    }
}
