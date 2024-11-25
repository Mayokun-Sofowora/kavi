package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.Achievement
import com.mayor.kavi.data.models.Game
import com.mayor.kavi.data.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAchievementRepository : AchievementRepository {

    private val achievements = mutableListOf(
        Achievement(1, "Perfect Score", "Achieve a perfect score", false),
        Achievement(2, "Image Recognition Master", "Master the image recognition mode", false),
        Achievement(3, "Roll Master", "Use all rolls", false)
    )

    private val unlockedAchievements = mutableListOf<Long>()

    override fun getAllAchievements(): Flow<List<Achievement>> {
        return flow {
            emit(achievements.map {
                if (unlockedAchievements.contains(it.id)) {
                    it.copy(isUnlocked = true)
                } else {
                    it
                }
            })
        }
    }

    override suspend fun checkNewAchievements(game: Game): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()

        // Example logic for unlocking an achievement (based on game)
        if (game.score.values.contains(50)) {
            unlocked.add(Achievement(1, "Perfect Score", "Achieve a perfect score", true))
        }

        // Add to unlockedAchievements for persistence simulation
        unlocked.forEach {
            unlockedAchievements.add(it.id)
        }

        return unlocked
    }

    override suspend fun unlockAchievement(achievementId: Long) {
        unlockedAchievements.add(achievementId)
    }

    override suspend fun getAchievementById(achievementId: Long): Achievement? {
        return achievements.find { it.id == achievementId }?.let {
            if (unlockedAchievements.contains(it.id)) {
                it.copy(isUnlocked = true)
            } else {
                it
            }
        }
    }

    override suspend fun deleteAchievement(achievementId: Long) {
        achievements.removeAll { it.id == achievementId }
        unlockedAchievements.remove(achievementId)
    }
}
