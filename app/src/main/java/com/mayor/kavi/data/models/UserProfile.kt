package com.mayor.kavi.data.models

import com.mayor.kavi.R
import kotlinx.serialization.Serializable

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatar: Avatar = Avatar.DEFAULT,
    val lastSeen: Long = 0,
    val isOnline: Boolean = false,
    val isInGame: Boolean = false,
    val isWaitingForPlayers: Boolean = false,
    val currentGameId: String = ""
)

@Serializable
enum class Avatar(val resourceId: Int) {
    AVATAR1(R.drawable.avatar_girl1),
    AVATAR2(R.drawable.avatar_girl2),
    AVATAR3(R.drawable.avatar_girl3),
    AVATAR4(R.drawable.avatar_girl4),
    AVATAR5(R.drawable.avatar_boy1),
    AVATAR6(R.drawable.avatar_boy2),
    AVATAR7(R.drawable.avatar_boy3),
    AVATAR8(R.drawable.avatar_boy4),
    AVATAR9(R.drawable.avatar_boy5),
    DEFAULT(R.drawable.default_avatar),
}