package com.mayor.kavi.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.models.*

@Composable
fun AnalyticsDashboard(
    modifier: Modifier = Modifier
) {
    val statisticsManager = StatisticsManager.LocalStatisticsManager.current
    val gameStats by statisticsManager.gameStatistics.collectAsState()
    val playerAnalysis = gameStats?.playerAnalysis

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Analytics Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Performance Overview Card
        PerformanceCard(playerAnalysis)

        Spacer(modifier = Modifier.height(16.dp))

        // Time Analytics Card
        TimeAnalyticsCard(playerAnalysis?.timeMetrics)

        Spacer(modifier = Modifier.height(16.dp))

        // Decision Patterns Card
        DecisionPatternsCard(playerAnalysis?.decisionPatterns)

        Spacer(modifier = Modifier.height(16.dp))

        // Achievements Card
        AchievementsCard(playerAnalysis?.achievementProgress)
    }
}

@Composable
private fun PerformanceCard(analysis: PlayerAnalysis?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Performance Metrics",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val metrics = analysis?.performanceMetrics
            if (metrics != null) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Current Streak", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = {
                            metrics.currentStreak.toFloat() / maxOf(
                                metrics.longestStreak,
                                1
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Text("Longest Streak", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { metrics.longestStreak.toFloat() / 10f }, // Normalize to 10 games
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Current Streak", metrics.currentStreak.toString())
                    StatItem("Comebacks", metrics.comebacks.toString())
                    StatItem("Close Games", metrics.closeGames.toString())
                }
            }
        }
    }
}

@Composable
private fun TimeAnalyticsCard(timeMetrics: TimeMetrics?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Time Analytics",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (timeMetrics != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        "Avg Game",
                        formatTime(timeMetrics.averageGameDuration)
                    )
                    StatItem(
                        "Fastest",
                        formatTime(timeMetrics.fastestGame)
                    )
                    StatItem(
                        "Total Time",
                        formatTime(timeMetrics.totalPlayTime)
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionPatternsCard(patterns: DecisionPatterns?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Decision Patterns",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (patterns != null) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Risk Taking", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { patterns.riskTaking },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Text("Average Rolls per Turn", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { patterns.averageRollsPerTurn / 6f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Text("Banking Threshold", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { patterns.bankingThreshold / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Risk Level", "${(patterns.riskTaking * 100).toInt()}%")
                    StatItem("Avg Rolls", "%.1f".format(patterns.averageRollsPerTurn))
                    StatItem("Bank At", patterns.bankingThreshold.toInt().toString())
                }
            }
        }
    }
}

@Composable
private fun AchievementsCard(achievements: Map<String, Float>?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Achievements",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            achievements?.forEach { (achievement, progress) ->
                AchievementItem(
                    name = achievement.replace("_", " "),
                    progress = progress
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementItem(name: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = when {
                progress >= 0.8f -> Color(0xFF4CAF50)  // Green
                progress >= 0.5f -> Color(0xFFFFC107)  // Yellow
                else -> Color(0xFFFF5722)              // Orange
            }
        )
    }
}

private fun formatTime(timeInMillis: Long): String {
    val seconds = (timeInMillis / 1000) % 60
    val minutes = (timeInMillis / (1000 * 60)) % 60
    val hours = (timeInMillis / (1000 * 60 * 60))

    return when {
        hours > 0 -> String.format("%dh %02dm", hours, minutes)
        minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
        else -> String.format("%ds", seconds)
    }
} 