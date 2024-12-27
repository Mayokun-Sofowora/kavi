package com.mayor.kavi.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerAnalysis(
    val winRate: String = "",
    val predictedWinRate: Float = 0f,
    val consistency: Float = 0f,
    val improvement: Float = 0f,
    val playStyle: PlayStyle = PlayStyle.BALANCED,
    val trends: List<TrendPoint> = emptyList()
)

@Serializable
data class TrendPoint(
    val turnNumber: Int,
    val score: Int,
    val decision: String
)

enum class PlayStyle {
    CAUTIOUS, BALANCED, AGGRESSIVE
}