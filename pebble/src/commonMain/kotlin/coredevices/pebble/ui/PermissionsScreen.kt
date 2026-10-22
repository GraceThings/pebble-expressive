package coredevices.pebble.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import coredevices.pebble.custom.ExpressiveListCard
import coredevices.pebble.custom.ExpressiveListDivider
import coredevices.pebble.custom.ExpressiveListFooter
import coredevices.pebble.custom.ExpressiveListItem
import coredevices.pebble.custom.ExpressiveListSectionHeader
import coredevices.pebble.custom.ExpressiveToggleListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import coredevices.util.CoreConfigHolder
import coredevices.util.PermissionRequester
import coredevices.util.PermissionResult
import coredevices.util.description
import coredevices.util.name
import coredevices.util.rememberUiContext
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val logger = Logger.withTag("PermissionsScreen")

@Composable
fun PermissionsScreen(navBarNav: NavBarNav, topBarParams: TopBarParams) {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        LaunchedEffect(Unit) {
            topBarParams.searchAvailable(null)
            topBarParams.actions {
            }
            topBarParams.title("Permissions")
        }
        val permissionRequester: PermissionRequester = koinInject()
        val coreConfigHolder: CoreConfigHolder = koinInject()
        val coreConfig by coreConfigHolder.config.collectAsState()

        val missingPermissions by permissionRequester.missingPermissions.collectAsState(emptySet())

        val scope = rememberCoroutineScope()
        val missingPermissionsList = remember(missingPermissions) { missingPermissions.toList() }
        val uiContext = rememberUiContext()
        if (uiContext == null) {
            logger.e { "uiContext is null" }
            return
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hide_badges_toggle") {
                ExpressiveListSectionHeader("Preferences")
                ExpressiveListCard {
                    ExpressiveToggleListItem(
                        headlineText = "Hide missing permission warnings",
                        supportingText = "Don't show a badge on Settings when permissions are missing",
                        checked = coreConfig.hidePermissionWarningBadges,
                        onCheckedChange = { checked ->
                            coreConfigHolder.update(
                                coreConfig.copy(hidePermissionWarningBadges = checked)
                            )
                        },
                    )
                }
            }

            if (missingPermissionsList.isEmpty()) {
                item(key = "all_granted") {
                    ExpressiveListSectionHeader("Status")
                    ExpressiveListCard {
                        ExpressiveListItem(
                            headlineText = "All permissions granted!",
                            supportingText = "All necessary device permissions are in place.",
                        )
                    }
                }
            } else {
                item(key = "missing_perms_group") {
                    ExpressiveListSectionHeader("Required Permissions")
                    ExpressiveListCard {
                        missingPermissionsList.forEachIndexed { index, permission ->
                            ExpressiveListItem(
                                headlineText = permission.name(),
                                supportingText = permission.description(),
                                onClick = {
                                    scope.launch {
                                        val granted =
                                            permissionRequester.requestPermission(permission, uiContext)
                                        logger.d { "$permission granted = $granted" }
                                        if (granted == PermissionResult.RejectedForever) {
                                            permissionRequester.openPermissionsScreen(uiContext)
                                        }
                                    }
                                },
                            )
                            if (index < missingPermissionsList.size - 1) {
                                ExpressiveListDivider()
                            }
                        }
                    }
                }
            }

            item(key = "footer") {
                ExpressiveListFooter()
            }
        }
    }
}
