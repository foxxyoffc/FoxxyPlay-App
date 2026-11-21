package com.foxxy.play.core.navigation

sealed class Screen(
    val route: String,
    val label: String
) {
    data object Home : Screen("home", "Home")
    data object Search : Screen("search", "Search")
    data object Library : Screen("library", "Library")
    data object Settings : Screen("settings", "Settings")
    data object Developer : Screen("developer", "Developer")
    data object Storage : Screen("storage", "Storage")
    data object About : Screen("about", "About")
}
