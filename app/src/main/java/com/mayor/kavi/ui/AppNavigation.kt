package com.mayor.kavi.ui

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mayor.kavi.authentication.signin.SignInScreen
import com.mayor.kavi.authentication.signup.SignUpScreen
import com.mayor.kavi.ui.screens.modes.*
import com.mayor.kavi.ui.screens.gameboards.*
import com.mayor.kavi.ui.screens.main.*
import com.mayor.kavi.ui.components.AnalyticsDashboard
import com.mayor.kavi.ui.screens.main.LeaderboardScreen
import com.mayor.kavi.ui.viewmodel.*

sealed class NavigationGraph(val route: String) {
    object Auth : NavigationGraph("auth_graph")
    object Main : NavigationGraph("main_graph")
    object Game : NavigationGraph("game_graph")
}

sealed class Screen(val route: String) {
    // Auth Screens
    object SignIn : Screen("signIn")
    object SignUp : Screen("signUp")

    // Main Screens
    object MainMenu : Screen("mainMenu")
    object Settings : Screen("settings")
    object Statistics : Screen("statistics")
    object Leaderboard : Screen("leaderboard")
    object Instructions : Screen("instructions/full")
    object InstructionsShort : Screen("instructions/short/{page}") {
        fun createRoute(page: Int) = "instructions/short/$page"
    }

    object Analytics : Screen("analytics")

    // Game Screens
    object Start : Screen("start")
    object Boards : Screen("classicBoards")

    // Game Board Screens
    sealed class Board(route: String) : Screen(route) {
        object One : Board("boardOne")
        object Two : Board("boardTwo")
        object Three : Board("boardThree")
        object Four : Board("boardFour")
    }
}

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val gameViewModel: GameViewModel = hiltViewModel()
    val leaderboardViewModel: LeaderboardViewModel = hiltViewModel()

    // Collect signed in state to update the navigation
    val signedInState by appViewModel.isUserSignedIn.collectAsState()
    val loginState by appViewModel.loginComplete.collectAsState()

    NavHost(
        navController = navController,
        startDestination = when {
            !loginState -> NavigationGraph.Auth.route
            signedInState -> NavigationGraph.Main.route
            else -> NavigationGraph.Auth.route
        }
    ) {
        // Auth Navigation Graph
        navigation(
            startDestination = Screen.SignIn.route,
            route = NavigationGraph.Auth.route
        ) {
            composable(Screen.SignIn.route) {
                SignInScreen(navController)
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(navController)
            }
        }

        // Main Navigation Graph
        navigation(
            startDestination = Screen.MainMenu.route,
            route = NavigationGraph.Main.route
        ) {
            composable(Screen.MainMenu.route) {
                MainMenuScreen(navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(gameViewModel, navController)
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen(appViewModel, gameViewModel, navController)
            }
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(leaderboardViewModel, navController)
            }
            composable(Screen.Instructions.route) {
                InstructionsScreen(navController, startPage = 0, showOnlyPage = false)
            }
            composable(
                route = Screen.InstructionsShort.route,
                arguments = listOf(navArgument("page") { type = NavType.IntType })
            ) { backStackEntry ->
                val page = backStackEntry.arguments?.getInt("page") ?: 1
                InstructionsScreen(navController, startPage = page, showOnlyPage = true)
            }
            composable(Screen.Analytics.route) {
                AnalyticsDashboard()
            }
        }

        // Game Navigation Graph
        navigation(
            startDestination = Screen.Start.route,
            route = NavigationGraph.Game.route
        ) {
            composable(Screen.Start.route) {
                InterfaceModeScreen(navController, appViewModel)
            }
            composable(Screen.Boards.route) {
                BoardsScreen(gameViewModel, navController)
            }

            // Game Boards
            composable(Screen.Board.One.route) {
                BoardOneScreen(gameViewModel, navController)
            }
            composable(Screen.Board.Two.route) {
                BoardTwoScreen(gameViewModel, navController)
            }
            composable(Screen.Board.Three.route) {
                BoardThreeScreen(gameViewModel, navController)
            }
            composable(Screen.Board.Four.route) {
                BoardFourScreen(gameViewModel, navController)
            }
        }
    }
}