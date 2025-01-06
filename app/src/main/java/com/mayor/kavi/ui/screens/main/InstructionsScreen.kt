package com.mayor.kavi.ui.screens.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavController
import com.mayor.kavi.R
import kotlinx.coroutines.launch

/**
 * Instructions screen providing game rules and gameplay guidance.
 *
 * Features:
 * - Horizontal paging for different game variants
 * - Visual instructions with animations
 * - Interactive examples
 * - Navigation controls
 * - Quick access to specific rules
 *
 * The screen provides comprehensive instructions for:
 * - Pig dice game rules
 * - Greed dice game rules
 * - Balut dice game rules
 * - Custom board rules
 * - Scoring systems
 * - Game controls
 * - Strategy tips
 *
 * @param navController Navigation controller for screen transitions
 * @param startPage Initial page to display (0-5)
 * @param showOnlyPage Whether to show only a single page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(
    navController: NavController,
    startPage: Int = 0,
    showOnlyPage: Boolean = false
) {
    val pagerState =
        rememberPagerState(
            initialPage = startPage,
            initialPageOffsetFraction = 0.0f,
            pageCount = { 6 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Kavi Logo",
                        modifier = Modifier.height(200.dp)
                    )
                },
                actions = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close Instructions",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorResource(id = R.color.primary_container).copy(alpha = 0.7f),
                            colorResource(id = R.color.primary).copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            if (showOnlyPage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    InstructionPage(
                        title = when (startPage) {
                            4 -> "Game Rules"
                            else -> "Game Modes"
                        },
                        description = when (startPage) {
                            4 -> "How to play the boards:\n" +
                                    "• Pig Dice Game: Roll a single die. If you roll a 1, you lose all points for the turn. " +
                                    "Otherwise, add the die's value to the turn score. Keep rolling to build your score or bank your points. " +
                                    "First player to reach 100 points wins." +
                                    "\n\n" +
                                    "• Greed (10,000): Score points by rolling combinations:\n" +
                                    "  - Single 1: 100 points\n" +
                                    "  - Single 5: 50 points\n" +
                                    "  - Three of a Kind: Number × 100 (e.g., three 2s = 200)\n" +
                                    "  - Three 1s: 1000 points\n" +
                                    "  - Five of a Kind: 2000 points\n" +
                                    "  - Six of a Kind: 3000 points\n" +
                                    "  - Straight (1-2-3-4-5-6): 1000 points\n" +
                                    "  - Three Pairs: 1500 points\n" +
                                    "First to 10,000 points wins." +
                                    "\n\n" +
                                    "• Balut Dice Game: Use five dice with these scoring categories:\n" +
                                    "  - Ones to Sixes: Sum of respective numbers\n" +
                                    "  - Full House: Sum of all dice (three of one number, two of another)\n" +
                                    "  - Four of a Kind: Sum of all five dice\n" +
                                    "  - Five of a Kind: Sum of all dice + 100 bonus\n" +
                                    "  - Small Straight: 30 points (1-2-3-4, 2-3-4-5, or 3-4-5-6)\n" +
                                    "  - Large Straight: 40 points (1-2-3-4-5 or 2-3-4-5-6)\n" +
                                    "  - Choice: Sum of all dice\n" +
                                    "You get 3 rolls per turn. After each roll, you can hold dice you want to keep. " +
                                    "Each category can only be used once. Highest total score wins." +
                                    "\n\n" +
                                    "• Custom Dice Game: A flexible board for any dice game:\n" +
                                    "  - Adjustable number of dice\n" +
                                    "  - Add/remove players\n" +
                                    "  - Custom player names\n" +
                                    "  - Score tracking with notes\n" +
                                    "  - View score history"

                            else -> "Kavi offers two exciting play modes:\n\n" +
                                    "• AR Mode: An immersive augmented reality experience where you interact with virtual dice in a dynamic environment.\n\n" +
                                    "• Classic Mode: A traditional board-style game focusing on pure strategic dice rolling and scoring."
                        },
                        imageRes = if (startPage == 1) R.drawable.instr_img1 else null
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            InstructionPage(
                                title = when (page) {
                                    0 -> "Game Overview"
                                    1 -> "Game Modes"
                                    2 -> "Playing the Game"
                                    3 -> "How to Win"
                                    4 -> "Scoring"
                                    5 -> "Strategic Tips"
                                    else -> "More Info"
                                },
                                description = when (page) {
                                    0 -> "Kavi is an exciting dice game that combines strategic gameplay with immersive augmented reality (AR) and classic board game modes. " +
                                            "Compete to outscore your opponents or challenge the AI by rolling dice, making calculated decisions, and mastering unique scoring categories."

                                    1 -> "Kavi offers two distinct play modes:\n" +
                                            "• Virtual Mode: Interact with virtual dice in a visually immersive AR environment. " +
                                            "You can roll AR dice or use the dice recognition feature to input results from physical dice.\n" +
                                            "• Classic Mode: Play the classic dice games, focusing on strategy and dice rolling. " +
                                            "Choose from various scoring boards for unique gameplay experiences."

                                    2 -> "Game Flow:\n" +
                                            "• Turn-based: Players alternate turns, rolling dice and deciding which dice to keep or re-roll.\n" +
                                            "• Strategic decisions: Fill scoring categories wisely; once filled, they cannot be used again.\n" +
                                            "• Modes: Depending on the selected mode, roll the classic dice, recognize dice images, or use virtual dice."

                                    3 -> "Winning Conditions:\n" +
                                            "• Highest Score: At the end of all rounds, the player with the highest total score wins.\n" +
                                            "• Bonus Victory: Some boards like pig allow early wins through bonuses or special conditions."

                                    4 -> "How to play the boards:\n" +
                                            "• Pig Dice Game: Roll a single die. If you roll a 1, you lose all points for the turn. " +
                                            "Otherwise, add the die's value to the turn score. Keep rolling to build your score or bank your points. " +
                                            "First player to reach 100 points wins." +
                                            "\n\n" +
                                            "• Greed (10,000): Score points by rolling combinations:\n" +
                                            "  - Single 1: 100 points\n" +
                                            "  - Single 5: 50 points\n" +
                                            "  - Three of a Kind: Number × 100 (e.g., three 2s = 200)\n" +
                                            "  - Three 1s: 1000 points\n" +
                                            "  - Five of a Kind: 2000 points\n" +
                                            "  - Six of a Kind: 3000 points\n" +
                                            "  - Straight (1-2-3-4-5-6): 1000 points\n" +
                                            "  - Three Pairs: 1000 points\n" +
                                            "First to 10,000 points wins." +
                                            "\n\n" +
                                            "• Balut Dice Game: Use five dice with these scoring categories:\n" +
                                            "  - Ones to Sixes: Sum of respective numbers\n" +
                                            "  - Full House: Sum of all dice (three of one number, two of another)\n" +
                                            "  - Four of a Kind: Sum of all five dice\n" +
                                            "  - Five of a Kind: Sum of all dice + 100 bonus\n" +
                                            "  - Small Straight: 30 points (1-2-3-4, 2-3-4-5, or 3-4-5-6)\n" +
                                            "  - Large Straight: 40 points (1-2-3-4-5 or 2-3-4-5-6)\n" +
                                            "  - Choice: Sum of all dice\n" +
                                            "You get 3 rolls per turn. After each roll, you can hold dice you want to keep. " +
                                            "Each category can only be used once. Highest total score wins." +
                                            "\n\n" +
                                            "• Custom Dice Game: A flexible board for any dice game:\n" +
                                            "  - Adjustable number of dice\n" +
                                            "  - Add/remove players\n" +
                                            "  - Custom player names\n" +
                                            "  - Score tracking with notes\n" +
                                            "  - View score history"

                                    5 -> "Master Kavi with these tips:\n" +
                                            "• Balance Risk and Reward: Know when to re-roll or settle.\n" +
                                            "• Adapt to Modes: Use AR for an interactive experience or classic for traditional strategy.\n" +
                                            "• Plan Ahead: Keep an eye on available categories.\n" +
                                            "•  Learn the Boards: Each board type offers unique scoring rules and strategies. " +
                                            "In games like Greed and Balut, you can lock the dice you don't want to reroll by simply tapping on them."

                                    else -> "Kavi combines luck and strategy. Master the nuances, challenge your friends, and become the ultimate Kavi champion!"
                                },
                                imageRes = if (page == 1) R.drawable.instr_img1 else null
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SmoothPageIndicator(pagerState = pagerState, count = 6)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(48.dp),
                                    tint = if (pagerState.currentPage > 0) Color.White else Color.White.copy(
                                        alpha = 0.5f
                                    )
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                enabled = pagerState.currentPage < 5
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(48.dp),
                                    tint = if (pagerState.currentPage < 5) Color.White else Color.White.copy(
                                        alpha = 0.5f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionPage(title: String, description: String, imageRes: Int?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                shadow = Shadow(
                    color = Color.Gray,
                    offset = Offset(1f, 1f),
                    blurRadius = 3f
                )
            ),
            color = Color.hsl(232f, 0.3f, 0.3f),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(26.dp))

        imageRes?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                lineHeight = 24.sp
            ),
            color = Color(0xFFFFFFFF),
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SmoothPageIndicator(pagerState: PagerState, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val color = if (pagerState.currentPage == index) Color.White else Color.LightGray
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(8.dp)
                    .background(color, shape = CircleShape)
            )
        }
    }
}
