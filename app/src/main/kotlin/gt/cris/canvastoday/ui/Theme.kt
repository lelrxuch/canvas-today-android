package gt.cris.canvastoday.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
        // En oscuro, paleta fija estilo iOS: los colores dinámicos dejan todo en grises sin contraste.
        dark -> AppleDark
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        else -> lightColorScheme(primary = Color(0xFF1F6FEB))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/**
 * Modo oscuro con los colores de sistema de iOS: fondo negro, celdas agrupadas en #1C1C1E,
 * texto secundario translúcido, azul #0A84FF como acento y rojo #FF453A.
 */
private val AppleDark = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A84FF),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF0A84FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF30D158),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0x99EBEBF5),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF3A1A18),
    onErrorContainer = Color(0xFFFF6961),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Color(0xFF007AFF),
    scrim = Color.Black,
)

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
