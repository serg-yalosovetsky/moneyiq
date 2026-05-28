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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import org.pixelrush.moneyiq.data.repository.SettingsRepository
import org.pixelrush.moneyiq.util.BackupData
import org.pixelrush.moneyiq.util.BackupSerializer
import org.pixelrush.moneyiq.util.CsvExporter
import org.pixelrush.moneyiq.util.suggestCategoryStyle
import org.pixelrush.moneyiq.workers.DriveBackupEntry
import org.pixelrush.moneyiq.workers.DriveBackupWorker
import org.pixelrush.moneyiq.workers.MonoFlowSyncWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── UiState ───────────────────────────────────────────────────────────────────

data class BackupEntry(
    val timestamp: Long,
    val txCount: Int,
    val accountCount: Int,
    val categoryCount: Int
)

data class DataUiState(
    val localBackups: List<BackupEntry>       = emptyList(),
    val driveBackups: List<DriveBackupEntry>  = emptyList(),
    val driveFolderUri: String                = "",
    val driveFolderName: String               = "",
    val driveBackupEnabled: Boolean           = false,
    val driveLastBackupMs: Long               = 0L,
    val isExporting: Boolean                  = false,
    val isImporting: Boolean                  = false,
    val isBacking: Boolean                    = false,
    val message: String?                      = null,
    // MonoFlow sync
    val monoflowUrl: String                   = "",
    val monoflowToken: String                 = "",
    val monoflowAutoSync: Boolean             = false,
    val monoflowLastSyncMs: Long              = 0L,
    val isSyncing: Boolean                    = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class DataViewModel @Inject constructor(
    private val txDao:       TransactionDao,
    private val accountDao:  AccountDao,
    private val categoryDao: CategoryDao,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state

    // ── Завантаження ──────────────────────────────────────────────────────────

    fun loadState(context: Context) {
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val localBackups = parseLocalBackups(context)
            val driveUri     = settings.driveBackupFolderUri
            val driveName    = if (driveUri.isNotBlank()) {
                runCatching {
                    DriveBackupWorker.getFolderDisplayName(context, Uri.parse(driveUri))
                }.getOrNull() ?: driveUri.substringAfterLast("/")
            } else ""
            val driveBackups = if (driveUri.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        DriveBackupWorker.listBackupFiles(context, Uri.parse(driveUri))
                    }.getOrDefault(emptyList())
                }
            } else emptyList()

            _state.value = _state.value.copy(
                localBackups       = localBackups,
                driveBackups       = driveBackups,
                driveFolderUri     = driveUri,
                driveFolderName    = driveName,
                driveBackupEnabled = settings.driveBackupEnabled,
                driveLastBackupMs  = settings.driveBackupLastDate,
                monoflowUrl        = settings.monoflowUrl,
                monoflowToken      = settings.monoflowToken,
                monoflowAutoSync   = settings.monoflowAutoSync,
                monoflowLastSyncMs = settings.monoflowLastSyncMs
            )
        }
    }

    // ── JSON Експорт ──────────────────────────────────────────────────────────

    /** Збирає всі дані і повертає JSON рядок (suspend) */
    suspend fun buildExportJson(): String = withContext(Dispatchers.IO) {
        val accounts     = accountDao.getAllAccountsOnce()
        val categories   = categoryDao.getAllCategoriesOnce()
        val transactions = txDao.getAllTransactions()
        BackupSerializer.serialize(
            BackupData(BackupSerializer.BACKUP_VERSION, System.currentTimeMillis(),
                accounts, categories, transactions)
        )
    }

    fun writeExportToUri(context: Context, uri: Uri, json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                showMessage("Експорт успішно збережено ✓")
            } catch (e: Exception) {
                showMessage("Помилка запису: ${e.message}")
            }
        }
    }

    // ── JSON Імпорт ───────────────────────────────────────────────────────────

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true)
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.readText()
                } ?: throw Exception("Не вдалося прочитати файл")

                val data = withContext(Dispatchers.IO) {
                    BackupSerializer.deserialize(json)
                }
                importBackupData(data)
                showMessage("Імпорт успішно завершено: " +
                        "${data.accounts.size} рахунків, " +
                        "${data.categories.size} категорій, " +
                        "${data.transactions.size} операцій ✓")
                loadState(context)
            } catch (e: Exception) {
                showMessage("Помилка імпорту: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isImporting = false)
            }
        }
    }

    private suspend fun importBackupData(data: BackupData) = withContext(Dispatchers.IO) {
        val sanitizedCategories = data.categories.map { cat ->
            if (cat.icon == "category") {
                val (icon, color) = suggestCategoryStyle(cat.name, cat.type)
                cat.copy(icon = icon, colorHex = color)
            } else cat
        }
        // Порядок: спочатку рахунки і категорії, потім транзакції (FK)
        txDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
        categoryDao.deleteAllCategories()
        accountDao.insertAccounts(data.accounts)
        categoryDao.insertCategories(sanitizedCategories)
        txDao.insertTransactions(data.transactions)
    }

    // ── CSV Експорт ───────────────────────────────────────────────────────────

    suspend fun buildCsvShareIntentSuspend(context: Context): Intent? = withContext(Dispatchers.IO) {
        try {
            val all = txDao.getAllTransactionsWithDetails()
            CsvExporter.export(context, all)
        } catch (e: Exception) { null }
    }

    // ── Google Drive ──────────────────────────────────────────────────────────

    fun configureDriveFolder(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            // Дозвіл на читання/запис
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)

            val uriStr = treeUri.toString()
            settingsRepo.update {
                this[SettingsRepository.KEY_DRIVE_BACKUP_FOLDER_URI] = uriStr
            }
            loadState(context)
        }
    }

    fun clearDriveFolder(context: Context) {
        viewModelScope.launch {
            settingsRepo.update {
                this[SettingsRepository.KEY_DRIVE_BACKUP_FOLDER_URI] = ""
                this[SettingsRepository.KEY_DRIVE_BACKUP_ENABLED]    = false
            }
            DriveBackupWorker.cancel(context)
            _state.value = _state.value.copy(
                driveFolderUri    = "",
                driveFolderName   = "",
                driveBackupEnabled = false,
                driveBackups      = emptyList()
            )
        }
    }

    fun setDriveAutoBackup(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.update {
                this[SettingsRepository.KEY_DRIVE_BACKUP_ENABLED] = enabled
            }
            if (enabled) DriveBackupWorker.schedule(context)
            else         DriveBackupWorker.cancel(context)
            _state.value = _state.value.copy(driveBackupEnabled = enabled)
        }
    }

    fun backupToDriveNow(context: Context) {
        if (_state.value.isBacking) return
        val folderUri = _state.value.driveFolderUri
        if (folderUri.isBlank()) {
            showMessage("Спочатку виберіть папку в Google Drive")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isBacking = true)
            try {
                val accounts     = withContext(Dispatchers.IO) { accountDao.getAllAccountsOnce() }
                val categories   = withContext(Dispatchers.IO) { categoryDao.getAllCategoriesOnce() }
                val transactions = withContext(Dispatchers.IO) { txDao.getAllTransactions() }

                val json = withContext(Dispatchers.IO) {
                    BackupSerializer.serialize(
                        BackupData(BackupSerializer.BACKUP_VERSION,
                            System.currentTimeMillis(), accounts, categories, transactions)
                    )
                }

                val treeUri  = Uri.parse(folderUri)
                val dateFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fileName = "moneyiq_${dateFmt.format(Date())}.json"

                val ok = withContext(Dispatchers.IO) {
                    try {
                        val docUri = DriveBackupWorker.createDocumentInFolder(context, treeUri, fileName)
                            ?: return@withContext false
                        context.contentResolver.openOutputStream(docUri)?.use { os ->
                            os.write(json.toByteArray(Charsets.UTF_8))
                            os.flush()
                        }
                        true
                    } catch (e: Exception) { false }
                }

                if (ok) {
                    val now = System.currentTimeMillis()
                    settingsRepo.update {
                        this[SettingsRepository.KEY_DRIVE_BACKUP_LAST_DATE] = now
                    }
                    showMessage("Резервну копію збережено в Google Drive ✓")
                    loadState(context)
                } else {
                    showMessage("Не вдалося записати в папку Drive")
                }
            } catch (e: Exception) {
                showMessage("Помилка: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isBacking = false)
            }
        }
    }

    fun restoreFromDrive(context: Context, treeUri: Uri, entry: DriveBackupEntry) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true)
            try {
                val json = withContext(Dispatchers.IO) {
                    DriveBackupWorker.readBackupFile(context, treeUri, entry.documentId)
                } ?: throw Exception("Не вдалося прочитати файл Drive")
                val data = withContext(Dispatchers.IO) { BackupSerializer.deserialize(json) }
                importBackupData(data)
                showMessage("Відновлено з Drive: ${entry.name} ✓")
                loadState(context)
            } catch (e: Exception) {
                showMessage("Помилка відновлення: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isImporting = false)
            }
        }
    }

    // ── MonoFlow синхронізація ────────────────────────────────────────────────

    fun saveMonoFlowConfig(context: Context, url: String, token: String) {
        viewModelScope.launch {
            settingsRepo.update {
                this[SettingsRepository.KEY_MONOFLOW_URL]       = url.trimEnd('/')
                this[SettingsRepository.KEY_MONOFLOW_TOKEN]     = token
                this[SettingsRepository.KEY_MONOFLOW_LAST_SYNC] = 0L  // скидаємо → наступний синк = повний
            }
            _state.value = _state.value.copy(
                monoflowUrl        = url.trimEnd('/'),
                monoflowToken      = token,
                monoflowLastSyncMs = 0L
            )
            // Одразу синхронізуємось
            syncFromMonoFlowNow(context)
        }
    }

    fun clearMonoFlowConfig(context: Context) {
        viewModelScope.launch {
            settingsRepo.update {
                this[SettingsRepository.KEY_MONOFLOW_URL]       = ""
                this[SettingsRepository.KEY_MONOFLOW_TOKEN]     = ""
                this[SettingsRepository.KEY_MONOFLOW_AUTO_SYNC] = false
                this[SettingsRepository.KEY_MONOFLOW_LAST_SYNC] = 0L
            }
            MonoFlowSyncWorker.cancel(context)
            _state.value = _state.value.copy(
                monoflowUrl        = "",
                monoflowToken      = "",
                monoflowAutoSync   = false,
                monoflowLastSyncMs = 0L
            )
        }
    }

    fun setMonoFlowAutoSync(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.update {
                this[SettingsRepository.KEY_MONOFLOW_AUTO_SYNC] = enabled
            }
            if (enabled) MonoFlowSyncWorker.schedule(context)
            else         MonoFlowSyncWorker.cancel(context)
            _state.value = _state.value.copy(monoflowAutoSync = enabled)
        }
    }

    fun syncFromMonoFlowNow(context: Context) {
        if (_state.value.isSyncing) return
        val url   = _state.value.monoflowUrl
        val token = _state.value.monoflowToken
        if (url.isBlank() || token.isBlank()) {
            showMessage("MonoFlow: налаштуйте URL та токен")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true)
            try {
                val since = _state.value.monoflowLastSyncMs
                val json  = withContext(Dispatchers.IO) {
                    val base = if (url.endsWith("/api/sync")) url else "$url/api/sync"
                    val conn = java.net.URL("$base?since=$since")
                        .openConnection() as java.net.HttpURLConnection
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.connectTimeout = 15_000
                    conn.readTimeout    = 60_000
                    val code = conn.responseCode
                    if (code != 200) throw Exception("HTTP $code")
                    conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                }
                val data = withContext(Dispatchers.IO) { BackupSerializer.deserialize(json) }
                mergeBackupData(data)
                val now = System.currentTimeMillis()
                settingsRepo.update {
                    this[SettingsRepository.KEY_MONOFLOW_LAST_SYNC] = now
                }
                _state.value = _state.value.copy(monoflowLastSyncMs = now)
                showMessage(
                    "MonoFlow синхронізовано ✓ " +
                    "(${data.accounts.size} рахунків, ${data.transactions.size} операцій)"
                )
                loadState(context)
            } catch (e: Exception) {
                showMessage("MonoFlow: помилка — ${e.message}")
            } finally {
                _state.value = _state.value.copy(isSyncing = false)
            }
        }
    }

    /** MERGE-імпорт: вставляє/оновлює дані БЕЗ видалення існуючих (on conflict REPLACE by id) */
    private suspend fun mergeBackupData(data: BackupData) = withContext(Dispatchers.IO) {
        val sanitizedCategories = data.categories.map { cat ->
            if (cat.icon == "category") {
                val (icon, color) = suggestCategoryStyle(cat.name, cat.type)
                cat.copy(icon = icon, colorHex = color)
            } else cat
        }
        // Порядок: спочатку рахунки і категорії, потім транзакції (FK)
        accountDao.insertAccounts(data.accounts)
        categoryDao.insertCategories(sanitizedCategories)
        txDao.insertTransactions(data.transactions)
    }

    // ── Локальний бекап ───────────────────────────────────────────────────────

    fun createLocalBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val txCount  = txDao.count()
                val accCount = accountDao.count()
                val catCount = categoryDao.count()
                val dbFile   = context.getDatabasePath("moneyiq.db")
                val backupDir = File(context.filesDir, "backups").also { it.mkdirs() }
                val ts = System.currentTimeMillis()
                dbFile.copyTo(File(backupDir, "backup_$ts.db"), overwrite = true)
                val entry = BackupEntry(ts, txCount, accCount, catCount)
                val prefs = context.getSharedPreferences("moneyiq_backups", Context.MODE_PRIVATE)
                val current = parseLocalBackupsFromPrefs(prefs).toMutableList()
                current.add(0, entry)
                if (current.size > 10) current.removeAt(current.lastIndex)
                saveLocalBackups(prefs, current)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(localBackups = current)
                    Toast.makeText(context, "Резервну копію створено ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Скидання даних ────────────────────────────────────────────────────────

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            txDao.deleteAllTransactions()
            accountDao.deleteAllAccounts()
            categoryDao.deleteAllCategories()
        }
    }

    fun deleteAllTransactions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            txDao.deleteAllTransactions()
            accountDao.resetAllBalances()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Всі операції видалено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Утиліти ───────────────────────────────────────────────────────────────

    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    private fun showMessage(msg: String) {
        viewModelScope.launch { _state.value = _state.value.copy(message = msg) }
    }

    private fun parseLocalBackups(context: Context): List<BackupEntry> {
        val prefs = context.getSharedPreferences("moneyiq_backups", Context.MODE_PRIVATE)
        return parseLocalBackupsFromPrefs(prefs)
    }

    private fun parseLocalBackupsFromPrefs(prefs: android.content.SharedPreferences): List<BackupEntry> {
        val raw = prefs.getString("backups_list", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { item ->
            val p = item.split(",")
            if (p.size == 4) BackupEntry(
                p[0].toLongOrNull() ?: return@mapNotNull null,
                p[1].toIntOrNull() ?: 0,
                p[2].toIntOrNull() ?: 0,
                p[3].toIntOrNull() ?: 0
            ) else null
        }
    }

    private fun saveLocalBackups(prefs: android.content.SharedPreferences, list: List<BackupEntry>) {
        prefs.edit().putString("backups_list",
            list.joinToString(";") { "${it.timestamp},${it.txCount},${it.accountCount},${it.categoryCount}" }
        ).apply()
    }
}

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

// ── MonoFlow Sync Card ────────────────────────────────────────────────────────

@Composable
private fun MonoFlowSyncCard(
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
private fun DataSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun DataActionItem(
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
private fun DriveBackupItem(
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
private fun LocalBackupItem(backup: BackupEntry) {
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

private fun pluralUk(n: Int, one: String, few: String, many: String): String {
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
private fun ResetDataDialog(
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
