package org.pixelrush.moneyiq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.pixelrush.moneyiq.ui.main.MainScreen
import org.pixelrush.moneyiq.ui.transactions.AddTransactionScreen

sealed class Screen(val route: String) {
    object Main    : Screen("main")
    object AddTx   : Screen("add_transaction")
    object EditTx  : Screen("edit_transaction/{txId}") {
        fun buildRoute(txId: Long) = "edit_transaction/$txId"
    }
}

@Composable
fun MoneyIQNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {

        composable(Screen.Main.route) {
            MainScreen(
                onAddTransaction = { navController.navigate(Screen.AddTx.route) },
                onEditTransaction = { txId ->
                    navController.navigate(Screen.EditTx.buildRoute(txId))
                }
            )
        }

        composable(Screen.AddTx.route) {
            AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EditTx.route,
            arguments = listOf(navArgument("txId") { type = NavType.LongType })
        ) {
            AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
