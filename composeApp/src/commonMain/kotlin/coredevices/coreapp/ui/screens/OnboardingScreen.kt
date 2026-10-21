package coredevices.coreapp.ui.screens

import CoreNav
import NoOpCoreNav
import PlatformUiContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import coreapp.composeapp.generated.resources.Res
import coreapp.composeapp.generated.resources.pebble_logo
import coredevices.pebble.ui.PebbleRoutes
import coredevices.pebble.ui.PreviewWrapper
import coredevices.ui.SecondaryProfileWarningDialog
import coredevices.ui.SignInButtons
import coredevices.util.CoreConfig
import coredevices.util.CoreConfigHolder
import coredevices.util.DoneInitialOnboarding
import coredevices.util.Permission
import coredevices.util.PermissionRequester
import coredevices.util.SecondaryProfileWarning
import coredevices.util.name
import coredevices.util.rememberUiContext
import coredevices.util.requestIsFullScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import theme.coreGrey
import theme.coreOrange
import theme.onboardingScheme

private val ONBOARDING_BUTTON_WIDTH = 260.dp


enum class OnboardingStage {
    Welcome,
    DeviceSelection,
    Permissions,
    SignIn,
    Done,
}

enum class DeviceChoice {
    Watch,
    Index01,
    Both,
}

class OnboardingViewModel(private val config: CoreConfigHolder) : ViewModel() {
    val stage = mutableStateOf(OnboardingStage.Welcome)
    val deviceChoice = mutableStateOf<DeviceChoice?>(null)
    val requestedPermissions = mutableStateOf(emptySet<Permission>())
    val coreConfig = config.config
    fun setIndexEnabled(enabled: Boolean) {
        config.update(config.config.value.copy(enableIndex = enabled))
    }
}

private val logger = Logger.withTag("OnboardingScreen")

@Preview
@Composable
fun OnboardingScreenPreview() {
    PreviewWrapper(extraModule = module {
        single { OnboardingViewModel(CoreConfigHolder(
            CoreConfig(),
            settings = Settings(),
            Json.Default
        )) }
    }) {
        OnboardingScreen(NoOpCoreNav)
    }
}


@Composable
fun OnboardingScreen(
    coreNav: CoreNav,
) {
    val viewModel = koinViewModel<OnboardingViewModel>()
    val permissionRequester: PermissionRequester = koinInject()
    val scope = rememberCoroutineScope()
    val settings: Settings = koinInject()
    val doneInitialOnboarding: DoneInitialOnboarding = koinInject()
    val secondaryProfileWarning: SecondaryProfileWarning = koinInject()
    var showSecondaryProfileWarning by remember { mutableStateOf(false) }

    fun chooseIndexDevice(choice: DeviceChoice) {
        viewModel.setIndexEnabled(true)
        viewModel.deviceChoice.value = choice
        if (secondaryProfileWarning.pending) {
            showSecondaryProfileWarning = true
        } else {
            viewModel.stage.value = OnboardingStage.Permissions
        }
    }
    if (showSecondaryProfileWarning) {
        SecondaryProfileWarningDialog {
            showSecondaryProfileWarning = false
            viewModel.stage.value = OnboardingStage.Permissions
        }
    }

    fun exitOnboarding() {
        logger.v { "exitOnboarding" }
        settings[SHOWN_ONBOARDING] = true
        doneInitialOnboarding.onDoneInitialOnboarding()
        coreNav.navigateTo(PebbleRoutes.WatchHomeRoute)
    }

    suspend fun requestPermission(permission: Permission, uiContext: PlatformUiContext) {
        permissionRequester.requestPermission(permission, uiContext)
        viewModel.requestedPermissions.value += permission
    }

    MaterialTheme(colorScheme = onboardingScheme) {
    Scaffold { windowInsets ->
        Box(modifier = Modifier.padding(windowInsets).fillMaxSize()) {
            when (viewModel.stage.value) {
                OnboardingStage.Welcome -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.pebble_logo),
                            contentDescription = "Pebble",
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.height(56.dp),
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        M3EOnboardingButton(
                            text = "Get Started",
                            onClick = {
                                viewModel.stage.value = OnboardingStage.DeviceSelection
                            },
                        )
                    }
                }

                OnboardingStage.DeviceSelection -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "I have a:",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.width(ONBOARDING_BUTTON_WIDTH),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        ) {
                            DeviceChoiceCard(
                                label = "Watch",
                                icon = Icons.Default.Watch,
                                onClick = {
                                    viewModel.deviceChoice.value = DeviceChoice.Watch
                                    viewModel.stage.value = OnboardingStage.Permissions
                                },
                                modifier = Modifier.weight(1f),
                            )
                            DeviceChoiceCard(
                                label = "Index 01",
                                icon = Icons.Default.RadioButtonUnchecked,
                                onClick = { chooseIndexDevice(DeviceChoice.Index01) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        DeviceChoiceCard(
                            label = "Both",
                            icon = Icons.Default.Devices,
                            onClick = { chooseIndexDevice(DeviceChoice.Both) },
                            modifier = Modifier.width(ONBOARDING_BUTTON_WIDTH),
                        )
                    }
                }

                OnboardingStage.Permissions -> {
                    val uiContext = rememberUiContext()
                    if (uiContext != null) {
                        val missingPermissions by permissionRequester.missingPermissions.collectAsState()
                        val permissionToRequest = missingPermissions.firstOrNull {
                            it !in viewModel.requestedPermissions.value
                        }
                        logger.v { "permissionToRequest = $permissionToRequest  /  missingPermissions = $missingPermissions " }
                        if (permissionToRequest == null) {
                            viewModel.stage.value = OnboardingStage.SignIn
                        } else {
                            val warnBeforeFullScreenRequest = permissionToRequest.requestIsFullScreen()
                            LaunchedEffect(permissionToRequest) {
                                if (!warnBeforeFullScreenRequest) {
                                    requestPermission(permissionToRequest, uiContext)
                                }
                            }
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = if (warnBeforeFullScreenRequest) {
                                    Arrangement.Center
                                } else {
                                    Arrangement.Top
                                },
                            ) {
                                if (!warnBeforeFullScreenRequest) {
                                    // Space from top of screen
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                                Text(
                                    text = permissionToRequest.name(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = permissionToRequest.descriptionOnboarding(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                                if (warnBeforeFullScreenRequest) {
                                    Spacer(modifier = Modifier.height(32.dp))
                                    M3EOnboardingButton(
                                        text = "OK",
                                        onClick = {
                                            scope.launch {
                                                requestPermission(
                                                    permissionToRequest,
                                                    uiContext
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                OnboardingStage.SignIn -> {
                    val coreConfig by viewModel.coreConfig.collectAsState()
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sign in to backup your Pebble account to backup apps, settings, etc",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        SignInButtons(
                            onDismiss = { viewModel.stage.value = OnboardingStage.Done },
                            primaryColor = true,
                            skipAccountSwitchConfirmation = true,
                            buttonWidth = ONBOARDING_BUTTON_WIDTH,
                        )
                        if (!coreConfig.enableIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                            M3EOnboardingButton(
                                text = "Skip",
                                onClick = { viewModel.stage.value = OnboardingStage.Done },
                                isSecondary = true,
                            )
                        }
                    }
                }

                OnboardingStage.Done -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Ready to Go!",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        M3EOnboardingButton(
                            text = "Connect a Pebble!",
                            onClick = ::exitOnboarding,
                        )
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EOnboardingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
) {
    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
            ),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
            modifier = modifier
                .width(ONBOARDING_BUTTON_WIDTH)
                .height(48.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = coreGrey,
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp,
            ),
            modifier = modifier
                .width(ONBOARDING_BUTTON_WIDTH)
                .height(48.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DeviceChoiceCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = coreGrey,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = coreOrange,
                modifier = Modifier.size(52.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = coreGrey,
                textAlign = TextAlign.Center,
            )
        }
    }
}

val HighlightStyle = SpanStyle(
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

expect fun Permission.descriptionOnboarding(): AnnotatedString

const val SHOWN_ONBOARDING = "shown_onboarding"