package theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import coreapp.util.generated.resources.Inter
import coreapp.util.generated.resources.Res
import coreapp.util.generated.resources.google_sans
import coreapp.util.generated.resources.roboto
import coreapp.util.generated.resources.ubuntu_sans
import org.jetbrains.compose.resources.Font
import org.koin.compose.koinInject

@Composable
fun currentFontFamily(): FontFamily {
    val themeProvider: ThemeProvider = koinInject()
    val appFont by themeProvider.font.collectAsState()
    val fontRes = when (appFont) {
        AppFont.GoogleSans -> Res.font.google_sans
        AppFont.UbuntuSans -> Res.font.ubuntu_sans
        AppFont.Roboto -> Res.font.roboto
        AppFont.System -> Res.font.Inter
    }
    val font = Font(fontRes)
    return remember(font) { FontFamily(font) }
}

@Composable
fun displayFontFamily(): FontFamily = currentFontFamily()

// Default Material 3 typography values
val baseline = Typography()

@Composable
fun AppTypography(): Typography {
    val fam = currentFontFamily()
    return remember(fam) {
        baseline.copy(
            displayLarge = baseline.displayLarge.copy(fontFamily = fam),
            displayMedium = baseline.displayMedium.copy(fontFamily = fam),
            displaySmall = baseline.displaySmall.copy(fontFamily = fam),
            headlineLarge = baseline.headlineLarge.copy(fontFamily = fam),
            headlineMedium = baseline.headlineMedium.copy(fontFamily = fam, fontWeight = FontWeight.SemiBold),
            headlineSmall = baseline.headlineSmall.copy(fontFamily = fam),
            titleLarge = baseline.titleLarge.copy(fontFamily = fam),
            titleMedium = baseline.titleMedium.copy(fontFamily = fam),
            titleSmall = baseline.titleSmall.copy(fontFamily = fam),
            bodyLarge = baseline.bodyLarge.copy(fontFamily = fam),
            bodyMedium = baseline.bodyMedium.copy(fontFamily = fam),
            bodySmall = baseline.bodySmall.copy(fontFamily = fam),
            labelLarge = baseline.labelLarge.copy(fontFamily = fam),
            labelMedium = baseline.labelMedium.copy(fontFamily = fam),
            labelSmall = baseline.labelSmall.copy(fontFamily = fam),
        )
    }
}
