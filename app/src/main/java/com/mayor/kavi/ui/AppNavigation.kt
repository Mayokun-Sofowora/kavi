package com.mayor.kavi.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mayor.kavi.authentication.signin.SignInScreen
import com.mayor.kavi.authentication.signup.SignUpScreen
import com.mayor.kavi.ui.screens.LobbyScreen
import com.mayor.kavi.ui.screens.modes.*
import com.mayor.kavi.ui.screens.boards.*
import com.mayor.kavi.ui.screens.main.*
import com.mayor.kavi.ui.viewmodel.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val gameViewModel = hiltViewModel<GameViewModel>()
    val appViewModel = hiltViewModel<AppViewModel>()

    NavHost(
        navController = navController,
        startDestination = Routes.SignIn.route,
        modifier = Modifier.fillMaxSize()
    ) {
        // Authentication
        composable(Routes.SignIn.route) {
            SignInScreen(navController = navController)
        }
        composable(Routes.SignUp.route) {
            SignUpScreen(navController = navController)
        }

        // Main Menu
        composable(Routes.MainMenu.route) {
            MainMenuScreen(navController = navController)
        }

        // Start Screen
        composable(Routes.Start.route) {
            InterfaceModeScreen(
                viewModel = appViewModel,
                navController = navController
            )
        }

        // Board Games
        composable(Routes.Boards.route) {
            BoardsScreen(
                viewModel = gameViewModel,
                navController = navController
            )
        }

        // Play mode
        composable(Routes.PlayMode.route) {
            PlayModeScreen(
                navController = navController,
                gameViewModel = gameViewModel,
                appViewModel = appViewModel
            )
        }

        // Game Boards
        composable(Routes.BoardOne.route) {
            BoardOneScreen(
                viewModel = gameViewModel,
                navController = navController
            )
        }

        composable(Routes.BoardTwo.route) {
            BoardTwoScreen(
                viewModel = gameViewModel,
                navController = navController
            )
        }

        composable(
            route = Routes.MultiplayerBoard.route + "/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("sessionId")
                ?: return@composable
            MultiplayerBoardScreen(
                viewModel = gameViewModel,
                navController = navController
            )
        }

        composable(Routes.BoardThree.route) {
            BoardThreeScreen(
                viewModel = gameViewModel,
                navController = navController
            )
        }

        composable(Routes.BoardFour.route) {
            BoardFourScreen(
                viewModel = gameViewModel,
                navController = navController
            )
        }

        // Multiplayer Lobby
        composable(Routes.Lobby.route) {
            LobbyScreen(
                navController = navController,
                appViewModel = appViewModel,
                gameViewModel = gameViewModel
            )
        }

        // Settings
        composable(Routes.Settings.route) {
            SettingsScreen(
                viewModel = gameViewModel,
                navController = navController
            )
        }

        // Statistics
        composable(Routes.Statistics.route) {
            StatisticsScreen(
                appViewModel = appViewModel,
                gameViewModel = gameViewModel,
                navController = navController  // Changed from onBack
            )
        }

        // Instructions
        composable(Routes.Instructions.route) {
            InstructionsScreen(
                navController = navController,
                startPage = 0,
                showOnlyPage = false
            )
        }

        composable(
            route = Routes.InstructionsShort.route + "/{page}",
            arguments = listOf(navArgument("page") { type = NavType.IntType })
        ) { backStackEntry ->
            val page = backStackEntry.arguments?.getInt("page") ?: 1
            InstructionsScreen(
                navController = navController,
                startPage = page,
                showOnlyPage = true
            )
        }

        // Sign Out
        composable(Routes.SignOut.route) {
            SignInScreen(navController = navController)
        }
    }
}

sealed class Routes(val route: String) {
    object SignIn : Routes("signIn")
    object SignUp : Routes("signUp")
    object MainMenu : Routes("mainMenu")
    object Start : Routes("start")
    object ArScreen : Routes("arScreen")
    object Boards : Routes("classicBoards")
    object BoardOne : Routes("boardOne")
    object BoardTwo : Routes("boardTwo")
    object BoardThree : Routes("boardThree")
    object BoardFour : Routes("boardFour")
    object MultiplayerBoard : Routes("multiplayerBoard")
    object PlayMode : Routes("playMode")
    object Settings : Routes("settings")
    object Lobby : Routes("lobby")
    object Instructions : Routes("instructions/full")
    object InstructionsShort : Routes("instructions/short")
    object Statistics : Routes("statistics")
    object SignOut : Routes("signOut")
}
