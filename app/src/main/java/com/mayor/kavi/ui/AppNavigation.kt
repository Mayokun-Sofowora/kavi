package com.mayor.kavi.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.mayor.kavi.authentication.signin.SignInScreen
import com.mayor.kavi.authentication.signup.SignUpScreen
import com.mayor.kavi.ui.screens.*
import com.mayor.kavi.ui.screens.boards.BoardFiveScreen
import com.mayor.kavi.ui.screens.boards.BoardFourScreen
import com.mayor.kavi.ui.screens.boards.BoardOneScreen
import com.mayor.kavi.ui.screens.boards.BoardThreeScreen
import com.mayor.kavi.ui.screens.boards.BoardTwoScreen
import com.mayor.kavi.ui.viewmodel.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val diceViewModel = hiltViewModel<DiceViewModel>()
    val appViewModel = hiltViewModel<AppViewModel>()

    NavHost(
        navController = navController,
        startDestination = Routes.SignIn.route,
        modifier = Modifier.fillMaxSize()
    ) {
        // Authentication
        composable(Routes.SignIn.route) { SignInScreen(navController = navController) }
        composable(Routes.SignUp.route) { SignUpScreen(navController = navController) }
        // Main Menu
        composable(Routes.MainMenu.route) { MainMenuScreen(navController = navController) }
        // Start Screen
        composable(Routes.Start.route) {
            InterfaceModeScreen(viewModel = appViewModel, navController = navController)
        }
        // AR Screen
        composable(Routes.ArScreen.route) {}
        // Game Selection Screen
        composable(Routes.PlayMode.route) {
            PlayModeScreen(
                navController = navController,
                diceViewModel = diceViewModel,
                appViewModel = appViewModel
            )
        }
        // Board Games
        composable(Routes.Boards.route) {
            BoardsScreen(viewModel = diceViewModel, navController = navController)
        }
        // Game Boards
        composable("boardOne") {
            BoardOneScreen(viewModel = diceViewModel, navController = navController)
        }
        composable("boardTwo") {
            BoardTwoScreen(viewModel = diceViewModel, navController = navController)
        }
        composable("boardThree") {
            BoardThreeScreen(viewModel = diceViewModel, navController = navController)
        }
        composable("boardFour") {
            BoardFourScreen(viewModel = diceViewModel, navController = navController)
        }
        composable("boardFive") {
            BoardFiveScreen(viewModel = diceViewModel, navController = navController)
        }

        // Settings
        composable(Routes.Settings.route) {}
        // Instructions
        composable(Routes.Instructions.route) {
            InstructionsScreen(onClose = { navController.popBackStack() })
        }
        composable(Routes.InstructionsShort.route) {
            InstructionsScreen(
                onClose = { navController.popBackStack() },
                startPage = 1, showOnlyPage = true
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
    object PlayMode : Routes("playMode")
    object Settings : Routes("settings")
    object Instructions : Routes("instructions/full")
    object InstructionsShort : Routes("instructions/short")
    object Statistics : Routes("statistics")
    object Achievements : Routes("achievements")
    object SignOut : Routes("signOut")
}