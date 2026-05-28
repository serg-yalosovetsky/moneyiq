package org.pixelrush.moneyiq.ui.data

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.workers.DriveBackupEntry
import java.text.SimpleDateFormat
import java.util.*
// ── MonoFlow Sync Card ────────────────────────────────────────────────────────

@Composable
internal fun MonoFlowSyncCard(
    state: DataUiState,
    context: android.content.Context,
    viewModel: DataViewModel
) {
    val scope = rememberCoroutineScope()
    val fmt   = remember { java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale("uk")) }

    // Локальні поля введення
    var editUrl   by remember(state.monoflowUrl)   { mutableStateOf(state.monoflowUrl)   }
    var editToken by remember(state.monoflowToken) { mutableStateOf(state.monoflowToken) }
    var showToken by remember { mutableStateOf(false) }

    val isConfigured = state.monoflowUrl.isNotBlank() && state.monoflowToken.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfigured)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {

            // Заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    tint = if (isConfigured) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isConfigured) "MonoFlow підключено" else "Підключити MonoFlow",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isConfigured) {
                    IconButton(
                        onClick = { viewModel.clearMonoFlowConfig(context) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Від'єднати",
                            modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // URL
            OutlinedTextField(
                value = editUrl,
                onValueChange = { editUrl = it },
                label = { Text("URL сервісу") },
                placeholder = { Text("https://monoflow.ibotz.fun/api/sync") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSyncing
            )

            Spacer(Modifier.height(8.dp))

            // Токен
            OutlinedTextField(
                value = editToken,
                onValueChange = { editToken = it },
                label = { Text("Токен") },
                placeholder = { Text("miq_xxxxxxxxxxxxxxxx") },
                singleLine = true,
                visualTransformation = if (showToken)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSyncing
            )

            Spacer(Modifier.height(12.dp))

            // Кнопка Зберегти/Оновити
            Button(
                onClick = {
                    if (editUrl.isNotBlank() && editToken.isNotBlank()) {
                        viewModel.saveMonoFlowConfig(context, editUrl, editToken)
                    }
                },
                enabled = editUrl.isNotBlank() && editToken.isNotBlank() && !state.isSyncing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Синхронізація…")
                } else {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isConfigured) "Синхронізувати зараз" else "Підключити та синхронізувати")
                }
            }

            // Статус і автосинк — тільки якщо налаштовано
            if (isConfigured) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                // Автосинк
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Автосинк кожні 2 год",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Тільки при наявності інтернету",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.monoflowAutoSync,
                        onCheckedChange = { viewModel.setMonoFlowAutoSync(context, it) }
                    )
                }

                // Час останнього синку
                if (state.monoflowLastSyncMs > 0L) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Останній синк: ${fmt.format(java.util.Date(state.monoflowLastSyncMs))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Приватні composable ───────────────────────────────────────────────────────

@Composable
internal fun DataSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
internal fun DataActionItem(
    icon: ImageVector,
    title: String,
    sub: String? = null,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(enabled = !loading, onClick = onClick),
        leadingContent = {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            else Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
        },
        headlineContent  = { Text(title) },
        supportingContent = sub?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } }
    )
}

@Composable
internal fun DriveBackupItem(
    entry: DriveBackupEntry,
    onRestore: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("uk")) }
    val size = when {
        entry.sizeBytes < 1024        -> "${entry.sizeBytes} Б"
        entry.sizeBytes < 1024 * 1024 -> "${entry.sizeBytes / 1024} КБ"
        else -> "${"%.1f".format(entry.sizeBytes / (1024.0 * 1024))} МБ"
    }
    ListItem(
        leadingContent = {
            Icon(Icons.Default.CloudQueue, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
        },
        headlineContent  = { Text(entry.name, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Text("${fmt.format(Date(entry.modifiedMs))}  ·  $size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            TextButton(onClick = onRestore) { Text("Відновити") }
        }
    )
}

@Composable
internal fun LocalBackupItem(backup: BackupEntry) {
    val fmt    = remember { SimpleDateFormat("d MMMM yyyy 'р.' H:mm", Locale("uk")) }
    val txLbl  = pluralUk(backup.txCount,      "операція",  "операції",  "операцій")
    val accLbl = pluralUk(backup.accountCount,  "рахунок",   "рахунки",   "рахунків")
    val catLbl = pluralUk(backup.categoryCount, "категорія", "категорії", "категорій")

    ListItem(
        leadingContent = {
            Icon(Icons.Default.History, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        },
        headlineContent  = { Text("Локальний бекап", fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(fmt.format(Date(backup.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                Text("${backup.txCount} $txLbl · ${backup.accountCount} $accLbl · ${backup.categoryCount} $catLbl",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        }
    )
}

internal fun pluralUk(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod100 in 11..14 -> many
        mod10 == 1       -> one
        mod10 in 2..4    -> few
        else             -> many
    }
}

@Composable
internal fun ResetDataDialog(
    onDeleteAll: () -> Unit,
    onDeleteTransactions: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Delete, null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text("Скинути дані",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Щоб видалити всі ваші дані (рахунки, категорії, операції та бюджети), " +
                    "виберіть Видалити всі дані.\n\n" +
                    "Щоб видалити лише операції, виберіть Видалити всі операції.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDeleteAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor   = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text("Видалити всі дані", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDeleteTransactions) {
                    Text("Видалити всі операції")
                }
                TextButton(onClick = onDismiss) { Text("Відмінити") }
            }
        }
    }
}
