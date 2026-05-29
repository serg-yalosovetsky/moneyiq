package org.pixelrush.moneyiq.ui.components.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    icon: ImageVector? = null,
    confirmText: String = "Видалити",
    dismissText: String = "Скасувати",
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = icon?.let { { Icon(it, null, tint = MaterialTheme.colorScheme.error) } },
        title = { Text(title) },
        text  = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors  = if (destructive)
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.textButtonColors()
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } }
    )
}
