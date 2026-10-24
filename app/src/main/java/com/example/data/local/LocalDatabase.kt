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

@Entity(tableName = "cached_announcements")
data class CachedAnnouncementEntity(
    @PrimaryKey val id: String,
    val titleBn: String,
    val bodyBn: String,
    val category: String,
    val targetAudience: String,
    val targetDivision: String,
    val targetDistrict: String,
    val status: String,
    val createdByUid: String,
    val createdByEmail: String,
    val createdAt: Long,
    val publishedAt: Long?,
    val recipientCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dismissed_announcements")
data class DismissedAnnouncementEntity(
    @PrimaryKey val announcementId: String,
    val dismissedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_blood_banks")
data class CachedBloodBankEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameBn: String,
    val organization: String,
    val address: String,
    val city: String,
    val district: String,
    val phone: String,
    val emergencyHotline: String,
    val is24Hours: Boolean,
    val availableGroupsCsv: String,
    val verified: Boolean,
    val isActive: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_ambulances")
data class CachedAmbulanceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleBn: String,
    val provider: String,
    val coverageArea: String,
    val district: String,
    val city: String,
    val phone: String,
    val type: String,
    val isAvailable24x7: Boolean,
    val isActive: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_blood_camps")
data class CachedBloodCampEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleBn: String,
    val organizer: String,
    val location: String,
    val district: String,
    val city: String,
    val date: String,
    val time: String,
    val contactPhone: String,
    val targetUnits: Int,
    val registeredCount: Int,
    val isUserRsvp: Boolean,
    val description: String,
    val isActive: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_camp_rsvps", primaryKeys = ["campId", "uid"])
data class CachedCampRsvpEntity(
    val campId: String,
    val uid: String,
    val createdAt: Long = System.currentTimeMillis()
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

    @Query("SELECT * FROM cached_announcements ORDER BY COALESCE(publishedAt, createdAt) DESC")
    fun getAllCachedAnnouncements(): Flow<List<CachedAnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<CachedAnnouncementEntity>)

    @Query("DELETE FROM cached_announcements")
    suspend fun clearAnnouncements()

    @Query("SELECT announcementId FROM dismissed_announcements")
    fun getDismissedAnnouncementIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDismissedAnnouncement(entity: DismissedAnnouncementEntity)

    // Blood Banks cache
    @Query("SELECT * FROM cached_blood_banks ORDER BY name ASC")
    fun getAllCachedBloodBanks(): Flow<List<CachedBloodBankEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodBanks(banks: List<CachedBloodBankEntity>)

    @Query("DELETE FROM cached_blood_banks")
    suspend fun clearBloodBanks()

    // Ambulances cache
    @Query("SELECT * FROM cached_ambulances ORDER BY title ASC")
    fun getAllCachedAmbulances(): Flow<List<CachedAmbulanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmbulances(ambulances: List<CachedAmbulanceEntity>)

    @Query("DELETE FROM cached_ambulances")
    suspend fun clearAmbulances()

    // Blood Camps cache
    @Query("SELECT * FROM cached_blood_camps ORDER BY date ASC")
    fun getAllCachedBloodCamps(): Flow<List<CachedBloodCampEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodCamps(camps: List<CachedBloodCampEntity>)

    @Query("DELETE FROM cached_blood_camps")
    suspend fun clearBloodCamps()

    // Camp RSVPs cache
    @Query("SELECT * FROM cached_camp_rsvps WHERE uid = :uid")
    fun getCachedRsvpsForUser(uid: String): Flow<List<CachedCampRsvpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampRsvps(rsvps: List<CachedCampRsvpEntity>)

    @Query("DELETE FROM cached_camp_rsvps WHERE campId = :campId AND uid = :uid")
    suspend fun deleteCampRsvp(campId: String, uid: String)
}

@Database(
    entities = [
        CachedRequestEntity::class,
        CachedDonorEntity::class,
        CachedAnnouncementEntity::class,
        DismissedAnnouncementEntity::class,
        CachedBloodBankEntity::class,
        CachedAmbulanceEntity::class,
        CachedBloodCampEntity::class,
        CachedCampRsvpEntity::class
    ],
    version = 3,
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
