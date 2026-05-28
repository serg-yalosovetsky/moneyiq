package org.pixelrush.moneyiq.workers

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import org.pixelrush.moneyiq.data.repository.SettingsRepository
import org.pixelrush.moneyiq.util.BackupData
import org.pixelrush.moneyiq.util.BackupSerializer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Hilt Entry Point для доступу до сховища з Worker ─────────────────────────

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DriveBackupEntryPoint {
    fun accountDao(): AccountDao
    fun categoryDao(): CategoryDao
    fun transactionDao(): TransactionDao
    fun settingsRepository(): SettingsRepository
}

// ── Worker ────────────────────────────────────────────────────────────────────

class DriveBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ep = EntryPointAccessors.fromApplication(
                applicationContext, DriveBackupEntryPoint::class.java
            )
            val settings = ep.settingsRepository().settings.first()

            // Якщо папка не вибрана — нічого не робимо
            val folderUriStr = settings.driveBackupFolderUri
            if (folderUriStr.isBlank()) return@withContext Result.success()

            val folderUri = Uri.parse(folderUriStr)

            // Збираємо дані
            val accounts     = ep.accountDao().getAllAccountsOnce()
            val categories   = ep.categoryDao().getAllCategoriesOnce()
            val transactions = ep.transactionDao().getAllTransactions()

            val backupData = BackupData(
                version      = BackupSerializer.BACKUP_VERSION,
                exportDate   = System.currentTimeMillis(),
                accounts     = accounts,
                categories   = categories,
                transactions = transactions
            )
            val json = BackupSerializer.serialize(backupData)

            // Ім'я файлу — дата
            val dateFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fileName = "moneyiq_${dateFmt.format(Date())}.json"

            // Видаляємо старий бекап за сьогодні (якщо є)
            deleteFileByName(applicationContext, folderUri, fileName)

            // Створюємо новий документ у папці
            val docUri = createDocumentInFolder(applicationContext, folderUri, fileName)
                ?: return@withContext Result.failure()

            applicationContext.contentResolver.openOutputStream(docUri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            // Зберігаємо час останнього бекапу
            ep.settingsRepository().update {
                this[SettingsRepository.KEY_DRIVE_BACKUP_LAST_DATE] = System.currentTimeMillis()
            }

            // Видаляємо старі бекапи (залишаємо 30 останніх)
            pruneOldBackups(applicationContext, folderUri, keepCount = 30)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    // ── Допоміжні функції ─────────────────────────────────────────────────────

    companion object {
        const val WORK_NAME = "drive_daily_backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(calcInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /** Планує запуск о 2:00 ночі наступного дня */
        private fun calcInitialDelay(): Long {
            val now    = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DATE, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }

        // ── SAF-утиліти ───────────────────────────────────────────────────────

        fun createDocumentInFolder(context: Context, treeUri: Uri, fileName: String): Uri? {
            return try {
                val docId    = DocumentsContract.getTreeDocumentId(treeUri)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                DocumentsContract.createDocument(
                    context.contentResolver, parentUri, "application/json", fileName
                )
            } catch (e: Exception) { null }
        }

        fun listBackupFiles(context: Context, treeUri: Uri): List<DriveBackupEntry> {
            val result = mutableListOf<DriveBackupEntry>()
            return try {
                val docId       = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                val cursor = context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null, null, null
                )
                cursor?.use {
                    val idIdx   = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val modIdx  = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val sizeIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val mimeIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    while (it.moveToNext()) {
                        val mime = it.getString(mimeIdx) ?: ""
                        val name = it.getString(nameIdx) ?: ""
                        if (mime == "application/json" || name.endsWith(".json")) {
                            result += DriveBackupEntry(
                                documentId  = it.getString(idIdx),
                                name        = name,
                                modifiedMs  = it.getLong(modIdx),
                                sizeBytes   = it.getLong(sizeIdx)
                            )
                        }
                    }
                }
                result.sortedByDescending { it.modifiedMs }
            } catch (e: Exception) {
                result
            }
        }

        fun readBackupFile(context: Context, treeUri: Uri, documentId: String): String? {
            return try {
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                context.contentResolver.openInputStream(docUri)?.use {
                    it.bufferedReader(Charsets.UTF_8).readText()
                }
            } catch (e: Exception) { null }
        }

        private fun deleteFileByName(context: Context, treeUri: Uri, name: String) {
            try {
                val found = listBackupFiles(context, treeUri).firstOrNull { it.name == name }
                    ?: return
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, found.documentId)
                DocumentsContract.deleteDocument(context.contentResolver, docUri)
            } catch (_: Exception) {}
        }

        fun deleteFile(context: Context, treeUri: Uri, documentId: String) {
            try {
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                DocumentsContract.deleteDocument(context.contentResolver, docUri)
            } catch (_: Exception) {}
        }

        private fun pruneOldBackups(context: Context, treeUri: Uri, keepCount: Int) {
            val all = listBackupFiles(context, treeUri) // sorted newest first
            if (all.size <= keepCount) return
            all.drop(keepCount).forEach { entry ->
                deleteFile(context, treeUri, entry.documentId)
            }
        }

        fun getFolderDisplayName(context: Context, treeUri: Uri): String? {
            return try {
                val docId  = DocumentsContract.getTreeDocumentId(treeUri)
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                context.contentResolver.query(
                    docUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null
                )?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            } catch (e: Exception) { null }
        }
    }
}

// ── Модель елемента Drive-бекапу ──────────────────────────────────────────────

data class DriveBackupEntry(
    val documentId: String,
    val name: String,
    val modifiedMs: Long,
    val sizeBytes: Long
)
