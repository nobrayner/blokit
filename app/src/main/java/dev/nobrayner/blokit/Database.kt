package dev.nobrayner.blokit

import android.content.Context
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
    @ColumnInfo(name = "completed_at") val completedAt: Instant?,
)

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo WHERE completed = 0")
    fun getIncompleteTodos(): Flow<List<Todo>>

    @Insert
    suspend fun insert(note: Todo)

    @Update
    suspend fun update(note: Todo)
}

@Database(entities = [Todo::class], version = 1)
@TypeConverters(InstantConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

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