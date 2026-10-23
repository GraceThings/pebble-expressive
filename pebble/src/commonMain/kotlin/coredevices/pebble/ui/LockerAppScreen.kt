package coredevices.pebble.ui

import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize

import CommonRoutes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import coredevices.database.AppstoreSource
import coredevices.database.AppstoreSourceDao
import coredevices.pebble.Platform
import coredevices.pebble.rememberLibPebble
import coredevices.pebble.services.AppstoreService
import coredevices.pebble.services.PEBBLE_FEED_URL
import coredevices.pebble.services.SettingsPageState
import coredevices.pebble.services.isPebbleFeed
import coredevices.ui.ConfirmDialog
import coredevices.ui.PebbleElevatedButton
import io.ktor.http.URLProtocol
import io.ktor.http.parseUrl
import io.rebble.libpebblecommon.connection.ConnectedPebbleDevice
import io.rebble.libpebblecommon.connection.LibPebble
import io.rebble.libpebblecommon.connection.PebbleIdentifier
import io.rebble.libpebblecommon.locker.AppCapability
import io.rebble.libpebblecommon.locker.AppType
import io.rebble.libpebblecommon.locker.orderIndexForInsert
import io.rebble.libpebblecommon.metadata.WatchType
import io.rebble.libpebblecommon.permissions.InstallReview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val logger = Logger.withTag("LockerAppScreen")

class LockerAppViewModel(
//    private val pebbleWebServices: RealPebbleWebServices,
    private val platform: Platform,
) : ViewModel(), KoinComponent {
//    var storeEntries by mutableStateOf<List<AppVariant>?>(null)
    var addedToLocker by mutableStateOf(false)
    var selectedStoreEntry by mutableStateOf<CommonApp?>(null)
    var hearts by mutableStateOf<Int?>(null)
    var loadingFromStore = false

    fun loadAppFromStore(id: String, watchType: WatchType, source: AppstoreSource, useCache: Boolean) {
        val service = get<AppstoreService> { parametersOf(source) }
        viewModelScope.launch {
            loadingFromStore = true
            val result = service.fetchAppStoreApp(id, watchType, useCache)?.data?.firstOrNull()
//            if (result != null) {
//                storeEntries = getAppVariants(Uuid.parse(result.uuid), watchType, sources)
//            }
            selectedStoreEntry = result?.asCommonApp(
                watchType = watchType,
                platform = platform,
                source = source,
                categories = service.cachedCategoriesOrDefaults(AppType.fromString(result.type)),
            ).also {
                hearts = it?.hearts
            }
            loadingFromStore = false
        }
    }

    fun serviceFor(entry: CommonApp) = get<AppstoreService> { parametersOf(entry.appstoreSource) }

//    private suspend fun getAppVariants(
//        uuid: Uuid,
//        watchType: WatchType,
//        storeSources: List<Pair<String, AppstoreSource>>? = null,
//    ): List<AppVariant> {
//        val sources = storeSources ?: pebbleWebServices.searchUuidInSources(uuid)
//        return sources.map { (id, source) ->
//            val service = get<AppstoreService> { parametersOf(source) }
//            //TODO: Search by uuid instead of id to get all variants, id is source-specific except for OG apps
//            viewModelScope.async(Dispatchers.IO) {
//                val result = service.fetchAppStoreApp(id, watchType)
//                val categories = result?.data?.firstOrNull()?.let { service.fetchCategories(AppType.fromString(it.type)!!) }
//                result?.data?.map { appEntry ->
//                    appEntry.asCommonApp(watchType, platform, source, categories)?.let { app ->
//                        AppVariant(
//                            source = source,
//                            app = app
//                        )
//                    }
//                } ?: emptyList()
//            }
//        }.awaitAll().flatten().filterNotNull().sortedBy { it.app.version ?: "0" }.reversed()
//    }
}

//data class AppVariant(
//    val source: AppstoreSource,
//    val app: CommonApp,
//)

@Composable
private fun appstoreSourceFromId(id: Int?): AppstoreSource? {
    val storeSourceDao: AppstoreSourceDao = koinInject()
    val storeSource by produceState<AppstoreSource?>(null, id) {
        value = id?.let { storeSourceDao.getSourceById(it) }
    }
    return storeSource
}

@Composable
fun LockerAppScreen(topBarParams: TopBarParams, uuid: Uuid?, navBarNav: NavBarNav, storeId: String?, storeSourceId: Int?) {
        val scope = rememberCoroutineScope()
        val libPebble = rememberLibPebble()
        val lastConnectedWatch = lastConnectedWatch()
        val runningApp by (lastConnectedWatch as? ConnectedPebbleDevice)?.runningApp?.collectAsState(
            null
        ) ?: mutableStateOf(null)
        val appIsRunning = runningApp == uuid
        val connected = lastConnectedWatch is ConnectedPebbleDevice
        val viewModel = koinViewModel<LockerAppViewModel>()
        val sharedViewModel: SharedLockerViewModel = koinInject()
        sharedViewModel.Init()
        var showRemoveConfirmDialog = remember { mutableStateOf(false) }
        var loadingToWatch by remember { mutableStateOf(false) }

        val lockerEntry = loadLockerEntry(uuid, sharedViewModel.watchType.value)
        val entry = remember(lockerEntry, viewModel.selectedStoreEntry) {
            lockerEntry ?: viewModel.selectedStoreEntry
        }
        val storeEntry = viewModel.selectedStoreEntry
        val commonAppStore =
            remember(storeEntry) { storeEntry?.commonAppType as? CommonAppType.Store }
        val storeSource = appstoreSourceFromId(storeSourceId)
        val platform: Platform = koinInject()
        val urlLauncher = LocalUriHandler.current
        val nativeLockerAddUtil: NativeLockerAddUtil = koinInject()
        var adding by remember { mutableStateOf(false) }
        var loadingSettings by remember { mutableStateOf(false) }
        var storeReview by remember { mutableStateOf<InstallReview?>(null) }
        var fabMenuExpanded by remember { mutableStateOf(false) }

        fun reloadFromStore(useCache: Boolean) {
            if (storeId != null && storeSource != null) {
                viewModel.loadAppFromStore(
                    id = storeId,
                    watchType = sharedViewModel.watchType.value,
                    source = storeSource,
                    useCache = useCache,
                )
            }
        }

        LaunchedEffect(storeId, storeSource, sharedViewModel.watchType.value) {
            reloadFromStore(useCache = true)
        }

        LaunchedEffect(entry) {
            topBarParams.searchAvailable(null)
            topBarParams.actions {
                if (entry != null) {
                    ConfirmDialog(
                        show = showRemoveConfirmDialog,
                        title = "Remove ${entry.title}?",
                        text = "Are you sure you want to remove this app from your Pebble?",
                        onConfirm = {
                            // Don't use local scope: that will die because we moved back
                            GlobalScope.launch {
                                logger.d { "removing app ${entry.uuid}" }
                                topBarParams.showSnackbar("Removing ${entry.title}")
                                val removed = nativeLockerAddUtil.removeFromLocker(storeSource, entry.uuid)
                                logger.d { "removed = $removed" }
                                topBarParams.showSnackbar("Removed ${entry.title}")
                            }
                        },
                        confirmText = "Remove",
                    )
                }
                val showRemove = entry?.commonAppType is CommonAppType.Locker
                if (showRemove) {
                    TopBarIconButtonWithToolTip(
                        onClick = { showRemoveConfirmDialog.value = true },
                        icon = Icons.Filled.Delete,
                        description = "Remove",
                        enabled = !showRemoveConfirmDialog.value,
                    )
                }
            }
            topBarParams.title(entry?.type?.name ?: "")
        }
        if (entry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            return
        }

        fun addToWatch(granted: Map<String, Boolean?>) {
            val storeApp = entry.commonAppType as? CommonAppType.Store ?: return
            GlobalScope.launch {
                val watch = lastConnectedWatch as? ConnectedPebbleDevice
                viewModel.addedToLocker = true
                val addResult = nativeLockerAddUtil.addAppToLocker(
                    storeApp,
                    storeApp.storeSource,
                    granted,
                )
                if (!addResult) {
                    topBarParams.showSnackbar("Failed to add app")
                    adding = false
                    return@launch
                }
                if (watch != null && entry.type != AppType.Plugin) {
                    libPebble.launchApp(
                        entry = entry,
                        snackbarDisplay = topBarParams,
                        connectedIdentifier = watch.identifier,
                    )
                }
                adding = false
            }
        }

        storeReview?.let { review ->
            InstallReviewDialog(review) { granted ->
                storeReview = null
                if (granted != null) addToWatch(granted) else adding = false
            }
        }

        val connectedIdentifier = lastConnectedWatch?.identifier
        val showStartApp = entry.isCompatible && entry.commonAppType.canStartApp() && !appIsRunning
                && !viewModel.addedToLocker && entry.type != AppType.Plugin
        val isStoreApp = entry.commonAppType is CommonAppType.Store && !viewModel.addedToLocker
        val canMoveToTop = entry.commonAppType is CommonAppTypeLocal && entry.commonAppType.order > 0
        val hasSettings = entry.hasSettings()
        val settingsEnabled = remember(entry, connected) {
            entry.type == AppType.Plugin ||
                entry.isCompatible && entry.isSynced() && (connected || entry.commonAppType is CommonAppType.System)
        }

        val actions = remember(
            isStoreApp,
            showStartApp,
            hasSettings,
            canMoveToTop,
            adding,
            connected,
            loadingToWatch,
            loadingSettings,
            settingsEnabled,
            entry,
            connectedIdentifier,
        ) {
            buildList {
                if (isStoreApp) {
                    val addText = if (entry.type == AppType.Plugin) "Add" else "Add To Watch"
                    add(
                        LockerAppAction(
                            label = addText,
                            icon = Icons.Default.Add,
                            enabled = !adding,
                            onClick = {
                                adding = true
                                scope.launch {
                                    val storeApp = entry.commonAppType as CommonAppType.Store
                                    val review = nativeLockerAddUtil.review(storeApp)
                                    if (review == null) addToWatch(emptyMap()) else storeReview = review
                                }
                            }
                        )
                    )
                }
                if (showStartApp) {
                    val startText = if (entry.type == AppType.Watchapp) "Start App" else "Start Watchface"
                    add(
                        LockerAppAction(
                            label = startText,
                            icon = Icons.Default.PlayCircle,
                            enabled = connected && !loadingToWatch,
                            onClick = {
                                GlobalScope.launch {
                                    if (connectedIdentifier != null) {
                                        loadingToWatch = true
                                        libPebble.launchApp(entry, topBarParams, connectedIdentifier)
                                        loadingToWatch = false
                                    }
                                }
                            }
                        )
                    )
                }
                if (hasSettings) {
                    add(
                        LockerAppAction(
                            label = "Settings",
                            icon = Icons.Default.Settings,
                            enabled = settingsEnabled && !loadingSettings,
                            isLoading = loadingSettings,
                            onClick = {
                                if (!loadingSettings) {
                                    loadingSettings = true
                                    scope.launch {
                                        try {
                                            entry.showSettings(navBarNav, libPebble, topBarParams)
                                        } finally {
                                            loadingSettings = false
                                        }
                                    }
                                }
                            }
                        )
                    )
                }
                if (canMoveToTop) {
                    add(
                        LockerAppAction(
                            label = "Move To Top",
                            icon = Icons.Default.VerticalAlignTop,
                            enabled = true,
                            onClick = {
                                scope.launch { libPebble.setAppOrder(entry.uuid, -1) }
                            }
                        )
                    )
                }
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (actions.size == 1) {
                    val action = actions.first()
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (action.enabled) {
                                action.onClick()
                            } else if (!connected && !action.isLoading && (action.label.contains("Start", ignoreCase = true) || action.label == "Settings")) {
                                topBarParams.showSnackbar("No watch connected")
                            }
                        },
                        icon = {
                            if (action.isLoading || loadingSettings) {
                                LoadingIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = LocalContentColor.current,
                                )
                            } else {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                )
                            }
                        },
                        text = {
                            Text(action.label)
                        },
                    )
                } else if (actions.size > 1) {
                    FloatingActionButtonMenu(
                        expanded = fabMenuExpanded,
                        button = {
                            ToggleFloatingActionButton(
                                checked = fabMenuExpanded,
                                onCheckedChange = { if (!loadingSettings) fabMenuExpanded = !fabMenuExpanded },
                            ) {
                                if (loadingSettings) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = LocalContentColor.current,
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (fabMenuExpanded) Icons.Filled.Close else Icons.Filled.MoreVert,
                                        contentDescription = if (fabMenuExpanded) "Close actions menu" else "App actions menu",
                                    )
                                }
                            }
                        },
                    ) {
                        actions.forEach { action ->
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    if (!action.enabled) {
                                        if (!connected && !action.isLoading) {
                                            topBarParams.showSnackbar("No watch connected")
                                        }
                                        return@FloatingActionButtonMenuItem
                                    }
                                    fabMenuExpanded = false
                                    action.onClick()
                                },
                                icon = {
                                    if (action.isLoading) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = action.label,
                                            tint = if (action.enabled) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            },
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        text = action.label,
                                        color = if (action.enabled) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = viewModel.loadingFromStore,
                onRefresh = { reloadFromStore(useCache = false) },
                modifier = Modifier.padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 96.dp)
                ) {
                if (commonAppStore?.headerImageUrl != null) {
                    AsyncImage(
                        model = commonAppStore.headerImageUrl,
                        contentDescription = "banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                }

                // Header Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(if (entry.type == AppType.Watchface) 120.dp else 84.dp),
                        ) {
                            if (entry.type == AppType.Watchface) {
                                AppImage(
                                    entry = entry,
                                    modifier = Modifier.fillMaxSize(),
                                    size = 120.dp,
                                )
                            } else {
                                val appIcon = entry.listImageUrl ?: entry.screenshotImageUrl
                                if (appIcon != null) {
                                    AsyncImage(
                                        model = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().padding(10.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    AppImage(
                                        entry = entry,
                                        modifier = Modifier.fillMaxSize(),
                                        size = 84.dp,
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            // Heart & Category Row
                            val hearts = if (viewModel.hearts != null) viewModel.hearts else entry.hearts
                            val isHearted = entry.isHearted()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (hearts != null && isHearted != null) {
                                    val service = remember(entry) { viewModel.serviceFor(entry) }
                                    val loggedInForHearts = remember(entry) { service.isLoggedIn() }
                                    val addUrl = commonAppStore?.addHeartUrl
                                    val removeUrl = commonAppStore?.removeHeartUrl
                                    var isChanging by remember { mutableStateOf(false) }
                                    val canAddAndRemove =
                                        loggedInForHearts && addUrl != null && removeUrl != null && entry.storeId != null && !isChanging
                                    val icon = if (isHearted) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder

                                    Surface(
                                        shape = CircleShape,
                                        color = if (isHearted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.clickable(enabled = canAddAndRemove) {
                                            if (entry.storeId == null) return@clickable
                                            scope.launch {
                                                if (canAddAndRemove) {
                                                    if (!isHearted) {
                                                        isChanging = true
                                                        val result = service.addHeart(addUrl, entry.storeId)
                                                        isChanging = false
                                                        if (result) {
                                                            viewModel.hearts?.let { viewModel.hearts = it + 1 }
                                                            reloadFromStore(useCache = false)
                                                        } else {
                                                            topBarParams.showSnackbar("Failed to heart app")
                                                        }
                                                    } else {
                                                        isChanging = true
                                                        val result = service.removeHeart(removeUrl, entry.storeId)
                                                        isChanging = false
                                                        if (result) {
                                                            viewModel.hearts?.let { viewModel.hearts = it - 1 }
                                                            reloadFromStore(useCache = false)
                                                        } else {
                                                            topBarParams.showSnackbar("Failed to remove heart")
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = "Hearts",
                                                tint = if (isHearted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$hearts",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isHearted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                if (entry.type == AppType.Watchapp && entry.category != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.clickable {
                                            if (entry.categorySlug != null && storeSource != null) {
                                                navBarNav.navigateTo(
                                                    PebbleNavBarRoutes.AppStoreCollectionRoute(
                                                        sourceId = storeSource.id,
                                                        path = "category/${entry.categorySlug}",
                                                        title = entry.category
                                                    )
                                                )
                                            }
                                        },
                                    ) {
                                        Text(
                                            text = entry.category,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }

                            val watchName = lastConnectedWatch?.displayName() ?: ""
                            val onWatchText = if (entry.type == AppType.Plugin) {
                                null
                            } else if (entry.isCompatible && entry.isSynced()) {
                                if (appIsRunning) "Running On Watch $watchName" else "On Watch $watchName"
                            } else if (!entry.isCompatible) {
                                "Not Compatible with $watchName"
                            } else if (entry.commonAppType !is CommonAppType.Store) {
                                "Not On Watch $watchName"
                            } else {
                                null
                            }
                            if (onWatchText != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    entry.CompatibilityWarning(topBarParams)
                                    Text(
                                        text = onWatchText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                if (commonAppStore?.settingsPageState == SettingsPageState.PageDoesntLoad) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "This app's settings page may not work any more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                val screenshotsToDisplay = remember(storeEntry) {
                    when (storeEntry?.commonAppType) {
                        is CommonAppType.Store -> storeEntry.commonAppType.allScreenshotUrls.drop(1)
                        else -> emptyList()
                    }
                }

                if (screenshotsToDisplay.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            text = "Screenshots",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(screenshotsToDisplay, key = { it }) { screenshotUrl ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.size(130.dp),
                                ) {
                                    AsyncImage(
                                        model = screenshotUrl,
                                        contentDescription = "Screenshot",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                            }
                        }
                    }
                }

                val pluginsEnabled = libPebble.config.collectAsState().value.watchConfig.enablePlugins
                val storeAppObj = (entry.commonAppType as? CommonAppType.Store)?.storeApp
                if (entry.commonAppType is CommonAppType.Locker) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        AppPermissionsSection(entry.uuid)
                    }
                } else if (pluginsEnabled && storeAppObj != null) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        StorePermissionsSection(storeAppObj.requestedPermissions())
                    }
                } else if (storeEntry?.capabilities?.isNotEmpty() == true) {
                    FlowRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        storeEntry.capabilities.forEach { permission ->
                            PermissionItem(permission, topBarParams)
                        }
                    }
                }

                // Metadata Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        if (platform == Platform.Android && entry.androidCompanion != null) {
                            M3EPropertyRow(
                                name = "COMPANION",
                                value = entry.androidCompanion.name,
                                onClick = entry.androidCompanion.url?.let { url ->
                                    { urlLauncher.open(url); Unit }
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        val description = viewModel.selectedStoreEntry?.description ?: entry.description
                        if (description != null && description.isNotBlank()) {
                            M3EPropertyRow(
                                name = "DESCRIPTION",
                                value = description,
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        M3EPropertyRow(
                            name = "DEVELOPER",
                            value = entry.developerName,
                            onClick = if (entry.developerId != null && storeSource != null) {
                                {
                                    val developerId = entry.developerId
                                    navBarNav.navigateTo(
                                        PebbleNavBarRoutes.AppStoreCollectionRoute(
                                            sourceId = storeSource.id,
                                            path = "dev/$developerId",
                                            title = "Developer: ${entry.developerName}"
                                        )
                                    )
                                }
                            } else {
                                null
                            },
                            onClickIcon = Icons.AutoMirrored.Default.ArrowForward,
                        )

                        entry.version?.let { version ->
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            val sideloadedText =
                                if (entry.commonAppType is CommonAppType.Locker && entry.commonAppType.sideloaded) {
                                    " (sideloaded)"
                                } else {
                                    ""
                                }
                            val updatedDateText = if (commonAppStore?.publishedDate != null) {
                                " - ${
                                    PUBLISHED_DATE_FORMAT.format(
                                        commonAppStore.publishedDate
                                            .toLocalDateTime(TimeZone.currentSystemDefault())
                                    ).uppercase()
                                }"
                            } else {
                                ""
                            }
                            M3EPropertyRow(
                                name = "VERSION",
                                value = "$version$sideloadedText$updatedDateText"
                            )
                        }

                        (viewModel.selectedStoreEntry?.sourceLink ?: entry.sourceLink)?.let { sourceLink ->
                            if (sourceLink.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                M3EPropertyRow(
                                    name = "SOURCE CODE",
                                    value = "External Link",
                                    onClick = { urlLauncher.open(sourceLink) }
                                )
                            }
                        }

                        commonAppStore?.developerLink?.let { developerLink ->
                            if (developerLink.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                M3EPropertyRow(
                                    name = "WEBSITE LINK",
                                    value = "External Link",
                                    onClick = { urlLauncher.open(developerLink) }
                                )
                            }
                        }

                        val contactStoreId = entry.storeId
                        if (
                            commonAppStore?.contactable == true &&
                            commonAppStore.storeSource.isPebbleFeed() &&
                            contactStoreId != null
                        ) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            M3EPropertyRow(
                                name = "CONTACT DEVELOPER",
                                value = "Send Message",
                                onClick = {
                                    navBarNav.navigateTo(
                                        PebbleRoutes.ContactDeveloperRoute(
                                            appId = contactStoreId,
                                            appTitle = entry.title,
                                        )
                                    )
                                },
                                onClickIcon = Icons.AutoMirrored.Default.ArrowForward,
                            )
                        }

                        commonAppStore?.changelog?.let { changelog ->
                            if (changelog.isNotEmpty()) {
                                val show = remember { mutableStateOf(false) }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                M3EPropertyRow(
                                    name = "CHANGELOG",
                                    value = "View Changelog",
                                    onClick = { show.value = true },
                                    onClickIcon = Icons.AutoMirrored.Default.ArrowForward,
                                )
                                if (show.value) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            show.value = false
                                        },
                                        title = { Text("Changelog") },
                                        text = {
                                            LazyColumn {
                                                items(changelog) { item ->
                                                    Row(modifier = Modifier.padding(5.dp)) {
                                                        Text(
                                                            item.version ?: "Unknown version",
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        val date = remember(item.publishedDate) {
                                                            item.publishedDate?.let {
                                                                PUBLISHED_DATE_FORMAT.format(
                                                                    it.toLocalDateTime(TimeZone.currentSystemDefault())
                                                                ).uppercase()
                                                            } ?: ""
                                                        }
                                                        Text(
                                                            date,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        item.releaseNotes ?: "\n",
                                                        modifier = Modifier.padding(5.dp)
                                                    )
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                show.value = false
                                            }) { Text("Dismiss") }
                                        }
                                    )
                                }
                            }
                        }

                        storeSource?.let { storeSource ->
                            val storeId = entry.storeId
                            val onClick = if (storeId != null && entry.appstoreSource?.url == PEBBLE_FEED_URL) {
                                {
                                    urlLauncher.open("https://apps.repebble.com/$storeId")
                                    Unit
                                }
                            } else {
                                null
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            M3EPropertyRow(
                                name = "STORE",
                                value = storeSource.title,
                                onClick = onClick,
                            )
                        }
                    }
                }
            }
            if (loadingSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3EPropertyRow(
    name: String,
    value: String,
    onClick: (() -> Unit)? = null,
    onClickIcon: ImageVector = Icons.AutoMirrored.Default.Launch,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) {
            Modifier.fillMaxWidth().clickable(onClick = onClick)
        } else {
            Modifier.fillMaxWidth()
        },
        overlineContent = {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        headlineContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingContent = if (onClick != null) {
            {
                Icon(
                    imageVector = onClickIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else null,
    )
}

fun UriHandler.open(url : String): Boolean {
    val urlParsed = parseUrl(url)
    if (urlParsed != null && urlParsed.protocolOrNull in listOf(URLProtocol.HTTP, URLProtocol.HTTPS)) {
        try {
            openUri(url)
            return true
        } catch (e: Exception) {
            // No browser / VIEW handler installed (Android Go, stripped ROMs, DPC policy).
            logger.w(e) { "Failed to open URL: $url" }
            return false
        }
    } else {
        logger.w { "Not opening invalid URL: $url" }
        return false
    }
}

@Composable
private fun PropertyRow(
    name: String,
    value: String,
    onClick: (() -> Unit)? = null,
    onClickIcon: ImageVector = Icons.AutoMirrored.Default.Launch,
    multiRow: Boolean = false,
    nameModifier: Modifier = Modifier,
) {
    Row(modifier = Modifier.padding(5.dp).let{
        if (onClick != null) {
            it.then(Modifier.clickable(onClick = onClick))
        } else {
            it
        }
    }) {
        Text(
            text = name,
            color = Color.Gray,
            modifier = nameModifier,
            maxLines = 1
        )
        if (!multiRow) {
            Text(text = value, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
        }
        if (onClick != null) {
            Icon(
                imageVector = onClickIcon,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp).align(Alignment.CenterVertically)
            )
        }
    }
    if (multiRow) {
        Text(text = value, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun PermissionItem(permission: AppCapability, topBarParams: TopBarParams) {
    AssistChip(
        onClick = {
            topBarParams.showSnackbar(permission.description())
        },
        label = { Text(permission.name()) },
        leadingIcon = { Icon(permission.icon(), null) },
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

fun AppCapability.icon(): ImageVector = when (this) {
    AppCapability.Health -> Icons.Default.HealthAndSafety
    AppCapability.Location -> Icons.Default.LocationSearching
    AppCapability.Timeline -> Icons.Default.LocationOn
    AppCapability.WebSignIn -> Icons.Default.AccountCircle
}

fun AppCapability.name(): String = when (this) {
    AppCapability.Health -> "Health"
    AppCapability.Location -> "Location"
    AppCapability.Timeline -> "Timeline"
    AppCapability.WebSignIn -> "Web sign-in"
}

fun AppCapability.description(): String = when (this) {
    AppCapability.Health -> "Can access health data"
    AppCapability.Location -> "Can access location"
    AppCapability.Timeline -> "Can create timeline pins"
    AppCapability.WebSignIn -> "Can ask you to sign in to web accounts"
}

suspend fun LibPebble.launchApp(
    entry: CommonApp,
    snackbarDisplay: SnackbarDisplay,
    connectedIdentifier: PebbleIdentifier
): Boolean {
    logger.d { "launchApp: ${entry.uuid} - ${entry.title}" }
    val typeText = when (entry.type) {
        AppType.Watchface -> "Watchface"
        AppType.Watchapp -> "Watchapp"
        AppType.Plugin -> "Plugin"
    }
    if (!entry.isSynced()) {
        try {
            withTimeout(15.seconds) {
                snackbarDisplay.showSnackbar("Waiting to sync $typeText to watch...")
                setAppOrder(entry.uuid, orderIndexForInsert(entry.type))
            }
        } catch (_: TimeoutCancellationException) {
            logger.w { "timed out waiting for order change to sync to watch" }
            snackbarDisplay.showSnackbar("$typeText failed to sync to watch")
            return false
        }
    }
    if (!waitUntilAppSyncedToWatch(entry.uuid, connectedIdentifier, 15.seconds)) {
        logger.w { "timed out waiting for blobdb item to sync to watch" }
        snackbarDisplay.showSnackbar("$typeText failed to sync to watch")
        return false
    }
    // Give it a small bit of time to settle
    delay(0.5.seconds)
    snackbarDisplay.showSnackbar("Loading $typeText")
    val launched = withTimeoutOrNull(30.seconds) {
        launchApp(entry.uuid)
        true
    }
    logger.d { "launched = $launched" }
    if (launched == null) {
        snackbarDisplay.showSnackbar("$typeText failed to load")
        return false
    }
    return true
}

fun SnackbarHostState.showSnackbar(scope: CoroutineScope, message: String) {
    scope.launch {
        showSnackbar(message)
    }
}

fun CommonApp.hasSettings(): Boolean = when (commonAppType) {
    is CommonAppType.Locker -> commonAppType.configurable
    is CommonAppType.System -> false
    is CommonAppType.Store -> false
}

suspend fun CommonApp.showSettings(
    navBarNav: NavBarNav,
    libPebble: LibPebble,
    topBarParams: TopBarParams,
) {
    when (commonAppType) {
        is CommonAppType.Locker -> {
            if (type == AppType.Plugin) {
                // A plugin's config page is phone-side; no watch or PKJS session to launch.
                val url = libPebble.appConfigPageUrl(uuid) ?: run {
                    topBarParams.showSnackbar("$title has no settings")
                    return
                }
                WatchappSettingsUrlCache.put(uuid.toString(), url)
                navBarNav.navigateTo(
                    CommonRoutes.WatchappSettingsRoute(uuid = uuid.toString(), title = title)
                )
                return
            }
            val watch = libPebble.watches.value.filterIsInstance<ConnectedPebbleDevice>()
                .firstOrNull()
            //TODO: Handle multiple watches connected, selector?
            if (watch == null) {
                logger.w("No connected watch found, cannot show settings")
                topBarParams.showSnackbar("No connected watch")
                return
            }
            val session = if (watch.currentPKJSSession.value?.uuid == uuid) {
                watch.currentPKJSSession.value
            } else {
                logger.d("Launching app $uuid for pkjs settings...")
                val launched = libPebble.launchApp(this, topBarParams, watch.identifier)
                if (!launched) {
                    return
                }
                withTimeoutOrNull(15.seconds) {
                    watch.currentPKJSSession.first { it?.uuid == uuid }
                }
            }
            if (session?.uuid != uuid) {
                logger.e("PKJS session UUID mismatch: expected $uuid, got ${session?.uuid}")
                topBarParams.showSnackbar("$title settings failed to open")
            } else {
                logger.d { "Opening app settings for $uuid" }
                val url = libPebble.appConfigPageUrl(uuid) ?: session.requestConfigurationUrl()
                url ?: run {
                    logger.e("No configuration URL returned for app $uuid")
                    topBarParams.showSnackbar("$title settings failed to open")
                    return
                }
                logger.d { "Got app settings URL" }
                WatchappSettingsUrlCache.put(uuid.toString(), url)
                navBarNav.navigateTo(
                    CommonRoutes.WatchappSettingsRoute(
                        uuid = uuid.toString(),
                        title = title,
                    )
                )
            }
        }

        is CommonAppType.System -> Unit

        is CommonAppType.Store -> Unit
    }
}

private val PUBLISHED_DATE_FORMAT = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(Padding.NONE)
    char(',')
    char(' ')
    year()
}

private data class LockerAppAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val isLoading: Boolean = false,
    val onClick: () -> Unit,
)
