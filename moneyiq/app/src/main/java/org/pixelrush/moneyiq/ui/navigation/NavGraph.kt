package org.pixelrush.moneyiq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.pixelrush.moneyiq.ui.main.MainScreen
import org.pixelrush.moneyiq.ui.transactions.AddTransactionScreen

sealed class Screen(val route: String) {
    object Main  : Screen("main")
    object AddTx : Screen("add_transaction")
}

@Composable
fun MoneyIQNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {

        composable(Screen.Main.route) {
            MainScreen(
                onAddTransaction = { navController.navigate(Screen.AddTx.route) }
            )
        }

        composable(Screen.AddTx.route) {
            AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
