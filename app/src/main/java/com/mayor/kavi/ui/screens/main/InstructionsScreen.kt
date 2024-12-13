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
import com.mayor.kavi.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(onClose: () -> Unit, startPage: Int = 0, showOnlyPage: Boolean = false) {
    val pagerState =
        rememberPagerState(initialPage = startPage, initialPageOffsetFraction = 0.0f) { 6 }
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
                    IconButton(onClick = onClose) {
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
                InstructionPage(
                    title = "Game Modes",
                    description = "Kavi offers two exciting play modes:\n\n" +
                            "• AR Mode: An immersive augmented reality experience where you interact with virtual dice in a dynamic environment.\n\n" +
                            "• Classic Mode: A traditional board-style game focusing on pure strategic dice rolling and scoring.",
                    imageRes = R.drawable.instr_img1
                )
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
                                    0 -> "Kavi is a dynamic dice game offering strategic gameplay in both augmented reality (AR) and classic board game modes. " +
                                            "Compete to outscore your opponents or the AI by rolling dice, making calculated decisions, and mastering scoring categories."

                                    1 -> "Kavi offers two distinct play modes:\n" +
                                            "• AR Mode: Interact with virtual dice in a visually immersive AR environment. You can roll AR dice or use the dice recognition feature" +
                                            " to input results from physical dice.\n" +
                                            "• Classic Mode: Play the classic dice games, focusing on strategy and dice rolling. " +
                                            "Choose from various scoring boards for unique gameplay experiences."

                                    2 -> "Game Flow:\n" +
                                            "• Turn-based: Players alternate turns, rolling dice and deciding which dice to keep or re-roll.\n" +
                                            "• Strategic decisions: Fill scoring categories wisely; once filled, they cannot be used again.\n" +
                                            "• Modes: Depending on the selected mode, roll the classic dice, recognize dice images, or use virtual dice."

                                    3 -> "Winning Conditions:\n" +
                                            "• Highest Score: At the end of all rounds, the player with the highest total score wins.\n" +
                                            "• Bonus Victory: Some boards like pig allow early wins through bonuses or special conditions."

                                    4 -> "Choose from a variety of unique boards:\n" +
                                            "• [Pig Dice Game] (Two or more players):  A fast-paced, strategic game where the goal is to be the first to reach 100 points. " +
                                            "Players roll a single die to determine who goes first, with the highest roll starting. " +
                                            "The starting player then rolls again, adding to their score as long as they avoid rolling a 1 or choose to end their turn to bank their points. " +
                                            "Rolling a 1 ends the turn with no points earned for that round. " +
                                            "The game concludes when a player reaches 100 points, declaring them the winner.\n\n" +
                                            "• [Greed (10,000)](Two or more players): A popular game where the goal is to accumulate 10,000 points by rolling combinations for high scores." +
                                            "All Players roll their dice, and the player with the highest score starts the game. A player's turn starts by rolling all six dice. " +
                                            "After each roll, players may set aside dice that form scoring combinations. At least one scoring die must be set aside per roll; " +
                                            "otherwise, the turn ends, and no points are scored for that round. Players may re-roll remaining dice or stop their turn and bank their score. " +
                                            "Scoring combinations include Single 1 (100 points), Single 5 (50 points), and various multiples of a kind.\n\n" +
                                            "• [Mexico Dice Game] (Two or more players): A bluffing game where players aim to score the most points by taking risks. Each player starts with 6 lives. " +
                                            "Players roll dice and set a score for each round. They can opt to challenge opponents’ rolls or bluff, risking lives in the process.\n\n" +
                                            "• [Chicago Dice Game] (Two or more players): Players score points based on round-specific targets, making it a strategic and competitive game where each round has a unique challenge. Players roll dice, set aside combinations that meet the round's target, and can re-roll the remaining dice to complete their goal. The game is played over multiple rounds with different targets each round, such as three of a kind, full house, or straights. The player with the highest score at the end of all rounds wins.\n\n" +
                                            "• [Balut Dice Game] (Three or more players): A poker-like dice game with strategic play, where players aim to form combinations to score points, including Full House, Straights, Four of a Kind, and more. Each turn, players roll five dice and try to form scoring combinations, keeping some dice and re-rolling others to complete the combinations. Points are scored based on the achieved combinations, and the game is played over multiple rounds. The player with the highest total score at the end of the game wins."

                                    5 -> "Master Kavi with these tips:\n" +
                                            "• Balance Risk and Reward: Know when to re-roll or settle.\n" +
                                            "• Adapt to Modes: Use AR for an interactive experience or classic for traditional strategy.\n" +
                                            "• Plan Ahead: Keep an eye on available categories.\n" +
                                            "• Learn Boards: Each board type has unique scoring and strategies."

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
                fontSize = 30.sp,
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            color = Color(0xFF4A3B2F),
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 16.dp)
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
