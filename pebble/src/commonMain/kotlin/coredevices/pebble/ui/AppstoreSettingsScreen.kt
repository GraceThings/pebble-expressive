package coredevices.pebble.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import coredevices.pebble.custom.ExpressiveListCard
import coredevices.pebble.custom.ExpressiveListDivider
import coredevices.pebble.custom.ExpressiveListFooter
import coredevices.pebble.custom.ExpressiveListSectionHeader
import coredevices.pebble.custom.ExpressiveSwitch
import coredevices.pebble.custom.ExpressiveToggleListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coredevices.database.AppstoreCollection
import coredevices.database.AppstoreCollectionDao
import coredevices.database.AppstoreSource
import coredevices.database.AppstoreSourceDao
import coredevices.pebble.account.PebbleAccount
import coredevices.pebble.services.PebbleWebServices
import coredevices.pebble.services.isPebbleFeed
import coredevices.pebble.services.isRebbleFeed
import coredevices.ui.M3Dialog
import io.ktor.http.URLProtocol
import io.ktor.http.parseUrl
import io.rebble.libpebblecommon.locker.AppType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class AppstoreSettingsScreenViewModel(
    private val sourceDao: AppstoreSourceDao,
    private val collectionDao: AppstoreCollectionDao,
    private val pebbleWebServices: PebbleWebServices,
) : ViewModel() {
    val sources = sourceDao.getAllSources()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateCollections() {
        AppType.entries.forEach { type ->
            viewModelScope.launch {
                pebbleWebServices.fetchAppStoreHome(type, null, enabledOnly = false, useCache = false)
            }
        }
    }

    val collections: StateFlow<Map<AppstoreSource, Map<AppType, List<AppstoreCollection>>>?> =
        sourceDao.getAllSources().combine(collectionDao.getAllCollectionsFlow()) { sources, collections ->
            sources.associateWith { source ->
                collections.filter { it.sourceId == source.id }
                    .groupBy { it.type }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun removeSource(sourceId: Int) {
        viewModelScope.launch {
            sourceDao.deleteSourceById(sourceId)
        }
    }

    fun updateCollectionEnabled(collection: AppstoreCollection, isEnabled: Boolean) {
        viewModelScope.launch {
            collectionDao.insertOrUpdateCollection(
                collection.copy(enabled = isEnabled)
            )
        }
    }

    fun addSource(title: String, url: String) {
        viewModelScope.launch {
            val source = AppstoreSource(
                title = title,
                url = url.normalizeSourceUrl(),
            )
            sourceDao.insertSource(source)
            updateCollections()
        }
    }
}

@Composable
fun AppstoreSettingsScreen(
    nav: NavBarNav,
    topBarParams: TopBarParams,
    addSourceName: String? = null,
    addSourceUrl: String? = null,
) {
    val uriHandler = LocalUriHandler.current
    val viewModel = koinViewModel<AppstoreSettingsScreenViewModel> { parametersOf(uriHandler) }
    val sources by viewModel.sources.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val pebbleAccount: PebbleAccount = koinInject()
    val sourceDao: AppstoreSourceDao = koinInject()
    val scope = rememberCoroutineScope()
    val pebbleLoggedIn = pebbleAccount.loggedIn
    LaunchedEffect(Unit) {
        topBarParams.searchAvailable(null)
        topBarParams.actions {
            TopBarIconButtonWithToolTip(
                onClick = {
                    viewModel.updateCollections()
                },
                icon = Icons.Filled.Refresh,
                description = "Refresh Collections",
            )
        }
        topBarParams.title("Appstore Sources")
    }

    AppstoreSettingsScreen(
        sources = sources,
        collections = collections,
        onSourceRemoved = viewModel::removeSource,
        onSourceAdded = viewModel::addSource,
        onSourceEnableChange = { sourceId, isEnabled ->
            scope.launch {
                if (sources.firstOrNull {
                        parseUrl(it.url)?.host?.endsWith("rebble.io") ?: false
                    }?.id == sourceId && isEnabled && pebbleLoggedIn.value == null) {
                        if (!uriHandler.open(REBBLE_LOGIN_URI)) {
                            topBarParams.showSnackbar("Couldn't open login page")
                        }
                } else {
                    sourceDao.setSourceEnabled(sourceId, isEnabled)
                    if (isEnabled) {
                        viewModel.updateCollections()
                    }
                }
            }
        },
        onCollectionEnabledChanged = viewModel::updateCollectionEnabled,
        addSourceName = addSourceName,
        addSourceUrl = addSourceUrl,
    )
}

@Composable
fun AppstoreSettingsScreen(
    sources: List<AppstoreSource>,
    collections: Map<AppstoreSource, Map<AppType, List<AppstoreCollection>>>?,
    onSourceRemoved: (Int) -> Unit,
    onSourceAdded: (title: String, url: String) -> Unit,
    onSourceEnableChange: (Int, Boolean) -> Unit,
    onCollectionEnabledChanged: (AppstoreCollection, Boolean) -> Unit,
    addSourceName: String? = null,
    addSourceUrl: String? = null,
) {
    var createSourceOpen by remember { mutableStateOf(addSourceName != null && addSourceUrl != null) }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    createSourceOpen = true
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Source")
            }
        }
    ) { insets ->
        if (createSourceOpen) {
            CreateAppstoreSourceDialog(
                existingUrls = sources.mapTo(mutableSetOf()) { it.url },
                initialTitle = addSourceName.orEmpty(),
                initialUrl = addSourceUrl.orEmpty(),
                onDismissRequest = {
                    createSourceOpen = false
                },
                onSourceCreated = { title, url ->
                    onSourceAdded(title, url)
                    createSourceOpen = false
                }
            )
        }
        val anySourceEnabled = sources.any { it.enabled }
        val pluginCollections = if (collections != null) {
            sources.filter { it.enabled }.flatMap { collections[it]?.get(AppType.Plugin) ?: emptyList() }
        } else emptyList()
        val watchappCollections = if (collections != null) {
            sources.filter { it.enabled }.flatMap { collections[it]?.get(AppType.Watchapp) ?: emptyList() }
        } else emptyList()
        val watchfaceCollections = if (collections != null) {
            sources.filter { it.enabled }.flatMap { collections[it]?.get(AppType.Watchface) ?: emptyList() }
        } else emptyList()

        LazyColumn(
            modifier = Modifier.padding(insets),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            // Section 1: Appstore Sources
            item {
                ExpressiveListSectionHeader(title = "Appstore Sources")
                ExpressiveListCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    sources.forEachIndexed { index, source ->
                        if (index > 0) {
                            ExpressiveListDivider()
                        }
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = source.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = source.url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!source.isPebbleFeed() && !source.isRebbleFeed()) {
                                        IconButton(
                                            onClick = {
                                                onSourceRemoved(source.id)
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Source")
                                        }
                                    }
                                    ExpressiveSwitch(
                                        checked = source.enabled,
                                        onCheckedChange = {
                                            onSourceEnableChange(source.id, it)
                                        }
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                onSourceEnableChange(source.id, !source.enabled)
                            }
                        )
                    }
                }
            }

            // If collections are loading
            if (collections == null && anySourceEnabled) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularWavyProgressIndicator()
                    }
                }
            }

            // Section 2: Plugin Collections
            if (pluginCollections.isNotEmpty()) {
                item {
                    ExpressiveListSectionHeader(title = "Plugin Collections")
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        pluginCollections.forEachIndexed { colIdx, col ->
                            if (colIdx > 0) {
                                ExpressiveListDivider()
                            }
                            ExpressiveToggleListItem(
                                headlineText = col.title,
                                checked = col.enabled,
                                onCheckedChange = {
                                    onCollectionEnabledChanged(col, it)
                                },
                            )
                        }
                    }
                }
            }

            // Section 3: Watchapp Collections
            if (watchappCollections.isNotEmpty()) {
                item {
                    ExpressiveListSectionHeader(title = "Watchapp Collections")
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        watchappCollections.forEachIndexed { colIdx, col ->
                            if (colIdx > 0) {
                                ExpressiveListDivider()
                            }
                            ExpressiveToggleListItem(
                                headlineText = col.title,
                                checked = col.enabled,
                                onCheckedChange = {
                                    onCollectionEnabledChanged(col, it)
                                },
                            )
                        }
                    }
                }
            }

            // Section 4: Watchface Collections
            if (watchfaceCollections.isNotEmpty()) {
                item {
                    ExpressiveListSectionHeader(title = "Watchface Collections")
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        watchfaceCollections.forEachIndexed { colIdx, col ->
                            if (colIdx > 0) {
                                ExpressiveListDivider()
                            }
                            ExpressiveToggleListItem(
                                headlineText = col.title,
                                checked = col.enabled,
                                onCheckedChange = {
                                    onCollectionEnabledChanged(col, it)
                                },
                            )
                        }
                    }
                }
            }

            item {
                ExpressiveListFooter("Appstore Sources")
            }
        }
    }
}

private fun String.normalizeSourceUrl() = trim().trimEnd('/')

@Composable
fun CreateAppstoreSourceDialog(
    existingUrls: Set<String>,
    initialTitle: String = "",
    initialUrl: String = "",
    onDismissRequest: () -> Unit,
    onSourceCreated: (title: String, url: String) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    val normalizedUrl = url.normalizeSourceUrl()
    val parsedUrl = parseUrl(normalizedUrl)
    // Feed urls are concatenated into request paths, so anything past the path is a broken source.
    val urlValid = parsedUrl != null &&
            parsedUrl.protocolOrNull in setOf(URLProtocol.HTTP, URLProtocol.HTTPS) &&
            parsedUrl.encodedQuery.isEmpty() &&
            parsedUrl.fragment.isEmpty() &&
            normalizedUrl !in existingUrls
    M3Dialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(Icons.Filled.Link, contentDescription = null)
        },
        title = {
            Text("Add Appstore Source")
        },
        buttons = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }
            TextButton(
                onClick = {
                    onSourceCreated(title, normalizedUrl)
                },
                enabled = title.isNotBlank() && urlValid
            ) {
                Text("Add")
            }
        }
    ) {
        Column {
            TextField(title, onValueChange = { title = it }, label = { Text("Name") })
            Spacer(Modifier.height(8.dp))
            TextField(url, onValueChange = { url = it }, label = { Text("Source URL") })
        }
    }
}

@Preview
@Composable
fun AppstoreSettingsScreenPreview() {
    val sourceA = AppstoreSource(id = 1, title = "Source 1", url = "https://example.com/source1")
    val sourceB = AppstoreSource(id = 2, title = "Source 2", url = "https://example.com/source2")
    PreviewWrapper {
        AppstoreSettingsScreen(
            sources = listOf(
                sourceA,
                sourceB
            ),
            collections = mapOf(
                sourceA to mapOf(
                    AppType.Watchapp to listOf(
                        AppstoreCollection(
                            sourceId = sourceA.id,
                            title = "Featured Apps",
                            slug = "featured-apps",
                            type = AppType.Watchapp,
                            enabled = true
                        )
                    )
                )
            ),
            onSourceRemoved = {},
            onSourceAdded = { _, _ -> },
            onSourceEnableChange = { _, _ -> },
            onCollectionEnabledChanged = { _, _ -> },
        )
    }
}
