package coredevices.pebble.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import coredevices.ui.PebbleElevatedButton
import io.rebble.libpebblecommon.packets.blobdb.TimelineIcon
import io.rebble.libpebblecommon.packets.blobdb.TimelineIcon.Companion.key
import org.jetbrains.compose.ui.tooling.preview.Preview

private val logger = Logger.withTag("Icons")

@Composable
fun IconPickerDialog(
    onIconSelected: (TimelineIcon?) -> Unit,
    onDismissWithoutResult: () -> Unit,
) {
    Dialog(
        onDismissRequest = { onDismissWithoutResult() }
    ) {
        Card(modifier = Modifier.padding(15.dp)) {
            Column {
                var searchQuery by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Icons") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                val filteredIcons = remember(searchQuery) {
                    TimelineIcon.entries.filter { it.key().contains(searchQuery, ignoreCase = true) }
                }

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) { items(filteredIcons, key = { it.code }) { icon ->
                        ListItem(
                            headlineContent = {
                                Text(icon.key(), fontSize = 14.sp)
                            },
                            leadingContent = {
                                IconImage(icon, modifier = Modifier.size(33.dp))
                            },
                            modifier = Modifier.clickable { onIconSelected(icon) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Center,
                ) {
                    TextButton(
                        onClick = {
                            onDismissWithoutResult()
                        },
                        content = {
                            Text("Cancel")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            onIconSelected(null)
                        },
                        content = {
                            Text("None")
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun IconImage(icon: TimelineIcon, modifier: Modifier) {
    AsyncImage(
        model = "https://developer.repebble.com/assets/images/guides/timeline/${icon.key()}.svg",
        contentDescription = icon.key(),
//        error = rememberVectorPainter(Icons.Default.BrokenImage),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    onIconSelected: (TimelineIcon?) -> Unit,
    onDismissWithoutResult: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onDismissWithoutResult() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Choose Icon",
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = { onIconSelected(null) }) {
                    Text("None")
                }
            }

            var searchQuery by remember { mutableStateOf("") }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Icons") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
            )

            val filteredIcons = remember(searchQuery) {
                TimelineIcon.entries.filter { it.key().contains(searchQuery, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(filteredIcons, key = { it.code }) { icon ->
                    ListItem(
                        headlineContent = {
                            Text(icon.key(), fontSize = 14.sp)
                        },
                        leadingContent = {
                            IconImage(icon, modifier = Modifier.size(33.dp))
                        },
                        modifier = Modifier.clickable { onIconSelected(icon) },
                    )
                }
            }
        }
    }
}

@Composable
fun SelectIconOrNone(
    currentIcon: TimelineIcon?,
    onChangeIcon: (TimelineIcon?) -> Unit,
    useSheet: Boolean = true,
) {
    var showIconChooser by remember { mutableStateOf(false) }
    if (showIconChooser) {
        if (useSheet) {
            IconPickerSheet(
                onIconSelected = { icon ->
                    onChangeIcon(icon)
                    showIconChooser = false
                },
                onDismissWithoutResult = {
                    showIconChooser = false
                },
            )
        } else {
            IconPickerDialog(
                onIconSelected = { icon ->
                    onChangeIcon(icon)
                    showIconChooser = false
                },
                onDismissWithoutResult = {
                    showIconChooser = false
                },
            )
        }
    }
    ListItem(
        headlineContent = {
            Text(
                text = "Icon",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        },
        supportingContent = {
            Text(
                text = currentIcon?.name ?: "Default",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (currentIcon != null) {
                    IconImage(currentIcon, modifier = Modifier.size(24.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = "Select icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable {
            showIconChooser = true
        },
    )
}

@Preview
@Composable
fun IconPickerPreview() {
    IconPickerDialog(onIconSelected = {}, onDismissWithoutResult = {})
}