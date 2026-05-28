package org.pixelrush.moneyiq.ui.data

import android.content.Context
import android.widget.Toast
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── Data models ───────────────────────────────────────────────────────────────

data class BackupEntry(
    val timestamp: Long,
    val txCount: Int,
    val accountCount: Int,
    val categoryCount: Int
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class DataViewModel @Inject constructor(
    private val txDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _backups = MutableStateFlow<List<BackupEntry>>(emptyList())
    val backups: StateFlow<List<BackupEntry>> = _backups

    fun loadBackups(context: Context) {
        val prefs = context.getSharedPreferences("moneyiq_backups", Context.MODE_PRIVATE)
        _backups.value = parseBackups(prefs)
    }

    fun createBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val txCount = txDao.count()
                val accCount = accountDao.count()
                val catCount = categoryDao.count()

                val dbFile = context.getDatabasePath("moneyiq.db")
                val backupDir = File(context.filesDir, "backups")
                backupDir.mkdirs()
                val timestamp = System.currentTimeMillis()
                val backupFile = File(backupDir, "backup_$timestamp.db")
                dbFile.copyTo(backupFile, overwrite = true)

                val entry = BackupEntry(timestamp, txCount, accCount, catCount)
                val prefs = context.getSharedPreferences("moneyiq_backups", Context.MODE_PRIVATE)
                val current = parseBackups(prefs).toMutableList()
                current.add(0, entry)
                if (current.size > 10) current.removeAt(current.lastIndex)
                saveBackups(prefs, current)
                _backups.value = current

                withContext(Dispatchers.Main) {
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

    private fun parseBackups(prefs: android.content.SharedPreferences): List<BackupEntry> {
        val raw = prefs.getString("backups_list", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { item ->
            val parts = item.split(",")
            if (parts.size == 4) BackupEntry(
                parts[0].toLongOrNull() ?: return@mapNotNull null,
                parts[1].toIntOrNull() ?: 0,
                parts[2].toIntOrNull() ?: 0,
                parts[3].toIntOrNull() ?: 0
            ) else null
        }
    }

    private fun saveBackups(prefs: android.content.SharedPreferences, list: List<BackupEntry>) {
        prefs.edit()
            .putString("backups_list", list.joinToString(";") {
                "${it.timestamp},${it.txCount},${it.accountCount},${it.categoryCount}"
            })
            .apply()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val backups by viewModel.backups.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadBackups(context) }

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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ListItem(
                    modifier = Modifier.clickable { showResetDialog = true },
                    leadingContent = {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    headlineContent = { Text("Скинути дані") }
                )
                HorizontalDivider()
            }

            item { DataSectionHeader("Експорт") }

            item {
                DataRowItem(
                    icon = Icons.Default.FileDownload,
                    title = "Імпорт даних",
                    premium = true,
                    onClick = {}
                )
            }
            item {
                DataRowItem(
                    icon = Icons.Default.FileUpload,
                    title = "Експорт даних",
                    premium = false,
                    onClick = {}
                )
            }
            item {
                DataRowItem(
                    icon = Icons.Default.TableChart,
                    title = "Експортувати дані в CSV",
                    premium = true,
                    onClick = {}
                )
                HorizontalDivider()
            }

            item { DataSectionHeader("Резервне копіювання даних") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Резервні копії зберігаються локально на твоєму пристрої. " +
                                "Видалення чи перевстановлення застосунку призведе до їхньої втрати.",
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                ListItem(
                    modifier = Modifier.clickable { viewModel.createBackup(context) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text(
                            "Створити резервну копію",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }

            items(backups) { backup ->
                BackupListItem(backup = backup)
            }
        }
    }

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
}

// ── Private composables ───────────────────────────────────────────────────────

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
private fun DataRowItem(
    icon: ImageVector,
    title: String,
    premium: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(modifier = Modifier.size(36.dp)) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                if (premium) {
                    Icon(
                        Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd),
                        tint = Color(0xFFFFB300)
                    )
                }
            }
        },
        headlineContent = { Text(title) }
    )
}

@Composable
private fun BackupListItem(backup: BackupEntry) {
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy 'р.' H:mm", Locale("uk")) }
    val dateStr = dateFormat.format(Date(backup.timestamp))
    val txLabel = pluralUk(backup.txCount, "операція", "операції", "операцій")
    val accLabel = pluralUk(backup.accountCount, "рахунок", "рахунки", "рахунків")
    val catLabel = pluralUk(backup.categoryCount, "категорія", "категорії", "категорій")

    ListItem(
        leadingContent = {
            Box(modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd),
                    tint = Color(0xFFFFB300)
                )
            }
        },
        headlineContent = { Text("Щоденне резервування", fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Text(
                    "${backup.txCount} $txLabel · ${backup.accountCount} $accLabel · ${backup.categoryCount} $catLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Скинути дані",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Щоб видалити всі ваші дані (рахунки, категорії, операції та бюджети), " +
                            "виберіть Видалити всі дані.\n\n" +
                            "Щоб видалити лише операції, виберіть Видалити всі операції.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDeleteAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Видалити всі дані", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDeleteTransactions) {
                    Text("Видалити всі операції")
                }
                TextButton(onClick = onDismiss) {
                    Text("Відмінити")
                }
            }
        }
    }
}
