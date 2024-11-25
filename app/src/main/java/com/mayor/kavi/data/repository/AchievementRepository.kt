package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.AchievementDao
import com.mayor.kavi.data.dao.Achievements
import com.mayor.kavi.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface AchievementRepository {

    fun getAllAchievements(): Flow<List<Achievement>>

    suspend fun checkNewAchievements(game: Games, gameSession: GameSessions): List<Achievement>

    suspend fun unlockAchievement(achievementId: Long)

    suspend fun getAchievementById(achievementId: Long): Achievement?

    suspend fun deleteAchievement(achievementId: Long)
}

class AchievementRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao
) : AchievementRepository {

    // Get all achievements as a Flow
    override fun getAllAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements().map { entities ->
            // Map AchievementEntity to Achievement
            entities.map {
                Achievement(
                    it.achievementId,
                    it.userId,
                    it.name,
                    it.description,
                    it.unlocked,
                    it.iconResource
                )
            }
        }
    }

    // Check new achievements based on the current game and session
    override suspend fun checkNewAchievements(
        game: Games,
        gameSession: GameSessions
    ): List<Achievement> {
        // Fetch all achievements and map to Achievement
        val achievements = achievementDao.getAllAchievements().firstOrNull()?.map {
            Achievement(
                it.achievementId,
                it.userId,
                it.name,
                it.description,
                it.unlocked,
                it.iconResource
            )
        } ?: emptyList()

        val unlockedAchievements = mutableListOf<Achievement>()

        // Helper function to process achievement unlocking
        suspend fun unlockAchievement(achievementName: String) {
            val achievement = achievements.firstOrNull { it.name == achievementName }
            achievement?.let {
                if (!it.unlocked) {
                    achievementDao.insertAchievement(
                        Achievements(
                            it.achievementId,
                            it.userId,
                            it.name,
                            it.description,
                            true,
                            it.iconResource
                        )
                    )
                    unlockedAchievements.add(it.copy(unlocked = true))
                }
            }
        }

        // Unlock achievements based on conditions

        // 1. Perfect Score Achievement
        if (gameSession.scores.values.contains(50)) unlockAchievement("Perfect Score")

        // 2. Image Recognition Achievement
        if (gameSession.rolledDice.isNotEmpty() && gameSession.rolledDice.all { it.value == 6 }) unlockAchievement(
            "Image Recognition Master"
        )

        // 3. Roll Master Achievement
        if (gameSession.rolledDice.isEmpty()) unlockAchievement("Roll Master")

        // 4. Dice Lock Expert Achievement
        if (gameSession.rolledDice.size >= 4) unlockAchievement("Dice Lock Expert")

        // 5. High Score Achievement
        if (gameSession.scores.values.any { it >= 100 }) unlockAchievement("Century Club")

        // 6. Multiplayer Champion Achievement
        if (game.players.size >= 3 && game.gameType == GameTypes.LOCAL_MULTIPLAYER) unlockAchievement(
            "Party Master"
        )

        // 7. AI Challenger Achievement
        if (game.gameType == GameTypes.COMPUTER_AI) unlockAchievement("AI Challenger")

        // 8. Virtual Mode Pioneer Achievement
        if (game.gameMode == GameModes.VIRTUAL) unlockAchievement("Virtual Pioneer")

        // 9. Quick Victory Achievement
        if (gameSession.scores.values.any { it >= 30 } && gameSession.rolledDice.size >= 2) unlockAchievement(
            "Quick Victor"
        )

        // 10. Consecutive Sixes Achievement
        if (gameSession.rolledDice.count { it.value == 6 } >= 3) unlockAchievement("Triple Sixes")

        // 11. Perfect Recognition Achievement
        if (gameSession.rolledDice.isNotEmpty() && gameSession.rolledDice.size == gameSession.scores.size) unlockAchievement(
            "Perfect Recognition"
        )

        // 12. Marathon Player Achievement
        val gameTimeInMinutes =
            (System.currentTimeMillis() - gameSession.sessionStart.toEpochSecond(java.time.ZoneOffset.UTC)) / 60
        if (gameTimeInMinutes >= 30) unlockAchievement("Marathon Player")

        // 13. Precision Player Achievement
        if (gameSession.rolledDice.size == gameSession.scores.values.size) unlockAchievement("Precision Master")

        // 14. Strategy Master Achievement
        if (gameSession.rolledDice.isNotEmpty() && gameSession.scores.values.any { it >= 40 }) unlockAchievement(
            "Strategy Master"
        )

        // 15. Social Butterfly Achievement
        if (game.players.size >= 4 && game.gameType == GameTypes.LOCAL_MULTIPLAYER) unlockAchievement(
            "Social Butterfly"
        )

        return unlockedAchievements
    }

    // Unlock an achievement by its ID
    override suspend fun unlockAchievement(achievementId: Long) {
        // Get the achievement entity and update it to unlocked
        val achievement = achievementDao.getAchievementById(achievementId)
        achievement?.let {
            achievementDao.insertAchievement(it.copy(unlocked = true))
        }
    }

    // Get an achievement by its ID
    override suspend fun getAchievementById(achievementId: Long): Achievement? {
        val achievementEntity = achievementDao.getAchievementById(achievementId)
        return achievementEntity?.let {
            Achievement(
                it.achievementId,
                it.userId,
                it.name,
                it.description,
                it.unlocked,
                it.iconResource
            )
        }
    }

    // Delete an achievement by its ID
    override suspend fun deleteAchievement(achievementId: Long) {
        val achievement = achievementDao.getAchievementById(achievementId)
        achievement?.let {
            achievementDao.deleteAchievement(it)
        }
    }
}
