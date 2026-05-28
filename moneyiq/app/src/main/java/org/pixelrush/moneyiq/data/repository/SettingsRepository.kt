package org.pixelrush.moneyiq.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HomeScreenTab(val index: Int, val label: String) {
    ACCOUNTS(0, "Рахунки"),
    CATEGORIES(1, "Категорії"),
    TRANSACTIONS(2, "Операції"),
    BUDGET(3, "Бюджет"),
    OVERVIEW(4, "Огляд");

    companion object {
        fun fromIndex(index: Int) = entries.find { it.index == index } ?: CATEGORIES
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorArgb: Int = 0xFF4D5C92.toInt(),
    val homeScreen: HomeScreenTab = HomeScreenTab.CATEGORIES,
    val budgetVisible: Boolean = true,
    val loginProtectionEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 20,
    val notificationMinute: Int = 0,
    val defaultCurrency: String = "UAH",
    val currencyFormatIndex: Int = 2,
    val firstDayOfWeek: Int = 2,
    val firstDayOfMonth: Int = 1,
    val language: String = "default",
    // Google Drive auto-backup
    val driveBackupFolderUri: String = "",
    val driveBackupEnabled: Boolean = false,
    val driveBackupLastDate: Long = 0L
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.settingsDataStore

    companion object {
        val KEY_THEME_MODE         = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_COLOR       = intPreferencesKey("accent_color")
        val KEY_HOME_SCREEN        = stringPreferencesKey("home_screen")
        val KEY_BUDGET_VISIBLE     = booleanPreferencesKey("budget_visible")
        val KEY_LOGIN_PROTECTION   = booleanPreferencesKey("login_protection")
        val KEY_NOTIFICATIONS      = booleanPreferencesKey("notifications")
        val KEY_NOTIF_HOUR         = intPreferencesKey("notif_hour")
        val KEY_NOTIF_MINUTE       = intPreferencesKey("notif_minute")
        val KEY_DEFAULT_CURRENCY   = stringPreferencesKey("default_currency")
        val KEY_CURRENCY_FORMAT    = intPreferencesKey("currency_format")
        val KEY_FIRST_DAY_OF_WEEK  = intPreferencesKey("first_day_of_week")
        val KEY_FIRST_DAY_OF_MONTH = intPreferencesKey("first_day_of_month")
        val KEY_LANGUAGE                = stringPreferencesKey("language")
        val KEY_DRIVE_BACKUP_FOLDER_URI = stringPreferencesKey("drive_backup_folder_uri")
        val KEY_DRIVE_BACKUP_ENABLED    = booleanPreferencesKey("drive_backup_enabled")
        val KEY_DRIVE_BACKUP_LAST_DATE  = longPreferencesKey("drive_backup_last_date")
    }

    val settings: Flow<AppSettings> = ds.data.map { p ->
        AppSettings(
            themeMode              = runCatching { ThemeMode.valueOf(p[KEY_THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            accentColorArgb        = p[KEY_ACCENT_COLOR] ?: 0xFF4D5C92.toInt(),
            homeScreen             = runCatching { HomeScreenTab.valueOf(p[KEY_HOME_SCREEN] ?: "") }.getOrDefault(HomeScreenTab.CATEGORIES),
            budgetVisible          = p[KEY_BUDGET_VISIBLE] ?: true,
            loginProtectionEnabled = p[KEY_LOGIN_PROTECTION] ?: false,
            notificationsEnabled   = p[KEY_NOTIFICATIONS] ?: true,
            notificationHour       = p[KEY_NOTIF_HOUR] ?: 20,
            notificationMinute     = p[KEY_NOTIF_MINUTE] ?: 0,
            defaultCurrency        = p[KEY_DEFAULT_CURRENCY] ?: "UAH",
            currencyFormatIndex    = p[KEY_CURRENCY_FORMAT] ?: 2,
            firstDayOfWeek         = p[KEY_FIRST_DAY_OF_WEEK] ?: 2,
            firstDayOfMonth        = p[KEY_FIRST_DAY_OF_MONTH] ?: 1,
            language               = p[KEY_LANGUAGE] ?: "default",
            driveBackupFolderUri   = p[KEY_DRIVE_BACKUP_FOLDER_URI] ?: "",
            driveBackupEnabled     = p[KEY_DRIVE_BACKUP_ENABLED] ?: false,
            driveBackupLastDate    = p[KEY_DRIVE_BACKUP_LAST_DATE] ?: 0L
        )
    }

    suspend fun update(block: MutablePreferences.() -> Unit) {
        ds.edit { block(it) }
    }
}
