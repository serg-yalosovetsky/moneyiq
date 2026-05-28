package org.pixelrush.moneyiq

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.BuildConfig
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import javax.inject.Inject

@HiltAndroidApp
class MoneyIQApp : Application() {

    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var accountDao: AccountDao

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = "https://8f8838dbabb042f825cb7b96f1a8f6d6@o4504272346480640.ingest.us.sentry.io/4511470109720576"
            options.isDebug = BuildConfig.DEBUG
            options.tracesSampleRate = 1.0
        }
        appScope.launch { seedInitialData() }
    }

    private suspend fun seedInitialData() {
        // Категории по умолчанию (seedDefaults внутри проверяет count > 0)
        categoryRepository.seedDefaults()

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
