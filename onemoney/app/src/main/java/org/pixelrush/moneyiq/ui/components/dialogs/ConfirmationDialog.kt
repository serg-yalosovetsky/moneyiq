package org.syalosovetskyi.onemoney.ui.components.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.syalosovetskyi.onemoney.R

@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    icon: ImageVector? = null,
    confirmText: String? = null,
    dismissText: String? = null,
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
            ) { Text(confirmText ?: stringResource(R.string.common_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText ?: stringResource(R.string.common_cancel)) } }
    )
}
