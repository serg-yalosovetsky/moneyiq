package org.pixelrush.moneyiq

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.repository.SettingsRepository
import org.pixelrush.moneyiq.data.repository.ThemeMode
import org.pixelrush.moneyiq.ui.navigation.MoneyIQNavGraph
import org.pixelrush.moneyiq.ui.settings.SettingsViewModel
import org.pixelrush.moneyiq.ui.theme.MoneyIQTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private var lastPausedAt = 0L
    private var isUnlocked   = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = org.pixelrush.moneyiq.data.repository.AppSettings()
            )
            val systemDark  = isSystemInDarkTheme()
            val darkTheme   = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
            }
            val accentColor = Color(settings.accentColorArgb)

            MoneyIQTheme(darkTheme = darkTheme, accentColor = accentColor) {
                val unlocked by isUnlocked
                if (!unlocked) {
                    LockScreen(onUnlock = { triggerBiometric() })
                } else {
                    MoneyIQNavGraph()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lastPausedAt = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.loginProtectionEnabled) {
                val elapsed = System.currentTimeMillis() - lastPausedAt
                if (elapsed > 30_000 && lastPausedAt > 0) {
                    isUnlocked.value = false
                    triggerBiometric()
                }
            } else {
                isUnlocked.value = true
            }
        }
    }

    private fun triggerBiometric() {
        val bm = BiometricManager.from(this)
        val canAuth = bm.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            isUnlocked.value = true
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isUnlocked.value = true
            }
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                if (code == BiometricPrompt.ERROR_USER_CANCELED ||
                    code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    finish()
                }
            }
            override fun onAuthenticationFailed() {
                Toast.makeText(this@MainActivity, "Автентифікацію не пройдено", Toast.LENGTH_SHORT).show()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MoneyIQ")
            .setSubtitle("Підтвердіть особу для входу")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
}
