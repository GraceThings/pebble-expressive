package coredevices.pebble.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive List Section Header
 * Matches the section header style in settings (e.g., "Unused app settings", "Advanced").
 */
@Composable
fun ExpressiveListSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * M3 Expressive Grouped List Card
 * Encloses a group of list items in a unified surface with large 24.dp rounded corners.
 */
@Composable
fun ExpressiveListCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        content = content,
    )
}

/**
 * M3 Expressive List Divider
 * Fine hairline divider between consecutive items within an [ExpressiveListCard].
 */
@Composable
fun ExpressiveListDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}

/**
 * M3 Expressive Switch with Checkmark & Cross Thumb Icons
 * Shows a white checkmark icon when checked and a subtle cross icon when unchecked.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        },
        modifier = modifier,
    )
}

/**
 * M3 Expressive List Item
 * Formatted with bold headline, clean supporting text, and optional leading/trailing content.
 */
@Composable
fun ExpressiveListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val itemModifier = if (onClick != null && enabled) {
        modifier.clickable(onClick = onClick)
    } else modifier

    ListItem(
        modifier = itemModifier,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
        leadingContent = leadingContent,
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        },
        supportingContent = if (supportingText != null) {
            {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
        } else null,
        trailingContent = trailingContent,
    )
}

/**
 * M3 Expressive Toggle List Item
 * Complete toggle row with headline, optional supporting text, and trailing [ExpressiveSwitch].
 */
@Composable
fun ExpressiveToggleListItem(
    headlineText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    ExpressiveListItem(
        headlineText = headlineText,
        supportingText = supportingText,
        leadingContent = leadingContent,
        trailingContent = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
            )
        },
        onClick = if (enabled) { { onCheckedChange(!checked) } } else null,
        enabled = enabled,
        modifier = modifier,
    )
}

/**
 * M3 Expressive List Version Footer
 * Subtle footer text at the end of lists (e.g., "version 1.15.0.1").
 */
@Composable
fun ExpressiveListFooter(
    text: String = "version 1.15.0.1",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * M3 Expressive Selection Bottom Sheet
 * Replaces dropdown menus or select buttons with a modern modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExpressiveSelectionSheet(
    title: String,
    items: List<T>,
    selectedItem: T,
    itemText: @Composable (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items) { item ->
                    val isSelected = item == selectedItem
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemSelected(item) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = itemText(item),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onItemSelected(item) },
                            )
                        },
                    )
                }
            }
        }
    }
}
