package com.ngi.streamnest.admin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ngi.streamnest.admin.AdminViewModel
import com.ngi.streamnest.admin.ui.channels.ChannelEditScreen
import com.ngi.streamnest.admin.ui.channels.ChannelsScreen
import com.ngi.streamnest.admin.ui.dashboard.DashboardScreen
import com.ngi.streamnest.admin.ui.events.EventEditScreen
import com.ngi.streamnest.admin.ui.events.EventsScreen
import com.ngi.streamnest.admin.ui.featured.FeaturedEditScreen
import com.ngi.streamnest.admin.ui.featured.FeaturedScreen
import com.ngi.streamnest.admin.ui.login.LoginScreen
import com.ngi.streamnest.admin.ui.settings.ApiSettingsScreen

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Tab("dashboard", "ড্যাশবোর্ড", Icons.Filled.Dashboard)
    data object Channels : Tab("channels", "চ্যানেল", Icons.Filled.LiveTv)
    data object Featured : Tab("featured", "ফিচার্ড", Icons.Filled.Star)
    data object Events : Tab("events", "ইভেন্ট", Icons.Filled.SportsSoccer)
    data object Settings : Tab("settings", "API", Icons.Filled.Settings)
}

private val tabs = listOf(Tab.Dashboard, Tab.Channels, Tab.Featured, Tab.Events, Tab.Settings)

private val idArg = listOf(navArgument("id") { type = NavType.StringType })

@Composable
fun AdminApp(vm: AdminViewModel = viewModel()) {
    val session by vm.session.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    if (session == null) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            LoginScreen(
                busy = busy,
                onSignIn = { email, password -> vm.signIn(email, password) },
                modifier = Modifier.padding(padding),
            )
        }
    } else {
        AdminMain(vm = vm, snackbar = snackbar)
    }
}

@Composable
private fun AdminMain(vm: AdminViewModel, snackbar: SnackbarHostState) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.Dashboard.route) {
                DashboardScreen(vm = vm, onNavigate = { nav.navigate(it) })
            }
            composable(Tab.Channels.route) {
                ChannelsScreen(
                    vm = vm,
                    onOpen = { nav.navigate("channel/$it") },
                    onAdd = { nav.navigate("channel/new") },
                )
            }
            composable("channel/{id}", arguments = idArg) { entry ->
                ChannelEditScreen(
                    vm = vm,
                    id = entry.arguments?.getString("id") ?: "new",
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Tab.Featured.route) {
                FeaturedScreen(
                    vm = vm,
                    onOpen = { nav.navigate("featured/$it") },
                    onAdd = { nav.navigate("featured/new") },
                )
            }
            composable("featured/{id}", arguments = idArg) { entry ->
                FeaturedEditScreen(
                    vm = vm,
                    id = entry.arguments?.getString("id") ?: "new",
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Tab.Events.route) {
                EventsScreen(
                    vm = vm,
                    onOpen = { nav.navigate("event/$it") },
                    onAdd = { nav.navigate("event/new") },
                )
            }
            composable("event/{id}", arguments = idArg) { entry ->
                EventEditScreen(
                    vm = vm,
                    id = entry.arguments?.getString("id") ?: "new",
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Tab.Settings.route) {
                ApiSettingsScreen(vm = vm)
            }
        }
    }
}
