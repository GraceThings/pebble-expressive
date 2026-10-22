package theme

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun setStatusBarTheme(colorScheme: CoreAppColorScheme) {
    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(colorScheme) {
        if (activity != null) {
            val style = if (colorScheme.isDark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
            activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
    }
}

actual fun isDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun getPlatformColorScheme(colorScheme: CoreAppColorScheme, dynamicColor: Boolean): ColorScheme? {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val scheme = if (colorScheme.isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        return if (colorScheme == CoreAppColorScheme.Black) {
            scheme.copy(
                background = androidx.compose.ui.graphics.Color.Black,
                surface = androidx.compose.ui.graphics.Color.Black,
                scrim = androidx.compose.ui.graphics.Color.Black,
                surfaceContainerLowest = androidx.compose.ui.graphics.Color.Black,
            )
        } else {
            scheme
        }
    }
    return null
}
