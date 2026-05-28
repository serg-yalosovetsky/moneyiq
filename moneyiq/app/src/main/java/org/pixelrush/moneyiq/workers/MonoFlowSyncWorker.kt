package org.pixelrush.moneyiq.workers

import android.content.Context
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
import org.pixelrush.moneyiq.util.BackupSerializer
import java.util.concurrent.TimeUnit

// ── Hilt Entry Point ──────────────────────────────────────────────────────────

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MonoFlowSyncEntryPoint {
    fun accountDao(): AccountDao
    fun categoryDao(): CategoryDao
    fun transactionDao(): TransactionDao
    fun settingsRepository(): SettingsRepository
}

// ── Worker ────────────────────────────────────────────────────────────────────

class MonoFlowSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ep = EntryPointAccessors.fromApplication(
                applicationContext, MonoFlowSyncEntryPoint::class.java
            )
            val settings = ep.settingsRepository().settings.first()

            // Якщо не налаштовано — нічого не робимо
            val url   = settings.monoflowUrl.trimEnd('/')
            val token = settings.monoflowToken
            if (url.isBlank() || token.isBlank()) return@withContext Result.success()

            val since = settings.monoflowLastSyncMs

            // Отримуємо JSON з сервера
            val json = fetchJson(url, token, since)

            // Парсимо
            val data = BackupSerializer.deserialize(json)

            // MERGE: insert/replace по id, не видаляємо існуючі дані
            ep.accountDao().insertAccounts(data.accounts)
            ep.categoryDao().insertCategories(data.categories)
            ep.transactionDao().insertTransactions(data.transactions)

            // Оновлюємо час останньої синхронізації
            ep.settingsRepository().update {
                this[SettingsRepository.KEY_MONOFLOW_LAST_SYNC] = System.currentTimeMillis()
            }

            Result.success()
        } catch (e: Exception) {
            // Retry при мережевих помилках, failure при parse-помилках
            if (e is java.net.SocketTimeoutException ||
                e is java.net.ConnectException ||
                e is java.io.IOException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    // ── HTTP (без зовнішніх залежностей) ─────────────────────────────────────

    private fun fetchJson(baseUrl: String, token: String, sinceMs: Long): String {
        val conn = java.net.URL("$baseUrl/api/sync?since=$sinceMs")
            .openConnection() as java.net.HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 15_000
        conn.readTimeout    = 60_000
        val code = conn.responseCode
        if (code != 200) {
            throw Exception("HTTP $code from MonoFlow")
        }
        return conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        const val WORK_NAME = "monoflow_auto_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MonoFlowSyncWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<MonoFlowSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
