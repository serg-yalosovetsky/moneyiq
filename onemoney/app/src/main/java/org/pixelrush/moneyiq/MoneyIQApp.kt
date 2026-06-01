package org.syalosovetskyi.onemoney

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.syalosovetskyi.onemoney.BuildConfig
import org.syalosovetskyi.onemoney.data.db.dao.AccountDao
import org.syalosovetskyi.onemoney.data.db.entities.AccountEntity
import org.syalosovetskyi.onemoney.data.db.entities.AccountType
import org.syalosovetskyi.onemoney.data.repository.CategoryRepository
import org.syalosovetskyi.onemoney.workers.RepeatTransactionWorker
import javax.inject.Inject

@HiltAndroidApp
class onemoneyApp : Application() {

    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var accountDao: AccountDao

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = "https://8f8838dbabb042f825cb7b96f1a8f6d6@o4504272346480640.ingest.us.sentry.io/4511470109720576"
            options.isEnabled = true
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            options.release = "onemoney@${BuildConfig.VERSION_NAME}"
            options.sampleRate = 1.0          // 100% ошибок
            options.tracesSampleRate = 1.0    // 100% performance traces
            options.isAttachScreenshot = true
            options.isAttachViewHierarchy = true
            options.isEnableUserInteractionTracing = true
            options.isDebug = BuildConfig.DEBUG  // подробные логи только в debug
        }

        appScope.launch { seedInitialData() }
        RepeatTransactionWorker.scheduleOnce(this)
    }

    private suspend fun seedInitialData() {
        // Категории по умолчанию (seedDefaults внутри проверяет count > 0)
        categoryRepository.seedDefaults()
        // Починяем ключи иконок для существующих категорий
        categoryRepository.repairIconKeys()
        categoryRepository.repairDefaultColors()

        // Счёт по умолчанию — только при первом запуске
        if (accountDao.count() == 0) {
            accountDao.insertAccount(
                AccountEntity(
                    name = "Кошелёк",
                    type = AccountType.CASH,
                    balance = 0.0,
                    currency = "RUB",
                    colorHex = "#4CAF50",
                    icon = "wallet",
                    includeInTotal = true,
                    sortOrder = 0
                )
            )
        }
    }
}
