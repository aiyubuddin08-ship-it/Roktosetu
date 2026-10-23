package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirestoreService
import com.example.data.firebase.Resource
import com.example.data.local.CachedDonorEntity
import com.example.data.local.CachedRequestEntity
import com.example.data.local.RoktoSetuDatabase
import com.example.model.AuditLogEntry
import com.example.model.AuthState
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.Campaign
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.Organization
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.SystemMetrics
import com.example.model.UserProfile
import com.example.model.VerificationTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Central Repository coordinating Firebase Auth, Cloud Firestore (using the custom
 * `R.string.firestore_database_id` database), and offline Room cache.
 */
class RoktoSetuRepository(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreService,
    private val localDb: RoktoSetuDatabase? = null
) {
    constructor(context: Context) : this(
        authService = FirebaseAuthService(context.applicationContext),
        firestoreService = FirestoreService(
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        ),
        localDb = RoktoSetuDatabase.getDatabase(context.applicationContext)
    )

    constructor(firestore: FirebaseFirestore, auth: FirebaseAuth = FirebaseAuth.getInstance()) : this(
        authService = FirebaseAuthService(auth, firestore),
        firestoreService = FirestoreService(firestore),
        localDb = null
    )

    val currentUserProfile: StateFlow<UserProfile?> = authService.currentUserProfile
    val authState: StateFlow<AuthState> = authService.authState
    val isAuthenticated: Boolean get() = authService.isAuthenticated
    val currentUserId: String? get() = authService.currentUserId

    // --- Authentication Operations ---

    suspend fun signIn(email: String, pass: String): Resource<UserProfile> =
        authService.signInWithEmail(email, pass)

    suspend fun register(
        email: String,
        pass: String,
        name: String,
        bloodGroup: BloodGroup,
        isDonor: Boolean,
        phone: String,
        division: String = "Dhaka",
        district: String = "Dhaka",
        upazila: String = "",
        city: String = district,
        area: String = "",
        dateOfBirth: String = ""
    ): Resource<UserProfile> = authService.registerWithEmail(
        email = email,
        pass = pass,
        name = name,
        bloodGroup = bloodGroup,
        isDonor = isDonor,
        phone = phone,
        division = division,
        district = district.ifBlank { city.ifBlank { "Dhaka" } },
        upazila = upazila,
        area = area,
        dateOfBirth = dateOfBirth
    )

    suspend fun signInWithGoogle(idToken: String): Resource<UserProfile> =
        authService.signInWithGoogleCredential(idToken)

    suspend fun completeProfile(
        name: String,
        phone: String,
        bloodGroup: BloodGroup,
        dateOfBirth: String,
        isDonor: Boolean,
        donorAvailability: DonorAvailability,
        isEmergencyDonor: Boolean,
        division: String = "Dhaka",
        district: String = "Dhaka",
        upazila: String = "",
        city: String = district,
        area: String = "",
        photoUrl: String = ""
    ): Resource<UserProfile> = authService.completeUserProfile(
        name = name,
        phone = phone,
        bloodGroup = bloodGroup,
        dateOfBirth = dateOfBirth,
        isDonor = isDonor,
        donorAvailability = donorAvailability,
        isEmergencyDonor = isEmergencyDonor,
        division = division,
        district = district.ifBlank { city.ifBlank { "Dhaka" } },
        upazila = upazila,
        area = area,
        photoUrl = photoUrl
    )

    suspend fun sendEmailVerification(): Resource<Unit> = authService.sendEmailVerification()

    suspend fun checkEmailVerification(): Boolean = authService.checkEmailVerification()

    suspend fun sendPasswordReset(email: String): Resource<Unit> =
        authService.sendPasswordResetEmail(email)

    suspend fun updateProfile(profile: UserProfile): Resource<UserProfile> =
        authService.updateProfile(profile)

    suspend fun deleteAccount(reauthPassword: String? = null): Resource<Unit> {
        val result = authService.deleteAccount(reauthPassword)
        if (result is Resource.Success) {
            localDb?.cachedDataDao()?.clearDonors()
            localDb?.cachedDataDao()?.clearRequests()
        }
        return result
    }

    suspend fun clearLocalUserCache() {
        localDb?.cachedDataDao()?.clearDonors()
        localDb?.cachedDataDao()?.clearRequests()
    }

    fun signOut() {
        authService.signOut()
    }

    // --- Blood Requests ---

    fun getBloodRequests(
        filterBloodGroup: BloodGroup? = null,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        onError: ((String) -> Unit)? = null
    ): Flow<List<BloodRequest>> =
        firestoreService.observeBloodRequests(
            filterBloodGroup = filterBloodGroup,
            divisionFilter = divisionFilter,
            districtFilter = districtFilter,
            upazilaFilter = upazilaFilter,
            areaQuery = areaQuery,
            onError = onError
        )

    suspend fun createBloodRequest(request: BloodRequest): Resource<String> {
        val activeUid = request.requesterUid.ifBlank { currentUserId ?: "" }
        return firestoreService.createBloodRequest(request.copy(requesterUid = activeUid))
    }

    suspend fun acceptBloodRequest(
        requestId: String,
        donorUid: String,
        donorName: String,
        donorBloodGroup: BloodGroup? = null
    ): Resource<Unit> =
        firestoreService.acceptBloodRequest(requestId, donorUid, donorName, donorBloodGroup)

    suspend fun updateRequestStatus(requestId: String, status: RequestStatus): Resource<Unit> =
        firestoreService.updateRequestStatus(requestId, status)

    // --- Public Donors Search (Map-Free: Division, District, Upazila, Area) ---

    fun getDonors(
        filterBloodGroup: BloodGroup? = null,
        includeCompatibleForRecipient: Boolean = false,
        includeCompatible: Boolean = includeCompatibleForRecipient,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        areaFilter: String? = areaQuery,
        onlyAvailable: Boolean = false,
        onError: ((String) -> Unit)? = null
    ): Flow<List<PublicDonorView>> =
        firestoreService.observePublicDonors(
            filterBloodGroup = filterBloodGroup,
            includeCompatibleForRecipient = includeCompatible || includeCompatibleForRecipient,
            divisionFilter = divisionFilter,
            districtFilter = districtFilter,
            upazilaFilter = upazilaFilter,
            areaQuery = areaQuery ?: areaFilter,
            onlyAvailable = onlyAvailable,
            onError = onError
        )

    // --- 1-to-1 Chat ---

    fun getConversations(
        userUid: String,
        onError: ((String) -> Unit)? = null
    ): Flow<List<Conversation>> =
        firestoreService.observeConversations(userUid, onError)

    fun getMessages(
        conversationId: String,
        onError: ((String) -> Unit)? = null
    ): Flow<List<ChatMessage>> =
        firestoreService.observeMessages(conversationId, onError)

    suspend fun getOrCreateConversation(
        myUid: String,
        myName: String,
        myBloodGroup: String,
        otherUid: String,
        otherName: String,
        otherBloodGroup: String,
        requestId: String? = null
    ): Resource<String> = firestoreService.getOrCreateConversation(
        myUid, myName, myBloodGroup, otherUid, otherName, otherBloodGroup, requestId
    )

    suspend fun sendMessage(
        conversationId: String,
        senderUid: String,
        senderName: String,
        text: String
    ): Resource<Unit> = firestoreService.sendMessage(conversationId, senderUid, senderName, text)

    // --- Donations, Organizations & Campaigns ---

    fun getUserDonations(
        donorUid: String,
        onError: ((String) -> Unit)? = null
    ): Flow<List<DonationRecord>> =
        firestoreService.observeUserDonations(donorUid, onError)

    fun getOrganizations(onError: ((String) -> Unit)? = null): Flow<List<Organization>> =
        firestoreService.observeOrganizations(onError)

    fun getCampaigns(onError: ((String) -> Unit)? = null): Flow<List<Campaign>> =
        firestoreService.observeCampaigns(onError)

    suspend fun createCampaign(campaign: Campaign): Resource<String> =
        firestoreService.createCampaign(campaign)

    // --- Admin & Verification Queue ---

    suspend fun checkIsCurrentUserAdmin(uid: String): Boolean {
        if (uid.isBlank()) return false
        return authService.isCurrentUserAdmin(forceRefreshToken = true) ||
            firestoreService.checkIsCurrentUserAdmin(uid)
    }

    suspend fun checkIsAdmin(): Boolean {
        val uid = currentUserId ?: return false
        return checkIsCurrentUserAdmin(uid)
    }

    suspend fun getSystemMetrics(): SystemMetrics {
        return firestoreService.fetchSystemMetrics()
    }

    suspend fun getEmergencyBroadcastArmed(): Boolean {
        return firestoreService.fetchEmergencyBroadcastArmed()
    }

    suspend fun setEmergencyBroadcastArmed(armed: Boolean): Resource<Boolean> {
        val adminUid = currentUserId ?: return Resource.Error("Not signed in as administrator.")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.setEmergencyBroadcastArmed(armed, adminUid, adminEmail)
    }

    suspend fun uploadVerificationDocument(
        context: Context,
        userId: String,
        uri: Uri
    ): Resource<String> =
        firestoreService.uploadVerificationDocument(context, userId, uri)

    suspend fun submitVerificationRequest(
        applicantUid: String,
        name: String,
        type: String,
        documents: List<String>
    ): Resource<String> =
        firestoreService.submitVerificationRequest(applicantUid, name, type, documents)

    suspend fun submitVerificationTask(
        targetId: String,
        type: String,
        name: String,
        bloodGroup: BloodGroup? = null,
        documents: List<String>
    ): Resource<String> =
        firestoreService.submitVerificationRequest(targetId, name, type, documents)

    fun getPendingVerificationTasks(
        onError: ((String) -> Unit)? = null
    ): Flow<List<VerificationTask>> =
        firestoreService.observePendingVerificationTasks(onError)

    fun getAuditLogs(
        onError: ((String) -> Unit)? = null
    ): Flow<List<AuditLogEntry>> =
        firestoreService.observeAuditLogs(onError)

    suspend fun reviewVerificationTaskAsAdmin(
        task: VerificationTask,
        approved: Boolean,
        adminUid: String,
        adminEmail: String
    ): Resource<Unit> =
        firestoreService.reviewVerificationTaskAsAdmin(task, approved, adminUid, adminEmail)

    suspend fun reviewVerificationTask(
        task: VerificationTask,
        approve: Boolean,
        reason: String = ""
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("Not signed in as administrator.")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.reviewVerificationTaskAsAdmin(task, approve, adminUid, adminEmail)
    }

    // --- Offline Room Cache ---

    fun getCachedRequests(): Flow<List<CachedRequestEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedRequests()

    fun getCachedDonors(): Flow<List<CachedDonorEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedDonors()

    suspend fun cacheRequestsLocally(requests: List<BloodRequest>) {
        val dao = localDb?.cachedDataDao() ?: return
        val entities = requests.map {
            CachedRequestEntity(
                id = it.id,
                patientName = it.patientName,
                bloodGroup = it.bloodGroup.display,
                unitsRequired = it.unitsRequired,
                hospitalName = it.hospitalName,
                hospitalAddress = it.hospitalAddress,
                city = it.district.ifBlank { it.city },
                urgency = it.urgency.name,
                status = it.status.name,
                contactPhone = it.contactPhone,
                requiredDateTime = it.requiredDateTime
            )
        }
        dao.insertRequests(entities)
    }

    suspend fun cacheDonorsLocally(donors: List<PublicDonorView>) {
        val dao = localDb?.cachedDataDao() ?: return
        val entities = donors.map {
            CachedDonorEntity(
                uid = it.uid,
                name = it.name,
                bloodGroup = it.bloodGroup.display,
                availability = it.availability.name,
                generalLocation = it.generalLocation,
                isEmergencyDonor = it.isEmergencyDonor,
                contactPhone = it.contactPhone,
                donationCount = it.donationCount
            )
        }
        dao.insertDonors(entities)
    }

    companion object {
        fun CachedRequestEntity.toBloodRequest(): BloodRequest = BloodRequest(
            id = id,
            patientName = patientName,
            bloodGroup = BloodGroup.fromDisplay(bloodGroup),
            unitsRequired = unitsRequired,
            hospitalName = hospitalName,
            hospitalAddress = hospitalAddress,
            district = city,
            city = city,
            area = hospitalAddress,
            urgency = runCatching { RequestUrgency.valueOf(urgency) }.getOrDefault(RequestUrgency.HIGH),
            status = runCatching { RequestStatus.valueOf(status) }.getOrDefault(RequestStatus.PENDING),
            contactPhone = contactPhone,
            requiredDateTime = requiredDateTime
        )

        fun CachedDonorEntity.toPublicDonorView(): PublicDonorView = PublicDonorView(
            uid = uid,
            name = name,
            bloodGroup = BloodGroup.fromDisplay(bloodGroup),
            availability = runCatching { DonorAvailability.valueOf(availability) }.getOrDefault(DonorAvailability.AVAILABLE),
            generalLocation = generalLocation,
            area = generalLocation,
            isEmergencyDonor = isEmergencyDonor,
            contactPhone = contactPhone,
            donationCount = donationCount
        )
    }
}
