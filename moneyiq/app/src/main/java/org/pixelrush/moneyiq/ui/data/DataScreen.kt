package org.pixelrush.moneyiq.ui.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.workers.DriveBackupEntry
import java.text.SimpleDateFormat
import java.util.*


// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataViewModel = hiltViewModel()
) {
    BackHandler(onBack = onNavigateBack)

    val context  = LocalContext.current
    val state    by viewModel.state.collectAsState()
    val scope    = rememberCoroutineScope()

    // Діалоги
    var showResetDialog   by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<Uri?>(null) }
    var showRestoreFromDrive by remember { mutableStateOf<DriveBackupEntry?>(null) }
    // JSON рядок для запису після відкриття CreateDocument
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingCsvIntent  by remember { mutableStateOf<Intent?>(null) }

    LaunchedEffect(Unit) { viewModel.loadState(context) }

    // Повідомлення
    state.message?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    // ── Лаунчери ──────────────────────────────────────────────────────────────

    // Вибір папки Google Drive
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.configureDriveFolder(context, it) }
    }

    // Збереження JSON-бекапу
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { dst ->
            pendingExportJson?.let { json ->
                viewModel.writeExportToUri(context, dst, json)
                pendingExportJson = null
            }
        }
    }

    // Відкриття JSON-файлу для імпорту
    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showImportConfirm = it }
    }

    // Реакція на підготовлений CSV
    LaunchedEffect(pendingCsvIntent) {
        pendingCsvIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, "Поділитися CSV"))
            pendingCsvIntent = null
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дані") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Скинути дані ──────────────────────────────────────────────────
            item {
                ListItem(
                    modifier = Modifier.clickable { showResetDialog = true },
                    leadingContent = {
                        Icon(Icons.Default.RestartAlt, null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    headlineContent = {
                        Text("Скинути дані", color = MaterialTheme.colorScheme.error)
                    }
                )
                HorizontalDivider()
            }

            // ── Імпорт / Експорт ──────────────────────────────────────────────
            item { DataSectionHeader("Імпорт / Експорт") }

            item {
                DataActionItem(
                    icon  = Icons.Default.FileUpload,
                    title = "Експорт даних (JSON)",
                    sub   = "Зберегти всі рахунки, категорії та операції",
                    loading = state.isExporting,
                    onClick = {
                        if (!state.isExporting) {
                            scope.launch {
                                val json = viewModel.buildExportJson()
                                pendingExportJson = json
                                val fmt  = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                exportJsonLauncher.launch("moneyiq_${fmt.format(Date())}.json")
                            }
                        }
                    }
                )
            }
            item {
                DataActionItem(
                    icon  = Icons.Default.FileDownload,
                    title = "Імпорт даних (JSON)",
                    sub   = "Відновити з файлу резервної копії",
                    loading = state.isImporting,
                    onClick = {
                        if (!state.isImporting) {
                            importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    }
                )
            }
            item {
                DataActionItem(
                    icon    = Icons.Default.TableChart,
                    title   = "Експорт у CSV",
                    sub     = "Операції у форматі Excel/Google Sheets",
                    onClick = {
                        scope.launch {
                            val intent = viewModel.buildCsvShareIntentSuspend(context)
                            if (intent != null) pendingCsvIntent = intent
                            else Toast.makeText(context, "Немає операцій для експорту", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider()
            }

            // ── Google Drive автобекап ─────────────────────────────────────────
            item { DataSectionHeader("Google Drive — автобекап") }

            if (state.driveFolderUri.isBlank()) {
                // Папка не вибрана
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Автоматично зберігайте бекапи у Google Drive",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Щодня о 2:00 нова резервна копія буде\nдодаватись до вибраної папки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { folderLauncher.launch(null) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Вибрати папку в Google Drive")
                            }
                        }
                    }
                }
            } else {
                // Папка вибрана — показуємо статус
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.driveFolderName.ifBlank { "Google Drive" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearDriveFolder(context) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Від'єднати",
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            if (state.driveLastBackupMs > 0L) {
                                Spacer(Modifier.height(4.dp))
                                val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("uk")) }
                                Text(
                                    "Останній бекап: ${fmt.format(Date(state.driveLastBackupMs))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Autorenew, null,
                                tint = MaterialTheme.colorScheme.onSurface)
                        },
                        headlineContent = { Text("Щоденний автобекап") },
                        supportingContent = { Text("Щодня о 2:00, при наявності інтернету") },
                        trailingContent = {
                            Switch(
                                checked = state.driveBackupEnabled,
                                onCheckedChange = { viewModel.setDriveAutoBackup(context, it) }
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        modifier = Modifier.clickable(enabled = !state.isBacking) {
                            viewModel.backupToDriveNow(context)
                        },
                        leadingContent = {
                            if (state.isBacking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Backup, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        headlineContent = {
                            Text("Зробити бекап зараз",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium)
                        }
                    )
                }
                item {
                    ListItem(
                        modifier = Modifier.clickable { folderLauncher.launch(null) },
                        leadingContent = {
                            Icon(Icons.Default.FolderOpen, null,
                                tint = MaterialTheme.colorScheme.onSurface)
                        },
                        headlineContent = { Text("Змінити папку") }
                    )
                    HorizontalDivider()
                }

                // Список Drive-бекапів
                if (state.driveBackups.isNotEmpty()) {
                    item {
                        Text(
                            "Збережені бекапи в Drive (${state.driveBackups.size})",
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.driveBackups) { entry ->
                        DriveBackupItem(
                            entry = entry,
                            onRestore = { showRestoreFromDrive = entry }
                        )
                    }
                    item { HorizontalDivider() }
                }
            }

            // ── MonoFlow синхронізація ─────────────────────────────────────────
            item { DataSectionHeader("MonoFlow — авто-синхронізація") }
            item {
                MonoFlowSyncCard(
                    state       = state,
                    context     = context,
                    viewModel   = viewModel
                )
                HorizontalDivider()
            }

            // ── Локальний бекап ────────────────────────────────────────────────
            item { DataSectionHeader("Локальний бекап") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Warning, null,
                        tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Локальні копії зберігаються лише на пристрої. " +
                        "Видалення або перевстановлення застосунку призведе до їхньої втрати.",
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { viewModel.createLocalBackup(context) },
                    leadingContent = {
                        Icon(Icons.Default.AddCircleOutline, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    headlineContent = {
                        Text("Створити резервну копію",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium)
                    }
                )
            }
            items(state.localBackups) { backup ->
                LocalBackupItem(backup = backup)
            }
        }
    }

    // ── Діалоги ───────────────────────────────────────────────────────────────

    if (showResetDialog) {
        ResetDataDialog(
            onDeleteAll = {
                showResetDialog = false
                viewModel.deleteAllData()
            },
            onDeleteTransactions = {
                showResetDialog = false
                viewModel.deleteAllTransactions(context)
            },
            onDismiss = { showResetDialog = false }
        )
    }

    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            icon = { Icon(Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.error) },
            title = { Text("Замінити всі дані?") },
            text = {
                Text("Поточні рахунки, категорії та операції будуть видалені " +
                     "і замінені даними з файлу. Дію неможливо скасувати.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importFromUri(context, uri)
                        showImportConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Замінити") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text("Скасувати") }
            }
        )
    }

    showRestoreFromDrive?.let { entry ->
        AlertDialog(
            onDismissRequest = { showRestoreFromDrive = null },
            icon = { Icon(Icons.Default.CloudDownload, null,
                tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Відновити з Drive?") },
            text = {
                val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("uk")) }
                Text("Відновити резервну копію:\n${entry.name}\n" +
                     "(${fmt.format(Date(entry.modifiedMs))})\n\n" +
                     "Поточні дані будуть замінені.")
            },
            confirmButton = {
                val uri = state.driveFolderUri
                TextButton(onClick = {
                    if (uri.isNotBlank()) {
                        viewModel.restoreFromDrive(context, Uri.parse(uri), entry)
                    }
                    showRestoreFromDrive = null
                }) { Text("Відновити") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreFromDrive = null }) { Text("Скасувати") }
            }
        )
    }

    // Прогрес-індикатор під час імпорту
    if (state.isImporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Імпорт даних…")
                }
            }
        }
    }
}
