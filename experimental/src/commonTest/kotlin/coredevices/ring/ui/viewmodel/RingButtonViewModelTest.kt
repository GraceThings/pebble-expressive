package coredevices.ring.ui.viewmodel

import com.russhwolf.settings.MapSettings
import coredevices.libindex.di.LibIndexCoroutineScope
import coredevices.ring.database.PreferencesImpl
import coredevices.ring.external.indexwebhook.IndexWebhookConfig
import coredevices.ring.external.indexwebhook.IndexWebhookPreferences
import coredevices.ring.service.button.GestureDestination
import coredevices.ring.service.button.GestureRoutingPreferences
import coredevices.ring.service.button.RingGesture
import coredevices.util.Platform
import coredevices.util.transcription.CactusModelPathProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RingButtonViewModelTest {

    @Test
    fun setGestureRouteWritesTheRoute() = runTest {
        val fixture = Fixture(this)

        fixture.viewModel.setGestureRoute(RingGesture.Hold, GestureDestination.WebSearch)

        assertEquals(GestureDestination.WebSearch, fixture.routing.destinationFor(RingGesture.Hold))
        assertEquals(GestureDestination.WebSearch, fixture.viewModel.gestureRoutes.value[RingGesture.Hold])
    }

    @Test
    fun agentDestinationsTriggerLmModelExtractionOnTheAppScope() = runTest {
        for (destination in listOf(GestureDestination.LocalAgent, GestureDestination.IndexAgent)) {
            val fixture = Fixture(this)

            fixture.viewModel.setGestureRoute(RingGesture.Hold, destination)
            assertEquals(0, fixture.cactus.lmModelPathCalls, "$destination resolved the model synchronously")
            advanceUntilIdle()

            assertEquals(1, fixture.cactus.lmModelPathCalls, "$destination did not extract the LM model")
        }
    }

    @Test
    fun otherDestinationsDoNotTouchTheLmModel() = runTest {
        val fixture = Fixture(this)

        fixture.viewModel.setGestureRoute(RingGesture.Hold, GestureDestination.RemoteAgent)
        fixture.viewModel.setGestureRoute(RingGesture.Hold, GestureDestination.WebSearch)
        fixture.viewModel.setGestureRoute(RingGesture.Hold, GestureDestination.WebhookOnly)
        fixture.viewModel.setGestureRoute(RingGesture.Hold, GestureDestination.Nothing)
        fixture.viewModel.setGestureRoute(RingGesture.Click, GestureDestination.PlayPause)
        advanceUntilIdle()

        assertEquals(0, fixture.cactus.lmModelPathCalls)
    }

    @Test
    fun disableWebhookKeepsTheConfigButStopsSending() = runTest {
        val fixture = Fixture(this)
        fixture.webhooks.setConfig(
            RingGesture.Hold,
            IndexWebhookConfig(url = "https://example.com/hook", saved = true),
        )
        assertTrue(fixture.webhooks.configFor(RingGesture.Hold).isActive)

        fixture.viewModel.disableWebhook(RingGesture.Hold)

        val config = fixture.viewModel.webhookConfigs.value.getValue(RingGesture.Hold)
        assertFalse(config.isActive)
        assertEquals("https://example.com/hook", config.url)
    }

    @Test
    fun musicEnabledFollowsThePlatform() = runTest {
        assertTrue(Fixture(this, platformName = "Android 15").viewModel.musicEnabled)
        assertFalse(Fixture(this, platformName = "iOS 18").viewModel.musicEnabled)
    }

    private class Fixture(scope: TestScope, platformName: String = "Android 15") {
        val routing = GestureRoutingPreferences(MapSettings(), PreferencesImpl(MapSettings()))
        val webhooks = IndexWebhookPreferences(MapSettings())
        val cactus = FakeCactusModelPathProvider()
        val viewModel = RingButtonViewModel(
            routing,
            webhooks,
            cactus,
            LibIndexCoroutineScope(StandardTestDispatcher(scope.testScheduler)),
            FakePlatform(platformName),
        )
    }

    private class FakePlatform(override val name: String) : Platform {
        override val deviceModelName = "test"
        override suspend fun openUrl(url: String) = Unit
        override suspend fun runWithBgTask(name: String, task: suspend () -> Unit) = task()
    }

    private class FakeCactusModelPathProvider : CactusModelPathProvider {
        var lmModelPathCalls = 0

        override suspend fun getSTTModelPath(modelName: String, version: String): String = error("unused")

        override suspend fun getLMModelPath(): String {
            lmModelPathCalls++
            return "lm"
        }

        override fun isModelDownloaded(modelName: String): Boolean = false
        override fun getDownloadedModels(): List<String> = emptyList()
        override fun getIncompatibleModels(): List<String> = emptyList()
        override fun deleteModel(modelName: String) = Unit
        override fun getModelSizeBytes(modelName: String): Long = 0L
        override fun initTelemetry() = Unit
    }
}
