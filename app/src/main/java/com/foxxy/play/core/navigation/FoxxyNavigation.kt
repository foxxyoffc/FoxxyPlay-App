package com.foxxy.play.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.foxxy.play.core.ui.theme.*
import com.foxxy.play.feature.home.HomeScreen
import com.foxxy.play.feature.developer.DeveloperPanelScreen
import com.foxxy.play.feature.library.LibraryScreen
import com.foxxy.play.feature.player.FoxxyMiniPlayer
import com.foxxy.play.feature.player.FoxxyPlayerViewModel
import com.foxxy.play.feature.player.NowPlayingScreen
import com.foxxy.play.feature.player.QueueScreen
import com.foxxy.play.feature.search.SearchScreen
import com.foxxy.play.feature.settings.SettingsScreen
import com.foxxy.play.feature.settings.AboutScreen
import com.foxxy.play.feature.storage.StorageScreen

@Composable
fun FoxxyNavigation() {
    val navController = rememberNavController()
    val playerViewModel: FoxxyPlayerViewModel = viewModel()

    val items = listOf(
        Triple(Screen.Home, Icons.Rounded.Home, "Home"),
        Triple(Screen.Search, Icons.Rounded.Search, "Search"),
        Triple(Screen.Library, Icons.Rounded.LibraryMusic, "Library"),
        Triple(Screen.Settings, Icons.Rounded.Settings, "Settings")
    )

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = FoxxyBackground,
        bottomBar = {
            if (
                currentRoute != "now_playing" &&
                currentRoute != "queue"
            ) {
                Column {
                    FoxxyMiniPlayer(playerViewModel) {
                        navController.navigate("now_playing") {
                            launchSingleTop = true
                        }
                    }

                    NavigationBar(
                        containerColor = FoxxyBackground,
                        contentColor = FoxxyText,
                        tonalElevation = 0.dp
                    ) {
                        items.forEach { (screen, icon, label) ->
                            val selected = currentRoute == screen.route

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(label)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FoxxyGoldLight,
                                    selectedTextColor = FoxxyGoldLight,
                                    indicatorColor = Color(0xFF332713),
                                    unselectedIconColor = FoxxyMuted,
                                    unselectedTextColor = FoxxyMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = playerViewModel,
                    onOpenOnlineTrack = { track ->
                        playerViewModel.playOnlineTrack(track)
                        navController.navigate("now_playing") {
                            launchSingleTop = true
                        }
                    },
                    onOpenDeveloperPanel = {
                        navController.navigate(Screen.Developer.route)
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = playerViewModel,
                    onOpenOnlineTrack = { track ->
                        playerViewModel.playOnlineTrack(track)
                        navController.navigate("now_playing") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = playerViewModel,
                    onOpenPlayer = {
                        navController.navigate("now_playing") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    playerViewModel = playerViewModel,
                    onOpenStorage = {
                        navController.navigate(Screen.Storage.route)
                    },
                    onOpenAbout = {
                        navController.navigate(Screen.About.route)
                    }
                )
            }

            composable(Screen.Developer.route) {
                DeveloperPanelScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Storage.route) {
                StorageScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("queue") {
                QueueScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("now_playing") {
                NowPlayingScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
