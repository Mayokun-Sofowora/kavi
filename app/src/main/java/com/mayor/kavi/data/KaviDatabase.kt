package com.mayor.kavi.data

import android.content.Context
import androidx.room.*
import com.mayor.kavi.data.dao.*
import com.mayor.kavi.utils.MyTypeConverters

/**
 * Manages data persistence using Room. It contains Dao interfaces (e.g., UserDao) allowing them
 * to interact with other tables like UserEntity. It is built as a singleton using Room's
 * DatabaseBuilder.
 * */
@Database(
    entities = [Users::class, Games::class, Achievements::class, LeaderboardEntry::class,
        GameSessions::class, GameResults::class, UserSettings::class, UserStatistics::class,
        Friends::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(MyTypeConverters::class)
abstract class KaviDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun userDao(): UserDao
    abstract fun achievementDao(): AchievementDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun gameSessionsDao(): GameSessionsDao
    abstract fun gameResultsDao(): GameResultsDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun userStatisticsDao(): UserStatisticsDao
    abstract fun friendsDao(): FriendsDao

    companion object {
        @Volatile
        private var INSTANCE: KaviDatabase? = null

        fun getInstance(context: Context): KaviDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaviDatabase::class.java,
                    "kavi_database"
                ).fallbackToDestructiveMigration()  // For future migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}