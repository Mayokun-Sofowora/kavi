package com.mayor.kavi

import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.mayor.kavi.ui.screens.*
import com.mayor.kavi.ui.theme.KaviTheme
import com.mayor.kavi.ui.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enableEdgeToEdge()
        }
        setContent {
            KaviTheme {
                val userViewModel: UserViewModel = hiltViewModel()
                AppNavigation(userViewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(userViewModel: UserViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "mainMenu", modifier = modifier) {
        // Define routes using "route" parameter for each screen
        composable(route = "mainMenu") {
            MainMenu(navController, userViewModel)
        }
        composable(route = "play") {
            val playViewModel: PlayViewModel = hiltViewModel()
            PlayScreen(navController, playViewModel)
        }
        composable(route = "settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(navController, settingsViewModel)
        }
        composable(route = "instructions/singlePage") {
            InstructionsScreen(
                startPage = 1, showOnlyPage = true,
                onClose = { navController.popBackStack() }
            )
        }
        composable(route = "instructions/full") {
            InstructionsScreen(
                showOnlyPage = false, onClose = { navController.popBackStack() },
                startPage = 0
            )
        }
        composable(route = "statistics/{playerId}") { backStackEntry ->
            val playerId = backStackEntry.arguments?.getString("playerId") ?: "defaultPlayerId"
            val statsViewModel: StatsViewModel = hiltViewModel()
            StatsScreen(navController, playerId = playerId, statsViewModel)
        }
        composable(route = "classicMode") {
            val userViewModel: UserViewModel = hiltViewModel()
            ClassicMode(navController, userViewModel)
        }
        composable(route = "arMode") {
            VirtualMode(navController)
        }
        composable(route = "leaderboard") {
            val leaderboardViewModel: LeaderboardViewModel =
                hiltViewModel()
            LeaderboardScreen(navController, leaderboardViewModel)
        }
        composable(route = "gameScene") {
            val userViewModel: UserViewModel = hiltViewModel()
            GameScene(navController, userViewModel)
        }
//        composable(route = "gameOver") {
//            val gameOverViewModel: GameOverViewModel =
//                hiltViewModel()
//            GameOverScreen(navController, gameOverViewModel)
//        }
//        composable(route = "profile") {
//            val profileViewModel: ProfileViewModel =
//                hiltViewModel()
//            ProfileScreen(navController, profileViewModel)
//        }
//        composable(route = "multiplayer") {
//            val multiplayerViewModel: MultiplayerViewModel =
//                hiltViewModel()
//            MultiplayerMode(navController, multiplayerViewModel)
//        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun AppPreview() {
//    KaviTheme {
//    val userViewModel = hiltViewModel<UserViewModel>() // For Preview, can mock
//        AppNavigation(userViewModel)
//    }
//}
