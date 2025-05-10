package dev.nobrayner.blokit

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Entity
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val completed: Boolean = false,
    val marked: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now(),
    @ColumnInfo(name = "marked_at") val markedAt: Instant? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Instant? = null,
)

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo WHERE completed = 0")
    fun getIncompleteTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todo WHERE completed = 0 AND marked = 1 ORDER BY marked_at DESC")
    fun getMarkedTodos(): Flow<List<Todo>>

    @Insert
    suspend fun insert(vararg todos: Todo)

    @Update
    suspend fun update(vararg todos: Todo)
}

@Entity
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "finished_at") val finishedAt: Instant,
)

@Dao
interface BlockDao {
    @Query("SELECT * FROM block WHERE DATE(started_at) = DATE('now')")
    fun getTodaysBlocks(): Flow<List<Block>>

    @Insert
    suspend fun insert(vararg blocks: Block)
}

@Database(
    version = 3,
    entities = [
        Todo::class,
        Block::class,
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
    exportSchema = true,
)
@TypeConverters(InstantConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun blockDao(): BlockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blokit_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class InstantConverters {
    @TypeConverter
    fun fromTimestamp(value: String?): Instant? = value?.let {
        Instant.parse(value)
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): String? = instant?.toString()
}