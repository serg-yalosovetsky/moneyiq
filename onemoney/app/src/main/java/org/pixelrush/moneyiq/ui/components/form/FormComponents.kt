package org.syalosovetskyi.onemoney.ui.components.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.syalosovetskyi.onemoney.ui.theme.Spacing

@Composable
internal fun FormSectionHeader(title: String) {
    Text(
        title,
        modifier   = Modifier.padding(start = Spacing.lg, top = Spacing.xl, bottom = Spacing.xs),
        style      = MaterialTheme.typography.labelLarge,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun FormNavRow(
    icon:    ImageVector?,
    label:   String,
    value:   String = "",
    onClick: () -> Unit
) {
    ListItem(
        modifier          = Modifier.clickable(onClick = onClick),
        leadingContent    = if (icon != null) {{
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }} else null,
        headlineContent   = { Text(label) },
        trailingContent   = if (value.isNotBlank()) {{
            Text(
                value,
                color      = MaterialTheme.colorScheme.primary,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
        }} else null
    )
    HorizontalDivider(
        modifier  = Modifier.padding(start = if (icon != null) 56.dp else Spacing.lg),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
internal fun FormValueRow(
    label:   String,
    value:   String,
    onClick: () -> Unit
) {
    ListItem(
        modifier        = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    )
    HorizontalDivider(
        modifier  = Modifier.padding(start = Spacing.lg),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}
