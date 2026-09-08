package org.syalosovetskyi.onemoney

import android.app.Application
import android.util.Log
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
import org.syalosovetskyi.onemoney.data.repository.CurrencyRatesRepository
import org.syalosovetskyi.onemoney.workers.RepeatTransactionWorker
import javax.inject.Inject

@HiltAndroidApp
class onemoneyApp : Application() {

    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var accountDao: AccountDao
    @Inject lateinit var currencyRatesRepository: CurrencyRatesRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Падения уезжают в СВОЙ GlitchTip (glitchtip.ibotz.fun, проект mesh/onemoney).
        // Публичный sentry.io запрещён правилами меша, а здесь он был особенно неуместен:
        // приложение показывает балансы Сержа.
        //
        // DSN в исходнике не живёт — только через BuildConfig (local.properties / CI).
        // Пустой DSN = отправка выключена: тогда Sentry не инициализируется вообще,
        // иначе SDK молча копит события в очередь и шлёт их в никуда.
        val dsn = BuildConfig.GLITCHTIP_DSN
        if (dsn.isBlank()) {
            Log.i(TAG, "GLITCHTIP_DSN не задан — звіти про падіння вимкнено")
        } else {
            SentryAndroid.init(this) { options ->
                options.dsn = dsn
                options.isEnabled = true
                options.environment = if (BuildConfig.DEBUG) "debug" else "production"
                options.release = "onemoney@${BuildConfig.VERSION_NAME}"
                options.sampleRate = 1.0          // 100% ошибок
                options.tracesSampleRate = 1.0    // 100% performance traces
                // Скриншот и дерево вьюх УВОЗЯТ БАЛАНСЫ целиком, а не текст ошибки, и
                // выключены даже для своего приёмника: UI GlitchTip открыт по домену.
                // Не включать без отдельного решения Сержа.
                options.isAttachScreenshot = false
                options.isAttachViewHierarchy = false
                options.isSendDefaultPii = false
                // Хлебные крошки по тапам называют экраны и элементы — для отладки
                // падения этого не нужно, а из имён видно, что человек делал с деньгами.
                options.isEnableUserInteractionTracing = false
                options.isDebug = BuildConfig.DEBUG  // подробные логи только в debug
            }
        }

        appScope.launch { seedInitialData() }
        appScope.launch { currencyRatesRepository.refreshIfStale() }
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

    private companion object {
        const val TAG = "MoneyIQApp"
    }
}
