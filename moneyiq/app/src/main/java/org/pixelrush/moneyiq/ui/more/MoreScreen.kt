package org.pixelrush.moneyiq.ui.more

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.util.CsvExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val txRepo: TransactionRepository
) : ViewModel() {

    fun exportCsv(context: Context, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val allTx = txRepo.getRecentTransactions(10_000).first()
            val intent = withContext(Dispatchers.IO) { CsvExporter.export(context, allTx) }
            onReady(Intent.createChooser(intent, "Экспортировать транзакции"))
        }
    }

    fun backupDb(context: Context, destUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("moneyiq.db")
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    dbFile.inputStream().use { it.copyTo(out) }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Резервная копия сохранена ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка резервного копирования: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun restoreDb(context: Context, srcUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("moneyiq.db")
                context.contentResolver.openInputStream(srcUri)?.use { input ->
                    dbFile.outputStream().use { input.copyTo(it) }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Данные восстановлены. Перезапустите приложение.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка восстановления: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun MoreScreen(
    padding: PaddingValues = PaddingValues(),
    viewModel: MoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }

    if (showCategories) {
        // embeddedMode=true — без вложенного Scaffold; используем отступ для нижней навигации
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showCategories = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    "Категории",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            HorizontalDivider()
            org.pixelrush.moneyiq.ui.categories.CategoriesScreen(
                onNavigateBack = { showCategories = false },
                embeddedMode = true
            )
        }
        return
    }

    // Лаунчер для экспорта CSV
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    // Лаунчер для создания файла бэкапа
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.backupDb(context, it) }
    }

    // Лаунчер для выбора файла восстановления
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreDb(context, it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Управление ──────────────────────────────────────────────────────
        item { GroupHeader("Управление") }

        item {
            SettingsItem(
                icon = Icons.Default.Category,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Категории",
                subtitle = "Управление категориями расходов и доходов",
                onClick = { showCategories = true }
            )
        }

        // ── Данные ──────────────────────────────────────────────────────────
        item { GroupHeader("Данные") }

        item {
            SettingsItem(
                icon = Icons.Default.FileDownload,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Экспорт в CSV",
                subtitle = "Сохранить транзакции для Excel",
                onClick = {
                    viewModel.exportCsv(context) { intent ->
                        shareLauncher.launch(intent)
                    }
                }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Backup,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Создать резервную копию",
                subtitle = "Сохранить базу данных на устройство",
                onClick = {
                    val name = "moneyiq_backup_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.db"
                    backupLauncher.launch(name)
                }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Restore,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "Восстановить из копии",
                subtitle = "Загрузить ранее сохранённую базу данных",
                onClick = { showRestoreConfirm = true }
            )
        }

        // ── Приложение ──────────────────────────────────────────────────────
        item { GroupHeader("Приложение") }

        item {
            SettingsItem(
                icon = Icons.Default.Palette,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Тема оформления",
                subtitle = "Светлая / Тёмная (следует за системной)",
                onClick = { /* TODO */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Language,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Язык",
                subtitle = "Русский",
                onClick = { /* TODO */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.AttachMoney,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "Валюта по умолчанию",
                subtitle = "RUB — Российский рубль",
                onClick = { /* TODO */ }
            )
        }

        // ── О приложении ────────────────────────────────────────────────────
        item { GroupHeader("О приложении") }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = "Версия",
                subtitle = "MoneyIQ 1.0.0",
                onClick = {}
            )
        }
    }

    // Диалог подтверждения восстановления
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Восстановить данные?") },
            text = { Text("Все текущие данные будут заменены содержимым резервной копии. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        restoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Восстановить") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Отмена") }
            }
        )
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
        },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    )
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
