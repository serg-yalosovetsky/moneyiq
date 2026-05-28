package org.pixelrush.moneyiq.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.repository.*
import org.pixelrush.moneyiq.workers.NotificationWorker
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings = repo.settings.stateIn(
        scope          = viewModelScope,
        started        = SharingStarted.WhileSubscribed(5_000),
        initialValue   = AppSettings()
    )

    fun setThemeMode(mode: ThemeMode) = save { it[SettingsRepository.KEY_THEME_MODE] = mode.name }
    fun setAccentColor(argb: Int)     = save { it[SettingsRepository.KEY_ACCENT_COLOR] = argb }
    fun setHomeScreen(tab: HomeScreenTab) = save { it[SettingsRepository.KEY_HOME_SCREEN] = tab.name }
    fun setBudgetVisible(v: Boolean)  = save { it[SettingsRepository.KEY_BUDGET_VISIBLE] = v }
    fun setLoginProtection(v: Boolean) = save { it[SettingsRepository.KEY_LOGIN_PROTECTION] = v }
    fun setDefaultCurrency(code: String) = save { it[SettingsRepository.KEY_DEFAULT_CURRENCY] = code }
    fun setCurrencyFormat(idx: Int)   = save { it[SettingsRepository.KEY_CURRENCY_FORMAT] = idx }
    fun setFirstDayOfWeek(day: Int)   = save { it[SettingsRepository.KEY_FIRST_DAY_OF_WEEK] = day }
    fun setFirstDayOfMonth(day: Int)  = save { it[SettingsRepository.KEY_FIRST_DAY_OF_MONTH] = day }
    fun setLanguage(lang: String)     = save { it[SettingsRepository.KEY_LANGUAGE] = lang }

    fun setNotifications(enabled: Boolean, hour: Int, minute: Int) {
        save {
            it[SettingsRepository.KEY_NOTIFICATIONS]  = enabled
            it[SettingsRepository.KEY_NOTIF_HOUR]     = hour
            it[SettingsRepository.KEY_NOTIF_MINUTE]   = minute
        }
        if (enabled) NotificationWorker.schedule(context, hour, minute)
        else         NotificationWorker.cancel(context)
    }

    private fun save(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { repo.update { block(this) } }
    }
}
