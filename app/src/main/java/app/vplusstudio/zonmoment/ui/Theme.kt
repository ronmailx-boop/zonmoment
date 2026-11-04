package app.vplusstudio.zonmoment.ui

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

/** Sun yellow for solar output, used in the chart in both themes. */
val SunYellow = Color(0xFFF5B400)
val PriceBlue = Color(0xFF2E6FD8)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A00),
    secondary = Color(0xFF2E6FD8),
    tertiary = Color(0xFF2E7D32),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC94D),
    secondary = Color(0xFF9BBCFF),
    tertiary = Color(0xFF8BD18E),
)

@Composable
fun ZonmomentTheme(
    dark: Boolean = isSystemInDarkTheme(),
    // Off for the store screenshots, which must show the brand colours rather than a wallpaper palette.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
