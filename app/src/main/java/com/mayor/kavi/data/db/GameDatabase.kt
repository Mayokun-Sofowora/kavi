import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [GameRecord::class],
    version = 1
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val gameMode: String,
    val score: Int,
    val isWin: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface GameDao {
    @Query("SELECT * FROM game_records WHERE userId = :userId")
    fun getGameRecords(userId: String): Flow<List<GameRecord>>

    @Query("""
        SELECT 
            COUNT(*) as gamesPlayed,
            MAX(CASE WHEN gameMode = :gameMode THEN score ELSE 0 END) as highScore,
            SUM(CASE WHEN gameMode = :gameMode AND isWin THEN 1 ELSE 0 END) as wins,
            COUNT(CASE WHEN gameMode = :gameMode THEN 1 END) as totalGames
        FROM game_records 
        WHERE userId = :userId AND gameMode = :gameMode
    """)
    fun getGameStats(userId: String, gameMode: String): Flow<GameModeStats>

    @Insert
    suspend fun insertGameRecord(record: GameRecord)
}

data class GameModeStats(
    val gamesPlayed: Int,
    val highScore: Int,
    val wins: Int,
    val totalGames: Int
) 