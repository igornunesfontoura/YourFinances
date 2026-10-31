package io.github.igornunesfontoura.yourfinances.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.igornunesfontoura.yourfinances.ui.card.CardDetailRoute
import io.github.igornunesfontoura.yourfinances.ui.card.CardFormRoute
import io.github.igornunesfontoura.yourfinances.ui.card.CardListRoute
import io.github.igornunesfontoura.yourfinances.ui.category.CategoryFormRoute
import io.github.igornunesfontoura.yourfinances.ui.category.CategoryListRoute
import io.github.igornunesfontoura.yourfinances.ui.entry.EntryFormRoute
import io.github.igornunesfontoura.yourfinances.ui.help.HelpScreen
import io.github.igornunesfontoura.yourfinances.ui.help.WelcomeScreen
import io.github.igornunesfontoura.yourfinances.ui.goal.InvestmentGoalFormRoute
import io.github.igornunesfontoura.yourfinances.ui.home.HomeRoute
import io.github.igornunesfontoura.yourfinances.ui.importing.ImportRoute
import io.github.igornunesfontoura.yourfinances.ui.installment.InstallmentFormRoute
import io.github.igornunesfontoura.yourfinances.ui.installment.InstallmentListRoute
import io.github.igornunesfontoura.yourfinances.ui.month.MonthRoute
import io.github.igornunesfontoura.yourfinances.ui.recurrence.RecurrenceFormRoute
import io.github.igornunesfontoura.yourfinances.ui.recurrence.RecurrenceListRoute
import io.github.igornunesfontoura.yourfinances.ui.report.ReportsRoute
import io.github.igornunesfontoura.yourfinances.ui.savings.PocketFormRoute
import io.github.igornunesfontoura.yourfinances.ui.subscription.SubscriptionsRoute
import io.github.igornunesfontoura.yourfinances.ui.savings.SavingsRoute
import io.github.igornunesfontoura.yourfinances.ui.settings.SettingsRoute
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.more.MoreScreen
import java.time.YearMonth

private const val HOME = "home"
private const val ENTRIES = "entries"
private const val MORE = "more"
private const val HELP = "help"
private const val WELCOME = "welcome"
private const val MONTH_ARG = "month"
private const val GOAL = "goal/{$MONTH_ARG}"
private const val RECURRENCES = "recurrences"
private const val INSTALLMENTS = "installments"
private const val INSTALLMENT_NEW = "installment/new"
private const val ID = "id"
private const val NO_ID = -1L
private const val ENTRY = "entry?$ID={$ID}"
private const val RECURRENCE = "recurrence?$ID={$ID}"
private const val CARDS = "cards"
private const val SETTINGS = "settings"
private const val IMPORT = "import"
private const val SAVINGS = "savings"
private const val REPORTS = "reports"
private const val SUBSCRIPTIONS = "subscriptions"
private const val CATEGORIES = "categories"
private const val CATEGORY_FORM = "categoryForm?$ID={$ID}"
private const val POCKET_FORM = "pocketForm?$ID={$ID}"
private const val CARD = "card/{$ID}"
private const val CARD_FORM = "cardForm?$ID={$ID}"

private fun goalRoute(month: YearMonth) = "goal/$month"

private val monthArgument = listOf(navArgument(MONTH_ARG) { type = NavType.StringType })

private fun NavBackStackEntry.month(): YearMonth = YearMonth.parse(requireNotNull(arguments?.getString(MONTH_ARG)))

private fun entryRoute(id: Long? = null) = "entry?$ID=${id ?: NO_ID}"

private fun recurrenceRoute(id: Long? = null) = "recurrence?$ID=${id ?: NO_ID}"

private fun cardRoute(id: Long) = "card/$id"

private fun cardFormRoute(id: Long? = null) = "cardForm?$ID=${id ?: NO_ID}"

private fun pocketFormRoute(id: Long? = null) = "pocketForm?$ID=${id ?: NO_ID}"

private fun categoryFormRoute(id: Long? = null) = "categoryForm?$ID=${id ?: NO_ID}"

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

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME_TAB(HOME, R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    ENTRIES_TAB(ENTRIES, R.string.tab_entries, Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    SAVINGS_TAB(SAVINGS, R.string.tab_savings, Icons.Outlined.Savings, Icons.Filled.Savings),
    MORE_TAB(MORE, R.string.tab_more, Icons.Outlined.GridView, Icons.Filled.GridView),
}

/** Each tab keeps its own back stack and scroll position while another tab is open. */
private fun NavController.openTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/**
 * The bottom bar shows only on the tabs; inner screens and forms take the whole height.
 * [onDisableLock] asks for the device credential before the app lock is turned off.
 */
@Composable
fun AppNavHost(onDisableLock: () -> Unit) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == currentRoute }
    // Insets stay with the inner screens; only the bar's height is taken off them.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (currentTab != null) BottomBar(currentTab) { navController.openTab(it) } },
    ) { padding ->
        AppNavGraph(navController, onDisableLock, Modifier.padding(padding).consumeWindowInsets(padding))
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onSelect(tab) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun AppNavGraph(navController: NavHostController, onDisableLock: () -> Unit, modifier: Modifier) {
    NavHost(navController = navController, startDestination = HOME, modifier = modifier) {
        composable(HOME) { entry ->
            HomeRoute(
                onAddEntry = { navController.navigateFrom(entry, entryRoute()) },
                onOpenEntry = { id -> navController.navigateFrom(entry, entryRoute(id)) },
                onSeeAllEntries = { navController.openTab(Tab.ENTRIES_TAB) },
                onEditGoal = { month -> navController.navigateFrom(entry, goalRoute(month)) },
            )
        }
        composable(ENTRIES) { entry ->
            MonthRoute(
                onAddEntry = { navController.navigateFrom(entry, entryRoute()) },
                onOpenEntry = { id -> navController.navigateFrom(entry, entryRoute(id)) },
            )
        }
        composable(MORE) { entry ->
            MoreScreen(
                onOpenRecurrences = { navController.navigateFrom(entry, RECURRENCES) },
                onOpenInstallments = { navController.navigateFrom(entry, INSTALLMENTS) },
                onOpenCards = { navController.navigateFrom(entry, CARDS) },
                onOpenSubscriptions = { navController.navigateFrom(entry, SUBSCRIPTIONS) },
                onOpenReports = { navController.navigateFrom(entry, REPORTS) },
                onOpenCategories = { navController.navigateFrom(entry, CATEGORIES) },
                onOpenImport = { navController.navigateFrom(entry, IMPORT) },
                onOpenSettings = { navController.navigateFrom(entry, SETTINGS) },
                onOpenHelp = { navController.navigateFrom(entry, HELP) },
            )
        }
        composable(HELP) { entry ->
            HelpScreen(
                onBack = { navController.closeFrom(entry) },
                onReplayWelcome = { navController.navigateFrom(entry, WELCOME) },
            )
        }
        composable(WELCOME) { entry ->
            WelcomeScreen(onFinish = { navController.closeFrom(entry) })
        }
        composable(REPORTS) { entry ->
            ReportsRoute(onBack = { navController.closeFrom(entry) })
        }
        composable(SUBSCRIPTIONS) { entry ->
            SubscriptionsRoute(
                onBack = { navController.closeFrom(entry) },
                onOpenRecurrence = { id -> navController.navigateFrom(entry, recurrenceRoute(id)) },
            )
        }
        composable(CATEGORIES) { entry ->
            CategoryListRoute(
                onBack = { navController.closeFrom(entry) },
                onAdd = { navController.navigateFrom(entry, categoryFormRoute()) },
                onOpen = { id -> navController.navigateFrom(entry, categoryFormRoute(id)) },
            )
        }
        composable(CATEGORY_FORM, arguments = idArgument) { entry ->
            CategoryFormRoute(categoryId = entry.optionalId(), onDone = { navController.closeFrom(entry) })
        }
        composable(SAVINGS) { entry ->
            SavingsRoute(
                onAddPocket = { navController.navigateFrom(entry, pocketFormRoute()) },
                onOpenPocket = { id -> navController.navigateFrom(entry, pocketFormRoute(id)) },
            )
        }
        composable(POCKET_FORM, arguments = idArgument) { entry ->
            PocketFormRoute(pocketId = entry.optionalId(), onDone = { navController.closeFrom(entry) })
        }
        composable(SETTINGS) { entry ->
            SettingsRoute(
                onBack = { navController.closeFrom(entry) },
                onDisableLock = onDisableLock,
                onOpenImport = { navController.navigateFrom(entry, IMPORT) },
            )
        }
        composable(IMPORT) { entry ->
            ImportRoute(onDone = { navController.closeFrom(entry) })
        }
        composable(GOAL, arguments = monthArgument) { entry ->
            InvestmentGoalFormRoute(month = entry.month(), onDone = { navController.closeFrom(entry) })
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
