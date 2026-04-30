package com.asyria.security.data.prayer

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    suspend fun getPrayerTimesForDate(date: String): PrayerTimesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: PrayerTimesEntity)
}

@Dao
interface SupplicationDao {
    @Query("SELECT * FROM supplications")
    suspend fun getAllSupplications(): List<SupplicationEntity>

    @Query("SELECT * FROM supplications WHERE category = :category")
    suspend fun getSupplicationsByCategory(category: String): List<SupplicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplications(supplications: List<SupplicationEntity>)
}

@Database(entities = [PrayerTimesEntity::class, SupplicationEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun supplicationDao(): SupplicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "asyria_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
