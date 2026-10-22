package theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun setStatusBarTheme(colorScheme: CoreAppColorScheme) {
}

actual fun isDynamicColorSupported(): Boolean = false

@Composable
actual fun getPlatformColorScheme(colorScheme: CoreAppColorScheme, dynamicColor: Boolean): ColorScheme? = null
