package coredevices.ring.ui.viewmodel

import androidx.lifecycle.ViewModel
import coredevices.libindex.di.LibIndexCoroutineScope
import coredevices.ring.external.indexwebhook.IndexWebhookPreferences
import coredevices.ring.service.button.GestureDestination
import coredevices.ring.service.button.GestureRoutingPreferences
import coredevices.ring.service.button.RingGesture
import coredevices.util.Platform
import coredevices.util.isAndroid
import coredevices.util.transcription.CactusModelPathProvider
import kotlinx.coroutines.launch

/** State for the Ring Button switchboard: per-gesture routes and their webhook copies. */
class RingButtonViewModel(
    private val gestureRouting: GestureRoutingPreferences,
    private val webhookPreferences: IndexWebhookPreferences,
    private val cactusModelProvider: CactusModelPathProvider,
    private val appScope: LibIndexCoroutineScope,
    platform: Platform,
) : ViewModel() {
    val gestureRoutes = gestureRouting.routes
    val webhookConfigs = webhookPreferences.configs
    val musicEnabled: Boolean = platform.isAndroid

    fun setGestureRoute(gesture: RingGesture, destination: GestureDestination) {
        gestureRouting.setRoute(gesture, destination)
        if (destination == GestureDestination.LocalAgent || destination == GestureDestination.IndexAgent) {
            // Trigger LM model extraction, should be already integrated into assets so no DL
            appScope.launch { cactusModelProvider.getLMModelPath() }
        }
    }

    fun disableWebhook(gesture: RingGesture) {
        webhookPreferences.setEnabled(gesture, false)
    }
}
