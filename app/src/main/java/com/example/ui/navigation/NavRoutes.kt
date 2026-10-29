package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Subscriptions : Screen("subscriptions")
    object Library : Screen("library")
    object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: String) = "player/$videoId"
    }
    object Channel : Screen("channel/{channelId}") {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
}
