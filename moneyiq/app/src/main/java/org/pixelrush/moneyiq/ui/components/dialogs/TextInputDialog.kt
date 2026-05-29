package org.pixelrush.moneyiq.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
internal fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    confirmText: String = "OK",
    dismissText: String = "Скасувати",
    allowDismiss: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = { if (allowDismiss || value.isNotBlank()) onDismiss() },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value         = value,
                onValueChange = { value = it },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text(label) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) { Text(confirmText) }
        },
        dismissButton = {
            if (allowDismiss || value.isNotBlank()) {
                TextButton(onClick = onDismiss) { Text(dismissText) }
            }
        }
    )
}
