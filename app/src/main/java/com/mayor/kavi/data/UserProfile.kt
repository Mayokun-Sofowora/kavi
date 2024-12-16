package com.mayor.kavi.data

import com.mayor.kavi.R

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val name: String = if (email.isNotEmpty() && email.contains("@")) email.substringBefore("@") else "Guest User",
    val avatar: Avatar = Avatar.DEFAULT,
    val favoriteGames: List<String> = listOf(),
    val recentScores: List<Int> = listOf()
)

enum class Avatar(val resourceId: Int) {
    GIRL1(R.drawable.avatar_girl1),
    GIRL2(R.drawable.avatar_girl2),
    GIRL3(R.drawable.avatar_girl3),
    GIRL4(R.drawable.avatar_girl4),
    BOY1(R.drawable.avatar_boy1),
    BOY2(R.drawable.avatar_boy2),
    BOY3(R.drawable.avatar_boy3),
    BOY4(R.drawable.avatar_boy4),
    BOY5(R.drawable.avatar_boy5),
    DEFAULT(R.drawable.default_avatar),
}