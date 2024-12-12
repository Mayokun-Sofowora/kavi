package com.mayor.kavi.data

data class UserProfile(
    val uid: String = "",
    val name: String = "Guest",
    val email: String = "",
    val image: String? = null,
    val favoriteGames: List<String> = listOf(),
    val recentScores: List<Int> = listOf()
)
