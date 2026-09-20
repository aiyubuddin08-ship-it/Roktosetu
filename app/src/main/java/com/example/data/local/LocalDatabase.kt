package com.example.data.local

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

@Entity(tableName = "cached_requests")
data class CachedRequestEntity(
    @PrimaryKey val id: String,
    val patientName: String,
    val bloodGroup: String,
    val unitsRequired: Int,
    val hospitalName: String,
    val hospitalAddress: String,
    val city: String,
    val urgency: String,
    val status: String,
    val contactPhone: String,
    val requiredDateTime: Long,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_donors")
data class CachedDonorEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val bloodGroup: String,
    val availability: String,
    val generalLocation: String,
    val isEmergencyDonor: Boolean,
    val contactPhone: String,
    val donationCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface CachedDataDao {
    @Query("SELECT * FROM cached_requests ORDER BY cachedAt DESC")
    fun getAllCachedRequests(): Flow<List<CachedRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<CachedRequestEntity>)

    @Query("DELETE FROM cached_requests")
    suspend fun clearRequests()

    @Query("SELECT * FROM cached_donors ORDER BY cachedAt DESC")
    fun getAllCachedDonors(): Flow<List<CachedDonorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonors(donors: List<CachedDonorEntity>)

    @Query("DELETE FROM cached_donors")
    suspend fun clearDonors()
}

@Database(
    entities = [CachedRequestEntity::class, CachedDonorEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RoktoSetuDatabase : RoomDatabase() {
    abstract fun cachedDataDao(): CachedDataDao

    companion object {
        @Volatile
        private var INSTANCE: RoktoSetuDatabase? = null

        fun getDatabase(context: Context): RoktoSetuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoktoSetuDatabase::class.java,
                    "roktosetu_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
