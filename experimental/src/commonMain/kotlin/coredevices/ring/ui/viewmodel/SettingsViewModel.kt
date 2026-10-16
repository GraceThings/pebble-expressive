package coredevices.ring.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import coredevices.libindex.device.IndexDeviceManager
import coredevices.libindex.device.InterviewedIndexDevice
import coredevices.libindex.device.KnownIndexDevice
import coredevices.libindex.device.RSSIMeasurement
import coredevices.libindex.di.LibIndexCoroutineScope
import coredevices.ring.RingDelegate
import coredevices.ring.agent.IndexActionsRepository
import coredevices.ring.agent.integrations.providers.IntegrationProviderRepository
import coredevices.ring.agent.integrations.providers.IntegrationSupport
import coredevices.ring.agent.integrations.providers.visibleDestinations
import coredevices.ring.agent.pebble_plugin.PluginGrant
import coredevices.ring.agent.pebble_plugin.PluginStore
import coredevices.ring.agent.pebble_plugin.StorePlugin
import coredevices.ring.data.NoteShortcutType
import coredevices.indexai.data.entity.mcp.HttpMcpServerEntity
import coredevices.ring.database.Preferences
import coredevices.ring.database.room.repository.McpServerRepository
import coredevices.ring.model.CactusModelProvider
import coredevices.ring.service.button.GestureDestination
import coredevices.ring.service.button.GestureRoutingPreferences
import coredevices.ring.service.button.RingGesture
import coredevices.util.CommonBuildKonfig
import coredevices.util.emailOrNull
import coredevices.util.flow.mapState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.rebble.libpebblecommon.connection.LibPebble
import io.rebble.libpebblecommon.locker.LockerWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

sealed class DiagnosticsState {
    object Idle : DiagnosticsState()
    object Running : DiagnosticsState()
    data class Error(val message: String) : DiagnosticsState()
    data class Completed(val measurement: RSSIMeasurement) : DiagnosticsState()
}

private const val DIAGNOSTICS_ATTEMPTS = 3
private val DIAGNOSTICS_RETRY_DELAY = 1.seconds
private val DIAGNOSTICS_CONNECTION_TIMEOUT = 10.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val preferences: Preferences,
    private val providers: IntegrationProviderRepository,
    private val pluginStore: PluginStore,
    private val indexDeviceManager: IndexDeviceManager,
    private val mcpServerRepository: McpServerRepository,
    private val ringDelegate: RingDelegate,
    private val cactusModelProvider: CactusModelProvider,
    private val appScope: LibIndexCoroutineScope,
    private val gestureRouting: GestureRoutingPreferences,
    private val indexActionsRepository: IndexActionsRepository,
    private val libPebble: LibPebble,
): ViewModel() {
    val version = CommonBuildKonfig.GIT_HASH
    val username = Firebase.auth.authStateChanged
        .map { it?.emailOrNull }
        .stateIn(viewModelScope, SharingStarted.Lazily, Firebase.auth.currentUser?.email)

    val gestureRoutes = gestureRouting.routes
    val debugDetailsEnabled = preferences.debugDetailsEnabled
    private val _showContactsDialog = MutableStateFlow(false)
    val showContactsDialog = _showContactsDialog.asStateFlow()

    // Eager so the list is loaded before the section is lazily scrolled into view.
    val indexActions = indexActionsRepository.actions.stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    val httpMcpDisabledReason = indexActionsRepository.httpMcpDisabledReason.stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )
    val plugins = indexActionsRepository.plugins.stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    val httpMcpServers = mcpServerRepository.httpServersFlow().stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    private val _showNoteShortcutDialog = MutableStateFlow(false)
    val showNoteShortcutDialog = _showNoteShortcutDialog.asStateFlow()
    val noteShortcut = preferences.noteShortcut
    val autoDismissActionNotifications = preferences.autoDismissActionNotifications
    private val currentRing = indexDeviceManager.rings.map {
        it.firstOrNull { ring -> ring is KnownIndexDevice }
    }
    val ringPaired = preferences.ringPaired
    val currentRingFirmware = currentRing
        .mapNotNull { (it as? InterviewedIndexDevice)?.firmwareVersion }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val currentRingName = currentRing
        .map { it?.name }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val pluginsEnabled: StateFlow<Boolean> = libPebble.config.mapState { it.watchConfig.enablePlugins }

    private val providerRefresh = MutableStateFlow(0)
    val availableNoteProviders = combine(providerRefresh, pluginsEnabled) { _, pluginsOn ->
        providers.availableProviders(IntegrationSupport.Note).visibleDestinations(pluginsOn)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val availableReminderProviders = combine(providerRefresh, pluginsEnabled) { _, pluginsOn ->
        providers.availableProviders(IntegrationSupport.Reminder).visibleDestinations(pluginsOn)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Fetched once per screen: the list only changes when the store does.
    private val indexStorePlugins = flow { emit(pluginStore.indexPlugins().orEmpty()) }
        .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    /** Store plugins not in the locker yet, offered as destinations while plugins are on. */
    val storePlugins: StateFlow<List<StorePlugin>> = pluginsEnabled
        .flatMapLatest { pluginsOn ->
            if (!pluginsOn) {
                flowOf(emptyList())
            } else {
                combine(indexStorePlugins, libPebble.getAllLockerUuids()) { listed, installed ->
                    listed.filter { it.pluginUuid !in installed }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    /** Plugin destinations whose pbw declares a settings page. */
    val configurablePlugins = providerRefresh
        .map { configurablePluginUuids() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val _diagnosticsState = MutableStateFlow<DiagnosticsState>(DiagnosticsState.Idle)
    val diagnosticsState = _diagnosticsState.asStateFlow()

    fun setActionEnabled(name: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { indexActionsRepository.setActionEnabled(name, enabled) }
    }

    fun setHttpServerEnabled(server: HttpMcpServerEntity, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { mcpServerRepository.setHttpServerEnabled(server, enabled) }
    }

    fun saveHttpServer(server: HttpMcpServerEntity) {
        viewModelScope.launch(Dispatchers.IO) { mcpServerRepository.addOrUpdateHttpServer(server) }
    }

    fun deleteHttpServer(server: HttpMcpServerEntity) {
        viewModelScope.launch(Dispatchers.IO) { mcpServerRepository.deleteHttpServer(server) }
    }

    fun setPluginGrantEnabled(pluginUuid: Uuid, grant: PluginGrant, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            indexActionsRepository.setPluginGrantEnabled(pluginUuid, grant, enabled)
        }
    }

    fun togglePendingIntentScanEnabled() {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setUsePendingIntentScan(!preferences.usePendingIntentScan.value)
        }
    }

    val pendingIntentScanEnabled = preferences.usePendingIntentScan

    fun toggleScanByPairedAddress() {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setScanByPairedAddress(!preferences.scanByPairedAddress.value)
        }
    }

    val scanByPairedAddress = preferences.scanByPairedAddress

    fun beginDiagnostics(): Job {
        _diagnosticsState.value = DiagnosticsState.Running
        return viewModelScope.launch {
            val ring = indexDeviceManager.rings.value.filterIsInstance<KnownIndexDevice>().firstOrNull()
                ?: run {
                    Logger.withTag("RingDiagnostics").w { "No paired ring found for diagnostics" }
                    _diagnosticsState.value = DiagnosticsState.Idle
                    return@launch
                }

            var lastError: Exception? = null
            repeat(DIAGNOSTICS_ATTEMPTS) { attempt ->
                try {
                    val result = ring.measureRSSI(DIAGNOSTICS_CONNECTION_TIMEOUT)
                    Logger.withTag("RingDiagnostics").i { "RSSI diagnostic result: $result" }
                    _diagnosticsState.value = DiagnosticsState.Completed(result)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    Logger.withTag("RingDiagnostics").w(e) {
                        "RSSI measurement failed (attempt ${attempt + 1}/$DIAGNOSTICS_ATTEMPTS)"
                    }
                    if (attempt < DIAGNOSTICS_ATTEMPTS - 1) delay(DIAGNOSTICS_RETRY_DELAY)
                }
            }
            _diagnosticsState.value = DiagnosticsState.Error(
                lastError?.message ?: "Couldn't reach your Index 01"
            )
        }.apply {
            invokeOnCompletion {
                if (it is CancellationException) {
                    _diagnosticsState.value = DiagnosticsState.Idle
                }
            }
        }
    }

    fun resetDiagnostics() {
        _diagnosticsState.value = DiagnosticsState.Idle
    }

    fun refreshAvailableProviders() {
        indexActionsRepository.refreshPlugins()
        providerRefresh.value++
    }

    private suspend fun configurablePluginUuids(): Set<Uuid> = indexActionsRepository.plugins.first()
        .filter {
            (libPebble.getLockerApp(it.pluginUuid).first() as? LockerWrapper.NormalApp)?.configurable == true
        }
        .map { it.pluginUuid }
        .toSet()

    fun setGestureRoute(gesture: RingGesture, destination: GestureDestination) {
        gestureRouting.setRoute(gesture, destination)
        if (destination == GestureDestination.LocalAgent || destination == GestureDestination.IndexAgent) {
            // Trigger LM model extraction, should be already integrated into assets so no DL
            appScope.launch { cactusModelProvider.getLMModelPath() }
        }
    }

    fun toggleDebugDetailsEnabled() {
        viewModelScope.launch {
            val newValue = !debugDetailsEnabled.value
            preferences.setDebugDetailsEnabled(newValue)
        }
    }

    fun toggleAutoDismissActionNotifications() {
        preferences.setAutoDismissActionNotifications(!autoDismissActionNotifications.value)
    }

    fun showNoteShortcutDialog() {
        refreshAvailableProviders()
        _showNoteShortcutDialog.value = true
    }

    fun closeNoteShortcutDialog() {
        _showNoteShortcutDialog.value = false
    }

    fun setNoteShortcut(shortcut: NoteShortcutType) {
        preferences.setNoteShortcut(shortcut)
    }

    fun showContactsDialog() {
        _showContactsDialog.value = true
    }

    fun closeContactsDialog() {
        _showContactsDialog.value = false
    }

    fun restartPreemptiveTransfer() {
        ringDelegate.restartPreemptiveTransfer()
    }
}
