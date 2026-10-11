package gt.cris.canvastoday.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import gt.cris.canvastoday.core.toCanvasAppUri

/**
 * Abre enlaces de Canvas en la app Canvas Student si está instalada (ya tiene la sesión iniciada)
 * y, si no, en el navegador.
 */
object CanvasLinks {
    /** Paquete de Canvas Student; también declarado en <queries> del manifiesto. */
    const val STUDENT_PACKAGE = "com.instructure.candroid"

    fun intentFor(context: Context, url: String): Intent {
        val app = toCanvasAppUri(url)?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)).setPackage(STUDENT_PACKAGE) }
        val target = if (app != null && app.resolveActivity(context.packageManager) != null) {
            app
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        return target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
