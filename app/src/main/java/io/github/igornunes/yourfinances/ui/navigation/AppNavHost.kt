package io.github.igornunes.yourfinances.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.igornunes.yourfinances.ui.entry.EntryFormRoute
import io.github.igornunes.yourfinances.ui.month.MonthRoute

private const val MONTH = "month"
private const val ENTRY_ID = "id"
private const val NO_ENTRY = -1L
private const val ENTRY = "entry?$ENTRY_ID={$ENTRY_ID}"

private fun entryRoute(id: Long? = null) = "entry?$ENTRY_ID=${id ?: NO_ENTRY}"

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MONTH) {
        composable(MONTH) { backStackEntry ->
            // Ignores a second tap while the transition to the form is already running.
            fun navigateIfResumed(route: String) {
                if (backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(route)
            }
            MonthRoute(
                onAddEntry = { navigateIfResumed(entryRoute()) },
                onOpenEntry = { id -> navigateIfResumed(entryRoute(id)) },
            )
        }
        composable(
            route = ENTRY,
            arguments = listOf(navArgument(ENTRY_ID) { type = NavType.LongType; defaultValue = NO_ENTRY }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(ENTRY_ID)?.takeIf { it != NO_ENTRY }
            EntryFormRoute(entryId = id, onDone = { navController.popBackStack(MONTH, inclusive = false) })
        }
    }
}
