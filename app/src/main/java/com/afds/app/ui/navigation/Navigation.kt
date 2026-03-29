package com.afds.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.afds.app.AFDSApplication
import com.afds.app.ui.screens.*

object Routes {
    const val LOGIN = "login"
    const val GOOGLE_LOGIN = "google_login"
    const val SETUP = "setup"
    const val TELEGRAM_SETUP = "telegram_setup"
    const val HOME = "home"
    const val SEARCH = "search?query={query}&category={category}"
    const val BROWSE = "browse/{category}"
    const val PROFILE = "profile"
    const val MY_FILES = "my_files"

    fun search(query: String, category: String): String {
        val encoded = Uri.encode(query)
        return "search?query=$encoded&category=$category"
    }

    fun browse(category: String) = "browse/$category"
}

@Composable
fun AFDSNavHost(navController: NavHostController) {
    val sessionManager = AFDSApplication.instance.sessionManager
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)

    val startDestination = when {
        !isLoggedIn -> Routes.LOGIN
        else -> Routes.HOME
    }

    // Reactively navigate to LOGIN whenever session expires mid-session
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute != Routes.LOGIN) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onSetupNeeded = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoogleLogin = {
                    navController.navigate(Routes.GOOGLE_LOGIN)
                }
            )
        }

        composable(Routes.GOOGLE_LOGIN) {
            GoogleLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRefreshProfile = { /* handled inside SetupScreen */ },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAutoSetup = {
                    navController.navigate(Routes.TELEGRAM_SETUP)
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onSearch = { query, category ->
                    navController.navigate(Routes.search(query, category))
                },
                onBrowse = { category ->
                    navController.navigate(Routes.browse(category))
                },
                onProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onMyFiles = {
                    navController.navigate(Routes.MY_FILES)
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.SEARCH,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = "files"
                }
            )
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            val category = backStackEntry.arguments?.getString("category") ?: "files"
            SearchScreen(
                query = query,
                category = category,
                onBack = { navController.popBackStack() },
                onNewSearch = { newQuery, newCategory ->
                    navController.navigate(Routes.search(newQuery, newCategory)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.BROWSE,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "files"
            BrowseScreen(
                category = category,
                onBack = { navController.popBackStack() },
                onSearch = { query, cat ->
                    navController.navigate(Routes.search(query, cat))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAutoSetup = {
                    navController.navigate(Routes.TELEGRAM_SETUP)
                }
            )
        }

        composable(Routes.MY_FILES) {
            MyFilesScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TELEGRAM_SETUP) {
            TelegramSetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}