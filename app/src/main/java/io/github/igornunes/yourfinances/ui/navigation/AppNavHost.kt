package io.github.igornunes.yourfinances.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.igornunes.yourfinances.ui.card.CardDetailRoute
import io.github.igornunes.yourfinances.ui.card.CardFormRoute
import io.github.igornunes.yourfinances.ui.card.CardListRoute
import io.github.igornunes.yourfinances.ui.entry.EntryFormRoute
import io.github.igornunes.yourfinances.ui.installment.InstallmentFormRoute
import io.github.igornunes.yourfinances.ui.installment.InstallmentListRoute
import io.github.igornunes.yourfinances.ui.month.MonthRoute
import io.github.igornunes.yourfinances.ui.recurrence.RecurrenceFormRoute
import io.github.igornunes.yourfinances.ui.recurrence.RecurrenceListRoute

private const val MONTH = "month"
private const val RECURRENCES = "recurrences"
private const val INSTALLMENTS = "installments"
private const val INSTALLMENT_NEW = "installment/new"
private const val ID = "id"
private const val NO_ID = -1L
private const val ENTRY = "entry?$ID={$ID}"
private const val RECURRENCE = "recurrence?$ID={$ID}"
private const val CARDS = "cards"
private const val CARD = "card/{$ID}"
private const val CARD_FORM = "cardForm?$ID={$ID}"

private fun entryRoute(id: Long? = null) = "entry?$ID=${id ?: NO_ID}"

private fun recurrenceRoute(id: Long? = null) = "recurrence?$ID=${id ?: NO_ID}"

private fun cardRoute(id: Long) = "card/$id"

private fun cardFormRoute(id: Long? = null) = "cardForm?$ID=${id ?: NO_ID}"

private val idArgument = listOf(navArgument(ID) { type = NavType.LongType; defaultValue = NO_ID })

private fun NavBackStackEntry.optionalId(): Long? = arguments?.getLong(ID)?.takeIf { it != NO_ID }

/** Ignores a second tap while the transition away from [from] is already running. */
private fun NavController.navigateFrom(from: NavBackStackEntry, route: String) {
    if (from.lifecycle.currentState == Lifecycle.State.RESUMED) navigate(route)
}

/**
 * Closes [from] only while it is the top destination, so a repeated "done" pops once. Not gated on
 * RESUMED: a form may finish during its enter transition (STARTED) and must still close.
 */
private fun NavController.closeFrom(from: NavBackStackEntry) {
    if (currentBackStackEntry?.id == from.id) popBackStack()
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MONTH) {
        composable(MONTH) { entry ->
            MonthRoute(
                onAddEntry = { navController.navigateFrom(entry, entryRoute()) },
                onOpenEntry = { id -> navController.navigateFrom(entry, entryRoute(id)) },
                onOpenRecurrences = { navController.navigateFrom(entry, RECURRENCES) },
                onOpenInstallments = { navController.navigateFrom(entry, INSTALLMENTS) },
                onOpenCards = { navController.navigateFrom(entry, CARDS) },
            )
        }
        composable(ENTRY, arguments = idArgument) { entry ->
            EntryFormRoute(entryId = entry.optionalId(), onDone = { navController.closeFrom(entry) })
        }
        composable(RECURRENCES) { entry ->
            RecurrenceListRoute(
                onBack = { navController.closeFrom(entry) },
                onAdd = { navController.navigateFrom(entry, recurrenceRoute()) },
                onOpen = { id -> navController.navigateFrom(entry, recurrenceRoute(id)) },
            )
        }
        composable(RECURRENCE, arguments = idArgument) { entry ->
            RecurrenceFormRoute(recurrenceId = entry.optionalId(), onDone = { navController.closeFrom(entry) })
        }
        composable(INSTALLMENTS) { entry ->
            InstallmentListRoute(
                onBack = { navController.closeFrom(entry) },
                onAdd = { navController.navigateFrom(entry, INSTALLMENT_NEW) },
            )
        }
        composable(INSTALLMENT_NEW) { entry ->
            InstallmentFormRoute(onDone = { navController.closeFrom(entry) })
        }
        composable(CARDS) { entry ->
            CardListRoute(
                onBack = { navController.closeFrom(entry) },
                onAdd = { navController.navigateFrom(entry, cardFormRoute()) },
                onOpen = { id -> navController.navigateFrom(entry, cardRoute(id)) },
            )
        }
        composable(CARD, arguments = listOf(navArgument(ID) { type = NavType.LongType })) { entry ->
            val cardId = requireNotNull(entry.arguments).getLong(ID)
            CardDetailRoute(
                cardId = cardId,
                onBack = { navController.closeFrom(entry) },
                onEdit = { navController.navigateFrom(entry, cardFormRoute(cardId)) },
                onOpenEntry = { id -> navController.navigateFrom(entry, entryRoute(id)) },
            )
        }
        composable(CARD_FORM, arguments = idArgument) { entry ->
            CardFormRoute(cardId = entry.optionalId(), onDone = { navController.closeFrom(entry) })
        }
    }
}
