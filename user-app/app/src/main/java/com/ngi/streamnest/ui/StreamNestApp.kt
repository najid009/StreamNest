package com.ngi.streamnest.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ngi.streamnest.MainViewModel
import com.ngi.streamnest.data.Channel
import com.ngi.streamnest.data.FeaturedItem
import com.ngi.streamnest.data.MatchItem
import com.ngi.streamnest.data.channelById
import com.ngi.streamnest.ui.content.ContentScreen
import com.ngi.streamnest.ui.home.HomeScreen
import com.ngi.streamnest.ui.player.PlayerScreen
import com.ngi.streamnest.ui.settings.SettingsScreen
import com.ngi.streamnest.ui.sports.SportsScreen
import kotlinx.coroutines.launch

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Dest("home", "হোম", Icons.Filled.Home)
    data object Sports : Dest("sports", "স্পোর্টস", Icons.Filled.SportsSoccer)
    data object Content : Dest("content", "কনটেন্ট", Icons.Filled.VideoLibrary)
    data object Settings : Dest("settings", "সেটিংস", Icons.Filled.Settings)
}

private val bottomTabs = listOf(Dest.Home, Dest.Sports, Dest.Content, Dest.Settings)

private const val PLAYER_ROUTE = "player/{url}/{title}"

private fun playerRoute(url: String, title: String) =
    "player/${Uri.encode(url)}/${Uri.encode(title)}"

@Composable
fun StreamNestApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    fun showMessage(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun playChannel(channel: Channel) {
        nav.navigate(playerRoute(channel.streamUrl, channel.name))
    }

    fun openEvent(match: MatchItem) {
        val channel = state.data?.channelById(match.channelId)
        if (channel == null) {
            showMessage("এই ইভেন্টের চ্যানেল এখনো যুক্ত করা হয়নি")
        } else {
            playChannel(channel)
        }
    }

    fun openExternal(url: String) {
        try {
            // বিজ্ঞাপন/বাইরের লিংক Chrome Custom Tab-এ খুলবে (অ্যাপের ভেতরেই, native অনুভূতি)
            CustomTabsIntent.Builder().setShowTitle(true).build()
                .launchUrl(context, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            showMessage("লিংক খোলা যাচ্ছে না")
        }
    }

    fun openFeatured(item: FeaturedItem) {
        val data = state.data ?: return
        when (item.actionType) {
            "url" -> openExternal(item.actionValue)
            "channel" -> {
                val channel = data.channelById(item.actionValue)
                if (channel != null) playChannel(channel) else showMessage("চ্যানেল পাওয়া যায়নি")
            }
            "match" -> {
                val match = data.matches.firstOrNull { it.id == item.actionValue }
                if (match != null) openEvent(match) else showMessage("ইভেন্ট পাওয়া যায়নি")
            }
            else -> showMessage("এই আইটেমটি খোলা যাবে না")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    state = state,
                    onRefresh = { vm.refresh() },
                    onFeatured = ::openFeatured,
                    onChannel = ::playChannel,
                    onMatch = ::openEvent,
                )
            }
            composable(Dest.Sports.route) {
                SportsScreen(
                    state = state,
                    onRefresh = { vm.refresh() },
                    onMatch = ::openEvent,
                    onChannel = ::playChannel,
                )
            }
            composable(Dest.Content.route) {
                ContentScreen()
            }
            composable(Dest.Settings.route) {
                SettingsScreen(state = state, onRefresh = { vm.refresh() })
            }
            composable(
                route = PLAYER_ROUTE,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { entry ->
                PlayerScreen(
                    url = entry.arguments?.getString("url").orEmpty(),
                    title = entry.arguments?.getString("title").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
