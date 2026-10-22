package coredevices.coreapp.ui.screens

import CoreNav
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import coredevices.pebble.Platform
import coredevices.pebble.services.AppStoreHomeResult
import coredevices.pebble.services.LanguagePackRepository
import coredevices.pebble.services.PebbleWebServices
import coredevices.pebble.services.StoreOnboarding
import coredevices.pebble.ui.CommonAppType
import coredevices.pebble.ui.InstallReviewDialog
import coredevices.pebble.ui.LanguageDialog
import coredevices.pebble.ui.NativeLockerAddUtil
import coredevices.pebble.ui.NativeWatchfaceMainContent
import coredevices.pebble.ui.SettingsIds.EnableActivityInsights
import coredevices.pebble.ui.SettingsIds.EnableHealthPlatformSync
import coredevices.pebble.ui.SettingsIds.EnableHealthTracking
import coredevices.pebble.ui.SettingsIds.EnableSleepInsights
import coredevices.pebble.ui.SettingsIds.HealthAge
import coredevices.pebble.ui.SettingsIds.HealthGenderId
import coredevices.pebble.ui.SettingsIds.HealthHeight
import coredevices.pebble.ui.SettingsIds.HealthImperialUnits
import coredevices.pebble.ui.SettingsIds.HealthWeight
import coredevices.pebble.ui.SettingsIds.HrmActivityTracking
import coredevices.pebble.ui.SettingsIds.HrmEnabled
import coredevices.pebble.ui.SettingsIds.HrmMeasurementInterval
import coredevices.pebble.ui.SettingsIds.OfflineSpeechRecognition
import coredevices.pebble.ui.SettingsItemsState
import coredevices.pebble.ui.SnackbarDisplay
import coredevices.pebble.ui.WatchOnboardingFinished
import coredevices.pebble.ui.allCollectionUuids
import coredevices.pebble.ui.asCommonApp
import coredevices.pebble.ui.connectedWatch
import coredevices.pebble.ui.languagePackInstalled
import coredevices.pebble.ui.launchApp
import coredevices.pebble.ui.rememberSettingsItemsState
import coredevices.ui.CoreLinearProgressIndicator
import io.rebble.libpebblecommon.connection.ConnectedPebbleDevice
import io.rebble.libpebblecommon.connection.ConnectedPebbleDeviceInRecovery
import io.rebble.libpebblecommon.connection.FirmwareUpdateCheckResult
import io.rebble.libpebblecommon.connection.LibPebble
import io.rebble.libpebblecommon.connection.endpointmanager.FirmwareUpdater
import io.rebble.libpebblecommon.connection.endpointmanager.LanguagePackInstallState
import io.rebble.libpebblecommon.connection.endpointmanager.installing
import io.rebble.libpebblecommon.database.entity.BoolWatchPref
import io.rebble.libpebblecommon.locker.AppType
import io.rebble.libpebblecommon.metadata.WatchHardwarePlatform
import io.rebble.libpebblecommon.metadata.WatchType
import io.rebble.libpebblecommon.metadata.supportsHrm
import io.rebble.libpebblecommon.packets.ProtocolCapsFlag
import io.rebble.libpebblecommon.permissions.InstallReview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import theme.AppTheme
import theme.coreGrey
import theme.coreOrange
import theme.onboardingScheme
import kotlin.time.Duration.Companion.seconds

private val logger = Logger.withTag("OnboardingScreen")

@Composable
fun WatchOnboardingScreen(
    coreNav: CoreNav,
) {
    val scope = rememberCoroutineScope()
    val connectedWatch = connectedWatch()
    val pebbleDevice = connectedWatch as? ConnectedPebbleDevice
    val pebbleWebServices: PebbleWebServices = koinInject()
    var loadingHomes by remember { mutableStateOf(true) }
    val pebbleStoreHomes = remember { mutableStateMapOf<AppType, AppStoreHomeResult?>() }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var haveUpdatedFirmware by remember { mutableStateOf(false) }
    var haveStartedFwupSinceLastConnection by remember { mutableStateOf(false) }
    val watchOnboardingFinished: WatchOnboardingFinished = koinInject()
    val snackbarDisplay =
        remember { SnackbarDisplay { scope.launch { snackbarHostState.showSnackbar(message = it) } } }
    val settings = rememberSettingsItemsState(
        navBarNav = null,
        snackbarDisplay = snackbarDisplay,
    )

    val targetWatchType = connectedWatch?.watchType?.watchType ?: WatchType.BASALT

    LaunchedEffect(targetWatchType) {
        launch {
            delay(6.seconds)
            // Don't block entire screen on loading apps - try to make it feel responsive
            loadingHomes = false
        }
        val results = pebbleWebServices.fetchPebbleAppStoreHomes(
            targetWatchType,
            useCache = true
        )
        pebbleStoreHomes.clear()
        pebbleStoreHomes.putAll(results)
        loadingHomes = false
    }

    val scrollState = rememberScrollState()
    MaterialTheme(colorScheme = onboardingScheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { windowInsets ->
            Box(modifier = Modifier.padding(windowInsets).fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(scrollState)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Hero Header
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Watch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Get Started!",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Let's personalize your Pebble watch experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (connectedWatch is ConnectedPebbleDeviceInRecovery) {
                        val firmwareUpdateAvailable = connectedWatch.firmwareUpdateAvailable.result
                        if (firmwareUpdateAvailable !is FirmwareUpdateCheckResult.FoundUpdate) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White,
                                    contentColor = coreGrey,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CircularWavyProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = coreOrange,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Checking for PebbleOS updates...",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = coreGrey,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { coreNav.goBack() },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = coreOrange,
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Text("Skip Setup")
                                    }
                                }
                            }
                            return@Scaffold
                        }

                        LaunchedEffect(haveStartedFwupSinceLastConnection) {
                            if (!haveStartedFwupSinceLastConnection) {
                                logger.d { "Starting firmware update from onboarding screen" }
                                haveStartedFwupSinceLastConnection = true
                                haveUpdatedFirmware = true
                                connectedWatch.updateFirmware(firmwareUpdateAvailable)
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White,
                                contentColor = coreGrey,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Updating PebbleOS to latest version...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = coreGrey,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                val progress = (connectedWatch.firmwareUpdateState as? FirmwareUpdater.FirmwareUpdateStatus.InProgress)?.progress?.collectAsState()
                                if (progress != null) {
                                    CoreLinearProgressIndicator(
                                        progress = { progress.value },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                    )
                                }
                            }
                        }
                        SectionDivider()
                    }

                    if (pebbleDevice == null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Watch,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "Previewing setup. Connect your Pebble anytime in Devices.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (loadingHomes) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading watchfaces & apps...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                        return@Scaffold
                    }

                    // Watchfaces Carousel
                    OnboardingAppCarousel(
                        header = "Add some Watchfaces",
                        storeHome = pebbleStoreHomes[AppType.Watchface],
                        watch = pebbleDevice,
                        footerText = null,
                        snackbarDisplay = snackbarDisplay,
                    )

                    SectionDivider()

                    // Apps Carousel
                    OnboardingAppCarousel(
                        header = "Add some Apps",
                        storeHome = pebbleStoreHomes[AppType.Watchapp],
                        watch = pebbleDevice,
                        footerText = "Get more apps from the Pebble App Store!",
                        snackbarDisplay = snackbarDisplay,
                    )

                    SectionDivider()

                    // Language Pack Section
                    val languagePackRepository: LanguagePackRepository = koinInject()
                    val languagePackInstalled = pebbleDevice?.languagePackInstalled(languagePackRepository)
                    val installingLanguagePack =
                        pebbleDevice?.languagePackInstallState?.installing()

                    AppTheme {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White,
                                contentColor = coreGrey,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = coreOrange.copy(alpha = 0.12f),
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Language,
                                                contentDescription = null,
                                                tint = coreOrange,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Language Pack",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = coreGrey,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (installingLanguagePack != null) {
                                            Text(
                                                text = "Installing $installingLanguagePack...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = coreOrange,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        } else if (languagePackInstalled != null) {
                                            Text(
                                                text = "Installed: $languagePackInstalled",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = coreGrey.copy(alpha = 0.7f),
                                            )
                                        } else {
                                            Text(
                                                text = "English (Built-in)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = coreGrey.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }

                                val languagePackInstallState = pebbleDevice?.languagePackInstallState as? LanguagePackInstallState.Installing
                                if (languagePackInstallState != null) {
                                    val progress by languagePackInstallState.progress.collectAsState()
                                    Spacer(modifier = Modifier.height(14.dp))
                                    LinearWavyProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = coreOrange,
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (pebbleDevice != null) {
                                            showLanguageDialog = true
                                        } else {
                                            snackbarDisplay.showSnackbar("Connect a Pebble watch to install language packs")
                                        }
                                    },
                                    enabled = installingLanguagePack == null,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = coreOrange,
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Choose Language",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    }

                    if (showLanguageDialog && pebbleDevice != null) {
                        AppTheme {
                            LanguageDialog(
                                pebbleDevice,
                                onDismissRequest = { showLanguageDialog = false })
                        }
                    }

                    // Speech Recognition Card
                    if (pebbleDevice == null || pebbleDevice.capabilities.contains(ProtocolCapsFlag.SupportsAppDictation)) {
                        SectionDivider()
                        AppTheme {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White,
                                    contentColor = coreGrey,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = coreOrange.copy(alpha = 0.12f),
                                            modifier = Modifier.size(44.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = coreOrange,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Speech Recognition",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = coreGrey,
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Voice dictation & transcription mode",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = coreGrey.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    settings.Show(OfflineSpeechRecognition)
                                }
                            }
                        }
                    }

                    // Watch Preferences Card (BlobDB supported or default)
                    if (pebbleDevice == null || pebbleDevice.capabilities.contains(ProtocolCapsFlag.SupportsBlobDbVersion)) {
                        SectionDivider()
                        AppTheme {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White,
                                    contentColor = coreGrey,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = coreOrange.copy(alpha = 0.12f),
                                            modifier = Modifier.size(44.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = null,
                                                    tint = coreOrange,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Watch Preferences",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = coreGrey,
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Clock format & health tracking",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = coreGrey.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    settings.Show(BoolWatchPref.Clock24h.id)
                                    settings.Show(EnableHealthTracking)
                                    if (pebbleDevice?.color?.supportsHrm() == true || pebbleDevice == null) {
                                        settings.Show(HrmEnabled)
                                        settings.Show(HrmMeasurementInterval)
                                        settings.Show(HrmActivityTracking)
                                    }
                                    settings.Show(EnableActivityInsights)
                                    settings.Show(EnableSleepInsights)
                                    settings.Show(HealthImperialUnits)
                                    settings.Show(HealthHeight)
                                    settings.Show(HealthWeight)
                                    settings.Show(HealthAge)
                                    settings.Show(HealthGenderId)
                                    settings.Show(EnableHealthPlatformSync)
                                }
                            }
                        }
                    }

                    // Bottom Finish Section
                    SectionDivider()

                    Text(
                        text = "You can change any of these settings later in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                watchOnboardingFinished.finished.trySend(Unit)
                                coreNav.goBack()
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = coreOrange,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .height(52.dp),
                    ) {
                        Text(
                            text = "Finish Setup",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    color: Color = Color.White.copy(alpha = 0.5f),
): Modifier = drawWithContent {
    drawContent()
    if (scrollState.maxValue > 0) {
        val viewportHeight = size.height
        val totalHeight = viewportHeight + scrollState.maxValue.toFloat()
        val scrollbarHeight = (viewportHeight / totalHeight) * viewportHeight
        val scrollbarOffsetY = (scrollState.value.toFloat() / totalHeight) * viewportHeight
        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
            size = Size(width.toPx(), scrollbarHeight),
        )
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
        color = Color.White.copy(alpha = 0.25f),
    )
}

@Composable
fun SettingsItemsState?.Show(id: String) {
    if (this == null) {
        return
    }
    val setting = remember(this) {
        rawSettingsItems.find { it.id == id }
    }
    if (setting == null || !setting.show()) {
        return
    }
    setting.Item()
}

@Composable
fun OnboardingAppCarousel(
    header: String,
    storeHome: AppStoreHomeResult?,
    watch: ConnectedPebbleDevice?,
    footerText: String?,
    snackbarDisplay: SnackbarDisplay,
) {
    if (storeHome == null) {
        return
    }
    val platform: Platform = koinInject()
    val libPebble: LibPebble = koinInject()
    val allCollectionUuids = allCollectionUuids()
    // Only use initial list of in-collection IDs (i.e. don't remove from list when they add from this screen)
    val initialAllCollectionUuids = remember(allCollectionUuids != null) { allCollectionUuids.orEmpty() }
    val nativeLockerAddUtil: NativeLockerAddUtil = koinInject()
    val scope = rememberCoroutineScope()
    val watchType = watch?.watchType?.watchType ?: WatchType.BASALT
    val apps = remember(storeHome, watchType, initialAllCollectionUuids) {
        storeHome.result.onboarding?.forType(watchType)?.mapNotNull { appId ->
            storeHome.result.applications.find { app ->
                app.id == appId
            }?.asCommonApp(
                watchType,
                platform,
                storeHome.source,
                storeHome.result.categories
            )
        }?.filter {
            it.isNativelyCompatible && it.uuid !in initialAllCollectionUuids
        }?.take(5)
    }
    if (apps.isNullOrEmpty()) {
        return
    }

    // Expressive Section Header
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (header.contains("Watchface", ignoreCase = true)) Icons.Default.Watch else Icons.Default.Apps,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = header,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Material 3 Expressive Multi-Browse Carousel (with dynamic expanding/shrinking items)
    val carouselState = rememberCarouselState { apps.size }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 145.dp,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 4.dp),
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) { index ->
        val entry = apps[index]
        var added by remember { mutableStateOf(false) }
        var review by remember { mutableStateOf<InstallReview?>(null) }
        val commonAppStore = entry.commonAppType as? CommonAppType.Store ?: return@HorizontalMultiBrowseCarousel
        val isAdded = entry.uuid in allCollectionUuids.orEmpty()

        AppTheme {
            Card(
                modifier = Modifier
                    .maskClip(RoundedCornerShape(20.dp))
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                    contentColor = coreGrey,
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp,
                ),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    NativeWatchfaceMainContent(
                        entry = entry,
                        highlightInLocker = false,
                        topBarParams = null,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isAdded) {
                        FilledTonalButton(
                            onClick = {
                                added = false
                                GlobalScope.launch {
                                    nativeLockerAddUtil.removeFromLocker(
                                        commonAppStore.storeSource,
                                        entry.uuid,
                                    )
                                    logger.v { "Remove from locker from watch onboarding ${commonAppStore.storeApp?.title}" }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Added",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    } else {
                        fun add(granted: Map<String, Boolean?>) {
                            added = true
                            GlobalScope.launch {
                                val addResult = nativeLockerAddUtil.addAppToLocker(
                                    commonAppStore,
                                    commonAppStore.storeSource,
                                    granted,
                                )
                                logger.v { "Add to locker from watch onboarding ${commonAppStore.storeApp?.title} result=$addResult" }
                                if (!addResult) {
                                    snackbarDisplay.showSnackbar("Failed to add app")
                                    return@launch
                                }
                                if (watch != null) {
                                    libPebble.launchApp(
                                        entry = entry,
                                        snackbarDisplay = NoOpSnackbarDisplay,
                                        connectedIdentifier = watch.identifier,
                                    )
                                }
                            }
                        }
                        review?.let { pending ->
                            InstallReviewDialog(pending) { granted ->
                                review = null
                                if (granted != null) add(granted) else added = false
                            }
                        }
                        Button(
                            onClick = {
                                added = true
                                scope.launch {
                                    val appReview = nativeLockerAddUtil.review(commonAppStore)
                                    if (appReview == null) add(emptyMap()) else review = appReview
                                }
                            },
                            enabled = !added,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = coreOrange,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }

    footerText?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

object NoOpSnackbarDisplay : SnackbarDisplay {
    override fun showSnackbar(message: String) {}
}

fun StoreOnboarding.forType(watchType: WatchType): List<String>? = when (watchType) {
    WatchType.APLITE -> aplite
    WatchType.BASALT -> basalt
    WatchType.CHALK -> chalk
    WatchType.DIORITE -> diorite
    WatchType.EMERY -> emery
    WatchType.FLINT -> flint
    WatchType.GABBRO -> gabbro
}
