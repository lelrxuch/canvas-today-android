package gt.cris.canvastoday.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Colores del sistema (Material You / One UI) cuando existen; si no, un azul neutro. */
@Composable
fun CanvasTodayTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF8AB4FF))
        else -> lightColorScheme(primary = Color(0xFF1F6FEB))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Misma paleta y mismo hash que la app de escritorio: cada curso conserva su color. */
val COURSE_PALETTE = listOf(
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF9500),
    Color(0xFFAF52DE),
    Color(0xFFFF2D55),
    Color(0xFF5AC8FA),
    Color(0xFFFFCC00),
    Color(0xFF5856D6),
    Color(0xFF00C7BE),
    Color(0xFFA2845E),
)
