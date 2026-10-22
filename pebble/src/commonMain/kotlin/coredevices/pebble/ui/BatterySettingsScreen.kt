package coredevices.pebble.ui

import CommonApiConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import coredevices.pebble.ui.SettingsKeys.KEY_ENABLE_MEMFAULT_UPLOADS
import coredevices.ui.PebbleElevatedButton
import coredevices.ui.PebbleWebview
import coredevices.ui.PebbleWebviewNavigator
import coredevices.ui.PebbleWebviewUrlInterceptor
import coredevices.ui.SignInDialog
import coredevices.util.emailOrNull
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.http.encodeURLParameter
import io.rebble.libpebblecommon.connection.CommonConnectedDevice
import io.rebble.libpebblecommon.connection.ConnectedPebble
import io.rebble.libpebblecommon.connection.LibPebble
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

private const val MOBILE_BATTERY_PATH = "/m/battery"

private enum class BatteryTab {
    Device,
    Cloud,
}

@Composable
fun BatterySettingsScreen(navBarNav: NavBarNav, topBarParams: TopBarParams) {
    val apiConfig = koinInject<CommonApiConfig>()
    val settings = koinInject<Settings>()
    val libPebble = koinInject<LibPebble>()
    val watches by libPebble.watches.collectAsState()
    val connectedWatch = watches.filterIsInstance<CommonConnectedDevice>().firstOrNull()
    val batteryLevel = (connectedWatch as? ConnectedPebble.Battery)?.batteryLevel

    val analyticsEnabled = settings.getBoolean(KEY_ENABLE_MEMFAULT_UPLOADS, true)
    val accountEmail by Firebase.auth.idTokenChanged
        .map { it?.emailOrNull }
        .distinctUntilChanged()
        .collectAsState(Firebase.auth.currentUser?.emailOrNull)

    var selectedTab by remember { mutableStateOf(BatteryTab.Device) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var pageError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        topBarParams.searchAvailable(null)
        topBarParams.title("Battery")
    }

    LaunchedEffect(accountEmail) {
        val email = accountEmail ?: run {
            url = null
            return@LaunchedEffect
        }
        val baseUrl = apiConfig.bugUrl
        if (baseUrl.isNullOrBlank()) {
            url = null
            loadError = "Battery analytics service is not configured"
            return@LaunchedEffect
        }
        val idToken = try {
            Firebase.auth.currentUser?.getIdToken(false)
        } catch (e: Exception) {
            Logger.withTag("BatterySettingsScreen").e(e) { "Failed to mint id token" }
            null
        }
        if (idToken == null) {
            url = null
            loadError = "Sign in to view your battery analytics"
            return@LaunchedEffect
        }
        loadError = null
        url = buildBatteryUrl(baseUrl, email = email, idToken = idToken)
    }

    val interceptor = remember {
        object : PebbleWebviewUrlInterceptor {
            override var navigator: PebbleWebviewNavigator? = null
            override fun onIntercept(url: String, navigator: PebbleWebviewNavigator) = true
        }
    }

    LaunchedEffect(selectedTab, url) {
        if (selectedTab == BatteryTab.Cloud && url != null) {
            topBarParams.actions {
                IconButton(onClick = { interceptor.navigator?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        } else {
            topBarParams.actions { }
        }
    }

    if (showSignInDialog) {
        SignInDialog(onDismiss = { showSignInDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = selectedTab == BatteryTab.Device,
                onClick = { selectedTab = BatteryTab.Device },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { SegmentedButtonDefaults.Icon(active = selectedTab == BatteryTab.Device) },
            ) {
                Text("Device Battery")
            }
            SegmentedButton(
                selected = selectedTab == BatteryTab.Cloud,
                onClick = { selectedTab = BatteryTab.Cloud },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { SegmentedButtonDefaults.Icon(active = selectedTab == BatteryTab.Cloud) },
            ) {
                Text("Cloud Analytics")
            }
        }

        when (selectedTab) {
            BatteryTab.Device -> {
                DeviceBatteryContent(
                    connectedWatch = connectedWatch,
                    batteryLevel = batteryLevel,
                    accountEmail = accountEmail,
                    onManageDevices = { navBarNav.navigateTo(PebbleNavBarRoutes.WatchesRoute) },
                    onOpenWatchSettings = { navBarNav.navigateTo(PebbleNavBarRoutes.WatchSettingsRoute) },
                    onOpenNotifications = { navBarNav.navigateTo(PebbleNavBarRoutes.NotificationsRoute) },
                    onSwitchToCloud = { selectedTab = BatteryTab.Cloud },
                )
            }
            BatteryTab.Cloud -> {
                CloudAnalyticsContent(
                    accountEmail = accountEmail,
                    analyticsEnabled = analyticsEnabled,
                    url = url,
                    loadError = loadError,
                    pageError = pageError,
                    interceptor = interceptor,
                    onSignIn = { showSignInDialog = true },
                    onRetry = { pageError = null },
                    onOpenSettings = {
                        navBarNav.navigateTo(
                            PebbleNavBarRoutes.WatchSettingsCategoryRoute(
                                section = Section.Diagnostics.name,
                                topLevelType = TopLevelType.Phone.name,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DeviceBatteryContent(
    connectedWatch: CommonConnectedDevice?,
    batteryLevel: Int?,
    accountEmail: String?,
    onManageDevices: () -> Unit,
    onOpenWatchSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSwitchToCloud: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WatchBatteryStatusCard(
                connectedWatch = connectedWatch,
                batteryLevel = batteryLevel,
                onManageDevices = onManageDevices,
            )
        }
        item {
            BatteryOptimizationTipsCard()
        }
        item {
            QuickSettingsCard(
                onOpenWatchSettings = onOpenWatchSettings,
                onOpenNotifications = onOpenNotifications,
                onSwitchToCloud = onSwitchToCloud,
                accountEmail = accountEmail,
            )
        }
    }
}

@Composable
private fun WatchBatteryStatusCard(
    connectedWatch: CommonConnectedDevice?,
    batteryLevel: Int?,
    onManageDevices: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (connectedWatch != null) {
            val batteryColor = when {
                batteryLevel == null -> MaterialTheme.colorScheme.primary
                batteryLevel > 50 -> MaterialTheme.colorScheme.primary
                batteryLevel > 20 -> Color(0xFFD97706)
                else -> MaterialTheme.colorScheme.error
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = batteryLevel?.batteryIcon() ?: Icons.Default.Watch,
                                contentDescription = null,
                                tint = batteryColor,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = connectedWatch.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.size(8.dp),
                            ) {}
                            Text(
                                text = "Connected via BLE",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (batteryLevel != null) "$batteryLevel%" else "--%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = batteryColor,
                    )
                    Text(
                        text = if (batteryLevel != null) "Remaining capacity" else "Reading battery state...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (batteryLevel != null) {
                    LinearWavyProgressIndicator(
                        progress = { batteryLevel / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = batteryColor,
                    )
                } else {
                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Estimated Run Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "5-7 Days Typical",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Display Tech",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ultra Low-Power E-Paper",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Watch Connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connect a Pebble watch to monitor real-time battery level and power status.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(18.dp))
                FilledTonalButton(
                    onClick = onManageDevices,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Go to Devices")
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationTipsCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Power,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Battery Optimization Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BatteryTipItem(
                title = "Heart Rate Sampling",
                description = "Periodic rest tracking saves power compared to continuous measurement.",
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 10.dp),
            )
            BatteryTipItem(
                title = "Backlight Duration & Motion",
                description = "Shortening backlight timeout to 3-5 seconds and disabling motion wake saves significant daily charge.",
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 10.dp),
            )
            BatteryTipItem(
                title = "Watchface Animations",
                description = "Watchfaces with sweeping second hands or frequent weather queries draw more battery.",
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 10.dp),
            )
            BatteryTipItem(
                title = "Notification Vibrations",
                description = "Muting non-critical app notifications prevents motor vibration battery drain.",
            )
        }
    }
}

@Composable
private fun BatteryTipItem(title: String, description: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun QuickSettingsCard(
    onOpenWatchSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSwitchToCloud: () -> Unit,
    accountEmail: String?,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = "Power & Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickSettingRow(
                icon = Icons.Default.Settings,
                title = "Watch Settings",
                subtitle = "Tune backlight, vibration, and heart rate",
                onClick = onOpenWatchSettings,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            QuickSettingRow(
                icon = Icons.Default.Warning,
                title = "App Notification Rules",
                subtitle = "Manage notification filters & vibrations",
                onClick = onOpenNotifications,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            QuickSettingRow(
                icon = Icons.Default.CloudSync,
                title = "Cloud Analytics",
                subtitle = if (accountEmail != null) "Signed in as $accountEmail" else "Sign in to sync discharge curves",
                onClick = onSwitchToCloud,
            )
        }
    }
}

@Composable
private fun QuickSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CloudAnalyticsContent(
    accountEmail: String?,
    analyticsEnabled: Boolean,
    url: String?,
    loadError: String?,
    pageError: String?,
    interceptor: PebbleWebviewUrlInterceptor,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (accountEmail == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Pebble Cloud Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sign in to synchronize detailed battery discharge curves, crash telemetry, and diagnostic health logs over time.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onSignIn,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Sign in to Pebble")
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = "What Cloud Analytics Provides",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        BatteryTipItem(
                            title = "Multi-Day Discharge Curves",
                            description = "Visualize voltage drop trends across days to estimate when your watch needs charging.",
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                        BatteryTipItem(
                            title = "Hardware & Brownout Diagnostics",
                            description = "Identify abnormal power spikes caused by buggy watchapps or worn battery cells.",
                        )
                    }
                }
            }
        }
        return
    }

    if (!analyticsEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analytics Disabled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You need 'Send watch analytics' enabled in Diagnostics settings to view battery usage telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Open Settings")
                    }
                }
            }
        }
        return
    }

    if (url == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loadError != null) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = loadError,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSignIn,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Sign in again")
                        }
                    }
                }
            } else {
                LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            }
        }
        return
    }

    if (pageError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Couldn't load battery analytics. Check your connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PebbleWebview(
            url = url,
            interceptor = interceptor,
            modifier = Modifier.fillMaxSize(),
            onPageError = { /* handled via pageError */ },
        )
    }
}

private fun buildBatteryUrl(
    baseUrl: String,
    email: String,
    idToken: String,
): String {
    val root = baseUrl.trimEnd('/').removeSuffix("/api")
    return buildString {
        append(root)
        append(MOBILE_BATTERY_PATH)
        append("?email=").append(email.encodeURLParameter())
        append("&googleIdToken=").append(idToken.encodeURLParameter())
    }
}
