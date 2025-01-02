package com.mayor.kavi.util

import androidx.navigation.NavController
import com.mayor.kavi.ui.Screen

// Auth Navigation
fun NavController.navigateToSignIn() {
    navigate(Screen.SignIn.route)
}

fun NavController.navigateToSignUp() {
    navigate(Screen.SignUp.route)
}


// Main Navigation
fun NavController.navigateToMainMenu() {
    navigate(Screen.MainMenu.route) {
        // Pop up to start destination to avoid building up a large stack
        popUpTo(Screen.MainMenu.route) { inclusive = true }
    }
}

fun NavController.navigateToSettings() {
    navigate(Screen.Settings.route)
}

fun NavController.navigateToStatistics() {
    navigate(Screen.Statistics.route)
}

fun NavController.navigateToLeaderboards() {
    navigate(Screen.Leaderboard.route)
}

fun NavController.navigateToInstructions(page: Int? = null) {
    if (page != null) {
        navigate(Screen.InstructionsShort.createRoute(page))
    } else {
        navigate(Screen.Instructions.route)
    }
}


// Game Navigation
fun NavController.navigateToStart() {
    navigate(Screen.Start.route)
}

fun NavController.navigateToBoards() {
    navigate(Screen.Boards.route)
}

fun NavController.navigateToBoard(board: Screen.Board) {
    navigate(board.route)
}

fun NavController.signOut() {
    navigate(Screen.SignIn.route) {
        // Clear all back stack
        popUpTo(0) { inclusive = true }
    }
}
