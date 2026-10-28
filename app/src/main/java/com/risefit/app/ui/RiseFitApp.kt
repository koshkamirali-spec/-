package com.risefit.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.risefit.app.MainViewModel
import com.risefit.app.PlusViewModel
import com.risefit.app.ui.screens.AiScreen
import com.risefit.app.ui.screens.AlarmsScreen
import com.risefit.app.ui.screens.CalendarScreen
import com.risefit.app.ui.screens.FocusScreen
import com.risefit.app.ui.screens.GoalScreen
import com.risefit.app.ui.screens.HabitsScreen
import com.risefit.app.ui.screens.HomeScreen
import com.risefit.app.ui.screens.JournalScreen
import com.risefit.app.ui.screens.MyGoalsScreen
import com.risefit.app.ui.screens.PlannerScreen
import com.risefit.app.ui.screens.ProgressScreen
import com.risefit.app.ui.screens.RemindersScreen
import com.risefit.app.ui.screens.ScheduleScreen
import com.risefit.app.ui.screens.SettingsScreen
import com.risefit.app.ui.screens.WorkoutScreen
import com.risefit.app.ui.theme.RF

private class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("home", "خانه", Icons.Rounded.Home),
    TabItem("alarms", "آلارم", Icons.Rounded.Alarm),
    TabItem("workout", "تمرین", Icons.Rounded.FitnessCenter),
    TabItem("goal", "هدف", Icons.Rounded.Flag),
    TabItem("calendar", "تقویم", Icons.Rounded.CalendarMonth)
)

@Composable
fun RiseFitApp(vm: MainViewModel, plus: PlusViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val ready by vm.ready.collectAsState()

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        plus.messages.collect { snackbar.showSnackbar(it) }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }

    fun go(target: String) {
        nav.navigate(target) {
            if (tabs.any { it.route == target }) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                restoreState = true
            }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = RF.Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = RF.Surface) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { go(t.route) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (ready) {
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") { HomeScreen(vm, onNavigate = { go(it) }) }
                    composable("alarms") { AlarmsScreen(vm) }
                    composable("workout") { WorkoutScreen(vm) }
                    composable("goal") { GoalScreen(vm) }
                    composable("calendar") { CalendarScreen(vm) }
                    composable("schedule") { ScheduleScreen(vm, onBack = { nav.popBackStack() }) }
                    composable("progress") { ProgressScreen(vm, onBack = { nav.popBackStack() }) }
                    composable("settings") { SettingsScreen(vm, plus, onBack = { nav.popBackStack() }) }
                    composable("planner") { PlannerScreen(plus, onBack = { nav.popBackStack() }) }
                    composable("reminders") { RemindersScreen(plus, onBack = { nav.popBackStack() }) }
                    composable("habits") { HabitsScreen(plus, onBack = { nav.popBackStack() }) }
                    composable("mygoals") { MyGoalsScreen(plus, onBack = { nav.popBackStack() }) }
                    composable("focus") { FocusScreen(plus, onBack = { nav.popBackStack() }) }
                    composable("journal") { JournalScreen(plus, onBack = { nav.popBackStack() }) }
                    composable("ai") { AiScreen(plus, onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}
