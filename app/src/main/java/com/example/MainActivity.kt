package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.channel.ChannelDetailScreen
import com.example.ui.screens.channel.ChannelViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.library.LibraryViewModel
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.player.PlayerViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchViewModel
import com.example.ui.theme.NewPlayerTheme

class MainActivity : ComponentActivity() {

    private val isInPiPModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NewPlayerTheme {
                // Arabic RTL Layout direction provider
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val app = application as NewPlayerApplication
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val inPiP = isInPiPModeState.value

                    val isTopLevelRoute = currentRoute in listOf(
                        Screen.Home.route,
                        Screen.Subscriptions.route,
                        Screen.Library.route
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (!inPiP && isTopLevelRoute) {
                                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                                    // 1. Home
                                    NavigationBarItem(
                                        selected = (currentRoute == Screen.Home.route),
                                        onClick = {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentRoute == Screen.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = stringResource(R.string.nav_home)
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_home)) },
                                        modifier = Modifier.testTag("nav_home_button")
                                    )

                                    // 2. Subscriptions
                                    NavigationBarItem(
                                        selected = (currentRoute == Screen.Subscriptions.route),
                                        onClick = {
                                            navController.navigate(Screen.Subscriptions.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentRoute == Screen.Subscriptions.route) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
                                                contentDescription = stringResource(R.string.nav_subscriptions)
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_subscriptions)) },
                                        modifier = Modifier.testTag("nav_subscriptions_button")
                                    )

                                    // 3. Library
                                    NavigationBarItem(
                                        selected = (currentRoute == Screen.Library.route),
                                        onClick = {
                                            navController.navigate(Screen.Library.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentRoute == Screen.Library.route) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                                contentDescription = stringResource(R.string.nav_library)
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_library)) },
                                        modifier = Modifier.testTag("nav_library_button")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (inPiP) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route
                            ) {
                                // Home Screen
                                composable(Screen.Home.route) {
                                    val homeViewModel: HomeViewModel = viewModel {
                                        HomeViewModel(app.videoRepository)
                                    }
                                    HomeScreen(
                                        viewModel = homeViewModel,
                                        authManager = app.authManager,
                                        onVideoClick = { videoId ->
                                            navController.navigate(Screen.Player.createRoute(videoId))
                                        },
                                        onSearchClick = {
                                            navController.navigate(Screen.Search.route)
                                        },
                                        onChannelClick = { channelId ->
                                            navController.navigate(Screen.Channel.createRoute(channelId))
                                        }
                                    )
                                }

                                // Search Screen
                                composable(Screen.Search.route) {
                                    val searchViewModel: SearchViewModel = viewModel {
                                        SearchViewModel(app.videoRepository)
                                    }
                                    SearchScreen(
                                        viewModel = searchViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onVideoClick = { videoId ->
                                            navController.navigate(Screen.Player.createRoute(videoId))
                                        }
                                    )
                                }

                                // Subscriptions Screen
                                composable(Screen.Subscriptions.route) {
                                    val subscriptionsViewModel = com.example.ui.screens.subscriptions.SubscriptionsViewModel(
                                        app.subscriptionRepository,
                                        app.videoRepository
                                    )
                                    com.example.ui.screens.subscriptions.SubscriptionsScreen(
                                        viewModel = subscriptionsViewModel,
                                        onVideoClick = { videoId ->
                                            navController.navigate(Screen.Player.createRoute(videoId))
                                        },
                                        onChannelClick = { channelId ->
                                            navController.navigate(Screen.Channel.createRoute(channelId))
                                        }
                                    )
                                }

                                // Library Screen
                                composable(Screen.Library.route) {
                                    val libraryViewModel = LibraryViewModel(
                                        app.historyRepository,
                                        app.downloadRepository,
                                        app.playlistRepository,
                                        app.downloadManagerHelper,
                                        app.playerManager
                                    )
                                    LibraryScreen(
                                        viewModel = libraryViewModel,
                                        authManager = app.authManager,
                                        onVideoClick = { videoId ->
                                            navController.navigate(Screen.Player.createRoute(videoId))
                                        }
                                    )
                                }

                                // Player Screen
                                composable(
                                    route = Screen.Player.route,
                                    arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                                    val playerViewModel = PlayerViewModel(
                                        videoId = videoId,
                                        videoRepository = app.videoRepository,
                                        historyRepository = app.historyRepository,
                                        subscriptionRepository = app.subscriptionRepository,
                                        playlistRepository = app.playlistRepository,
                                        playerManager = app.playerManager,
                                        downloadManagerHelper = app.downloadManagerHelper
                                    )
                                    PlayerScreen(
                                        viewModel = playerViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onVideoClick = { nextId ->
                                            navController.navigate(Screen.Player.createRoute(nextId)) {
                                                popUpTo(Screen.Player.route) { inclusive = true }
                                            }
                                        },
                                        onChannelClick = { channelId ->
                                            navController.navigate(Screen.Channel.createRoute(channelId))
                                        },
                                        onEnterPiP = { enterPictureInPicture() },
                                        isInPiPMode = inPiP
                                    )
                                }

                                // Channel Detail Screen
                                composable(
                                    route = Screen.Channel.route,
                                    arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                                    val channelViewModel = ChannelViewModel(
                                        channelId = channelId,
                                        videoRepository = app.videoRepository,
                                        subscriptionRepository = app.subscriptionRepository
                                    )
                                    ChannelDetailScreen(
                                        viewModel = channelViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onVideoClick = { videoId ->
                                            navController.navigate(Screen.Player.createRoute(videoId))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPiPModeState.value = isInPictureInPictureMode
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(params)
        }
    }
}
