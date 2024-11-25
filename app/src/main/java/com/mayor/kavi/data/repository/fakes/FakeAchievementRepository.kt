package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.mayor.kavi.R

class FakeAchievementRepository : AchievementRepository {

    private val achievements = mutableListOf<AchievementsEntity>()
    private val achievementsFlow = MutableStateFlow<List<AchievementsEntity>>(emptyList())
    private var achievementIdCounter = 1L

    override fun getAllAchievements(): Flow<List<AchievementsEntity>> {
        return achievementsFlow
    }

    override suspend fun checkNewAchievements(
        game: GamesEntity,
        user: UsersEntity,
        gameSession: GameSessionsEntity
    ): List<AchievementsEntity> {
        val newAchievements = mutableListOf<AchievementsEntity>()

        // Helper function to unlock achievements
        fun unlockAchievement(name: String, description: String, iconResource: Int) {
            val existing = achievements.find { it.name == name && it.userId == user.userId }
            if (existing == null) {
                val achievement = AchievementsEntity(
                    achievementId = achievementIdCounter++,
                    userId = user.userId,
                    name = name,
                    description = description,
                    unlocked = true,
                    iconResource = iconResource,
                )
                achievements.add(achievement)
                newAchievements.add(achievement)
                emitAchievements()
            }
        }

        // Sample logic to unlock achievements based on conditions
        if (gameSession.scores.values.contains(50)) {
            unlockAchievement(
                "Perfect Score",
                "Achieve a perfect score in a session.",
                R.drawable.logo // TODO: Replace
            )
        }

        if (gameSession.rolledDice.count { it.value == 6 } >= 3) {
            unlockAchievement(
                "Triple Sixes",
                "Roll three or more sixes in a session.",
                R.drawable.logo // TODO: Replace
            )
        }

        if (gameSession.rolledDice.isNotEmpty() && gameSession.rolledDice.all { it.value == 6 }) {
            unlockAchievement(
                "Image Recognition Master",
                "Achieve mastery in dice recognition.",
                R.drawable.logo // TODO: Replace
            )
        }

        return newAchievements
    }

    override suspend fun unlockAchievement(achievementId: Long) {
        val achievement = achievements.find { it.achievementId == achievementId }
        achievement?.let {
            val updated = it.copy(unlocked = true)
            achievements.remove(it)
            achievements.add(updated)
            emitAchievements()
        }
    }

    override suspend fun getAchievementById(achievementId: Long): AchievementsEntity? {
        return achievements.find { it.achievementId == achievementId }
    }

    override suspend fun deleteAchievement(achievementId: Long) {
        achievements.removeIf { it.achievementId == achievementId }
        emitAchievements()
    }

    private fun emitAchievements() {
        achievementsFlow.value = achievements.toList()
    }
}
