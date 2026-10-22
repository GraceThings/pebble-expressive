package coredevices.pebble.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coreapp.util.generated.resources.Inter
import coreapp.util.generated.resources.Res
import coreapp.util.generated.resources.google_sans
import coreapp.util.generated.resources.roboto
import coreapp.util.generated.resources.ubuntu_sans
import coredevices.pebble.ui.NavBarNav
import coredevices.pebble.ui.TopBarParams
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import theme.AppFont
import theme.CoreAppTheme
import theme.CustomThemeColor
import theme.ThemeProvider
import theme.isDynamicColorSupported

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    navBarNav: NavBarNav,
    topBarParams: TopBarParams,
) {
    val themeProvider: ThemeProvider = koinInject()
    val currentTheme by themeProvider.theme.collectAsState()
    val dynamicThemeEnabled by themeProvider.dynamicColor.collectAsState()
    val selectedThemeColor by themeProvider.themeColor.collectAsState()
    val selectedFont by themeProvider.font.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        topBarParams.searchAvailable(null)
        topBarParams.actions {}
        topBarParams.title("Appearance")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Section 1: App Theme
            item {
                ExpressiveListSectionHeader("Theme")
                ExpressiveListCard {
                    ExpressiveListItem(
                        headlineText = "App Theme",
                        supportingText = stringResource(currentTheme.resource),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showThemeDialog = true },
                    )
                }
            }

            // Section 2: Theme Color
            item {
                ExpressiveListSectionHeader("Theme Color")
                ExpressiveListCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        // Dynamic Theme Switch
                        val dynamicSupported = isDynamicColorSupported()
                        ExpressiveListItem(
                            headlineText = "Dynamic Theme",
                            supportingText = if (dynamicSupported) {
                                "Adapt app colors to system wallpaper (Android 12+)"
                            } else {
                                "Only available on Android 12+"
                            },
                            trailingContent = {
                                ExpressiveSwitch(
                                    checked = dynamicSupported && dynamicThemeEnabled,
                                    onCheckedChange = {
                                        if (dynamicSupported) {
                                            themeProvider.setDynamicColor(it)
                                        }
                                    },
                                    enabled = dynamicSupported,
                                )
                            },
                            onClick = if (dynamicSupported) {
                                { themeProvider.setDynamicColor(!dynamicThemeEnabled) }
                            } else null,
                            enabled = dynamicSupported,
                        )

                        ExpressiveListDivider()

                        // Color Presets
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                text = "Preset Colors",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (dynamicSupported && dynamicThemeEnabled) {
                                Text(
                                    text = "Tap a preset to disable dynamic theme and use this color",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(CustomThemeColor.entries, key = { it.key }) { preset ->
                                    val isSelected = !dynamicThemeEnabled && selectedThemeColor == preset
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(preset.color)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(
                                                        width = 3.dp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        shape = CircleShape,
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                if (dynamicSupported && dynamicThemeEnabled) {
                                                    themeProvider.setDynamicColor(false)
                                                }
                                                themeProvider.setThemeColor(preset)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = preset.displayName,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Font
            item {
                ExpressiveListSectionHeader("Typography")
                ExpressiveListCard {
                    ExpressiveListItem(
                        headlineText = "App Font",
                        supportingText = selectedFont.displayName,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showFontDialog = true },
                    )
                }
            }

            // Version Footer
            item {
                ExpressiveListFooter(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // Modal Bottom Sheet: Select App Theme
    if (showThemeDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showThemeDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "Select App Theme",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                CoreAppTheme.entries.forEach { theme ->
                    val isSelected = currentTheme == theme
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                themeProvider.setTheme(theme)
                                showThemeDialog = false
                            },
                        headlineContent = {
                            Text(
                                text = stringResource(theme.resource),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    themeProvider.setTheme(theme)
                                    showThemeDialog = false
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet: Select Font
    if (showFontDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFontDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "Select Font",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                AppFont.entries.forEach { font ->
                    val isSelected = selectedFont == font
                    val fontPreviewFamily = when (font) {
                        AppFont.GoogleSans -> {
                            val f = Font(Res.font.google_sans)
                            remember(f) { FontFamily(f) }
                        }
                        AppFont.UbuntuSans -> {
                            val f = Font(Res.font.ubuntu_sans)
                            remember(f) { FontFamily(f) }
                        }
                        AppFont.Roboto -> {
                            val f = Font(Res.font.roboto)
                            remember(f) { FontFamily(f) }
                        }
                        AppFont.System -> {
                            val f = Font(Res.font.Inter)
                            remember(f) { FontFamily(f) }
                        }
                    }

                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                themeProvider.setFont(font)
                                showFontDialog = false
                            },
                        headlineContent = {
                            Text(
                                text = font.displayName,
                                fontFamily = fontPreviewFamily,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "The quick brown fox jumps over the lazy dog",
                                fontFamily = fontPreviewFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    themeProvider.setFont(font)
                                    showFontDialog = false
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
