package org.pixelrush.moneyiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import org.pixelrush.moneyiq.ui.navigation.MoneyIQNavGraph
import org.pixelrush.moneyiq.ui.theme.MoneyIQTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyIQTheme {
                MoneyIQNavGraph()
            }
        }
    }
}
