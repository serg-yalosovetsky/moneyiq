package org.pixelrush.moneyiq.ui.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
    val monoflowUrl: String                   = "",
    val monoflowToken: String                 = "",
    val monoflowAutoSync: Boolean             = false,
    val monoflowLastSyncMs: Long              = 0L,
    val isSyncing: Boolean                    = false
)

// TODO: Migrate direct DAO access to repository layer
@HiltViewModel
class DataViewModel @Inject constructor(
    private val txDao:       TransactionDao,
    private val accountDao:  AccountDao,
    private val categoryDao: CategoryDao,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state

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

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true)
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.readText()
                } ?: throw Exception("Не вдалося прочитати файл")

                val data = withContext(Dispatchers.IO) { BackupSerializer.deserialize(json) }
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

    private fun normalizeImportedCategory(cat: org.pixelrush.moneyiq.data.db.entities.CategoryEntity): org.pixelrush.moneyiq.data.db.entities.CategoryEntity {
        val n = cat.name.lowercase().trim()
        if (cat.parentId != null) {
            when {
                n.contains("food delivery") || n == "glovo" || n.contains("bolt food") || n.contains("uber eats") || n.contains("uklon food") ->
                    return cat.copy(icon = "delivery", colorHex = "#FF6F00")
                n.contains("кафе") || n.contains("cafe") || n.contains("кав'ярн") ->
                    return cat.copy(icon = "coffee", colorHex = "#795548")
                n.contains("ресторан") && n != "ресторація" ->
                    return cat.copy(icon = "restaurant", colorHex = "#E53935")
            }
        }
        if (cat.icon == "category") {
            val (icon, color) = suggestCategoryStyle(cat.name, cat.type)
            return cat.copy(icon = icon, colorHex = color)
        }
        return when (cat.icon) {
            "movie"  -> if (cat.colorHex != "#9C27B0") cat.copy(colorHex = "#9C27B0") else cat
            "coffee" -> if (cat.colorHex != "#795548") cat.copy(colorHex = "#795548") else cat
            else -> cat
        }
    }

    private suspend fun importBackupData(data: BackupData) = withContext(Dispatchers.IO) {
        val sanitizedCategories = data.categories.map { normalizeImportedCategory(it) }
        txDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
        categoryDao.deleteAllCategories()
        accountDao.insertAccounts(data.accounts)
        categoryDao.insertCategories(sanitizedCategories)
        txDao.insertTransactions(data.transactions)
    }

    suspend fun buildCsvShareIntentSuspend(context: Context): android.content.Intent? = withContext(Dispatchers.IO) {
        try {
            val all = txDao.getAllTransactionsWithDetails()
            CsvExporter.export(context, all)
        } catch (e: Exception) { null }
    }

    fun configureDriveFolder(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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

    fun saveMonoFlowConfig(context: Context, url: String, token: String) {
        viewModelScope.launch {
            settingsRepo.update {
                this[SettingsRepository.KEY_MONOFLOW_URL]       = url.trimEnd('/')
                this[SettingsRepository.KEY_MONOFLOW_TOKEN]     = token
                this[SettingsRepository.KEY_MONOFLOW_LAST_SYNC] = 0L
            }
            _state.value = _state.value.copy(
                monoflowUrl        = url.trimEnd('/'),
                monoflowToken      = token,
                monoflowLastSyncMs = 0L
            )
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

    private suspend fun mergeBackupData(data: BackupData) = withContext(Dispatchers.IO) {
        val sanitizedCategories = data.categories.map { normalizeImportedCategory(it) }
        accountDao.insertAccounts(data.accounts)
        categoryDao.insertCategories(sanitizedCategories)
        txDao.insertTransactions(data.transactions)
    }

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
