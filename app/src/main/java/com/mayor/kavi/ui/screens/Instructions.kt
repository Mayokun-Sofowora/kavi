package com.mayor.kavi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.geometry.Offset
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
                        modifier = Modifier.height(100.dp)
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFDBB5).copy(alpha = 0.7f),
                            Color(0xFF6C4E31).copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            if (showOnlyPage) {
                // Display only the specified page
                InstructionPage(
                    title = "Game Modes",
                    description = "Kavi offers two exciting play modes:\n\n" +
                            "• AR Mode: An immersive augmented reality experience where you interact with virtual dice in a dynamic environment.\n\n" +
                            "• Classic Mode: A traditional board-style game focusing on pure strategic dice rolling and scoring.",
                    imageRes = R.drawable.instr_img1
                )
            } else {
                // Standard multi-page mode
                val scope = rememberCoroutineScope()
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
                                    0 -> "Kavi is an exciting multiplayer dice game where you compete against another player to accumulate points through strategic dice rolls. Your goal is to outscore your opponent by making smart choices with each roll."
                                    1 -> "Kavi offers two exciting play modes:\n\n" +
                                            "• AR Mode: An immersive augmented reality experience where you interact with virtual dice in a dynamic environment.\n\n" +
                                            "• Classic Mode: A traditional board-style game focusing on pure strategic dice rolling and scoring."

                                    2 -> "Game Flow:\n\n" +
                                            "• Turn-based gameplay where players alternate rolling dice\n" +
                                            "• After each roll, choose which dice to keep or re-roll\n" +
                                            "• Strategically select scoring categories\n" +
                                            "• Once a category is filled, it cannot be used again"

                                    3 -> "Winning Conditions:\n\n" +
                                            "• Game ends when all scoring categories are filled\n" +
                                            "• Highest total score wins\n" +
                                            "• Bonus: Reach 7 or more points to claim early victory"

                                    4 -> "Scoring Categories:\n\n" +
                                            "• Ones to Sixes: Sum of matching dice values\n" +
                                            "• Three/Four/Five of a Kind: Total dice value for matching sets\n" +
                                            "• Full House: 25 points for three of one number, two of another\n" +
                                            "• Small Straight: 30 points for four consecutive dice\n" +
                                            "• Large Straight: 40 points for five consecutive dice\n" +
                                            "• Chance: Total of all dice, regardless of pattern"

                                    5 -> "Winning Strategies:\n\n" +
                                            "• Plan each roll carefully\n" +
                                            "• Anticipate opponent's moves\n" +
                                            "• Balance risk and reward\n" +
                                            "• Keep track of available scoring categories\n" +
                                            "• Adapt your strategy as the game progresses"

                                    else -> "Kavi combines luck and strategy. Master the nuances, challenge your friends, and become the ultimate Kavi champion!"
                                },
                                imageRes = if (page == 1) R.drawable.instr_img1 else null
                            )
                        }
                    }

                    // Navigation and Page Indicator
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
                ) // Add shadow
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
                    .heightIn(max = 200.dp)
                    .padding(bottom = 16.dp)
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Normal,
            lineHeight = 30.sp,
            letterSpacing = 0.3.sp,
            fontSize = 18.sp,
            color = Color.White,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 3.dp),
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

@Preview(showBackground = true)
@Composable
fun InstructionsScreenPreview() {
    InstructionsScreen(onClose = {})
}