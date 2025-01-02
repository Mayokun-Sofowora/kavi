package com.mayor.kavi.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerAnalysis(
    val predictedWinRate: Float = 0f,
    val consistency: Float = 0f,
    val playStyle: PlayStyle = PlayStyle.BALANCED,
    val improvement: Float = 0f,
    // Enhanced analysis metrics
    val decisionPatterns: DecisionPatterns = DecisionPatterns(),
    val timeMetrics: TimeMetrics = TimeMetrics(),
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val achievementProgress: Map<String, Float> = emptyMap()
)

@Serializable
data class DecisionPatterns(
    val averageRollsPerTurn: Float = 0f,
    val bankingThreshold: Float = 0f,  // Average score when player banks
    val riskTaking: Float = 0f,  // Measure of how often player continues rolling
    val decisionSpeed: Float = 0f  // Average time to make decisions
)

@Serializable
data class TimeMetrics(
    val averageGameDuration: Long = 0L,
    val averageTurnDuration: Long = 0L,
    val fastestGame: Long = 0L,
    val totalPlayTime: Long = 0L
)

@Serializable
data class PerformanceMetrics(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val comebacks: Int = 0,  // Games won after being behind by significant margin
    val closeGames: Int = 0,  // Games decided by small margin
    val personalBests: Map<String, Int> = emptyMap(),  // Game mode to highest score
    val averageScoreByMode: Map<String, Float> = emptyMap()
)

enum class PlayStyle {
    AGGRESSIVE,
    BALANCED,
    CAUTIOUS
}

enum class Achievement {
    STREAK_MASTER,
    COMEBACK_KING,
    CONSISTENT_PLAYER,
    RISK_TAKER,
    SPEED_STAR,
    VETERAN_PLAYER
}