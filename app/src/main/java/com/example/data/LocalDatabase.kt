package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tourist_spots")
data class TouristSpot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val visitDurationMinutes: Int = 60,
    val description: String = "",
    val isCustom: Boolean = false,
    val isSelected: Boolean = false
)

@Entity(tableName = "saved_itineraries")
data class SavedItinerary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val city: String,
    val spotsCount: Int,
    val totalDurationMinutes: Int,
    val routeDetails: String, // Step-by-step description or serialized details
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "testing_logs")
data class TestingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val city: String,
    val placeName: String,
    val note: String,
    val observerName: String = "Testor Teren",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TouristSpotDao {
    @Query("SELECT * FROM tourist_spots ORDER BY id ASC")
    fun getAllSpotsFlow(): Flow<List<TouristSpot>>

    @Query("SELECT * FROM tourist_spots WHERE city = :city")
    suspend fun getSpotsByCity(city: String): List<TouristSpot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: TouristSpot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpots(spots: List<TouristSpot>)

    @Query("DELETE FROM tourist_spots WHERE id = :id")
    suspend fun deleteSpotById(id: Long)

    @Query("DELETE FROM tourist_spots WHERE isCustom = 1 AND city = :city")
    suspend fun clearCustomSpotsByCity(city: String)

    @Query("DELETE FROM tourist_spots WHERE isCustom = 1")
    suspend fun deleteAllCustomSpots()

    @Query("UPDATE tourist_spots SET isSelected = :isSelected WHERE id = :id")
    suspend fun updateSelection(id: Long, isSelected: Boolean)

    @Query("UPDATE tourist_spots SET isSelected = 0 WHERE city = :city")
    suspend fun deselectAllSpotsForCity(city: String)
}

@Dao
interface SavedItineraryDao {
    @Query("SELECT * FROM saved_itineraries ORDER BY timestamp DESC")
    fun getAllSavedItinerariesFlow(): Flow<List<SavedItinerary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItinerary(itinerary: SavedItinerary): Long

    @Query("DELETE FROM saved_itineraries WHERE id = :id")
    suspend fun deleteItineraryById(id: Long)

    @Query("DELETE FROM saved_itineraries")
    suspend fun deleteAllItineraries()
}

@Dao
interface TestingLogDao {
    @Query("SELECT * FROM testing_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<TestingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TestingLog): Long

    @Query("DELETE FROM testing_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM testing_logs WHERE city = :city")
    suspend fun clearLogsByCity(city: String)

    @Query("DELETE FROM testing_logs")
    suspend fun deleteAllLogs()
}

@Database(entities = [TouristSpot::class, SavedItinerary::class, TestingLog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun touristSpotDao(): TouristSpotDao
    abstract fun savedItineraryDao(): SavedItineraryDao
    abstract fun testingLogDao(): TestingLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bustour_optimizer_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}