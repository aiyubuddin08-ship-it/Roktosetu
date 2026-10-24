package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.firebase.*
import com.example.data.local.CachedAmbulanceEntity
import com.example.data.local.CachedAnnouncementEntity
import com.example.data.local.CachedBloodBankEntity
import com.example.data.local.CachedBloodCampEntity
import com.example.data.local.CachedDonorEntity
import com.example.data.local.CachedRequestEntity
import com.example.data.local.CachedCampRsvpEntity
import com.example.data.local.DismissedAnnouncementEntity
import com.example.data.local.NetworkMonitor
import com.example.data.local.RoktoSetuDatabase
import com.example.model.AccountStatus
import com.example.model.AmbulanceService
import com.example.model.AuditLogEntry
import com.example.model.AuthState
import com.example.model.BloodBank
import com.example.model.BloodDonationCamp
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.Campaign
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DatabaseProtectionCheck
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.Organization
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.SecurityAlertItem
import com.example.model.SystemAnnouncement
import com.example.model.SystemConfigSettings
import com.example.model.SystemMetrics
import com.example.model.UserProfile
import com.example.model.UserReport
import com.example.model.VerificationStatus
import com.example.model.VerificationTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow

/**
 * Central Repository coordinating Authentication, Database (using the configured
 * `R.string.firestore_database_id` instance), Server Functions, NetworkMonitor, and Room cache.
 */
class RoktoSetuRepository(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreService,
    private val localDb: RoktoSetuDatabase? = null,
    private val functionsService: FunctionsService = FunctionsService(),
    private val networkMonitor: NetworkMonitor? = null
) {
    constructor(context: Context) : this(
        authService = FirebaseAuthService(context.applicationContext),
        firestoreService = FirestoreService(
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        ),
        localDb = RoktoSetuDatabase.getDatabase(context.applicationContext),
        functionsService = FunctionsService(),
        networkMonitor = NetworkMonitor(context.applicationContext)
    )

    constructor(firestore: FirebaseFirestore, auth: FirebaseAuth = FirebaseAuth.getInstance()) : this(
        authService = FirebaseAuthService(auth, firestore),
        firestoreService = FirestoreService(firestore),
        localDb = null,
        functionsService = FunctionsService(),
        networkMonitor = null
    )

    val currentUserProfile: StateFlow<UserProfile?> = authService.currentUserProfile
    val authState: StateFlow<AuthState> = authService.authState
    val isAuthenticated: Boolean get() = authService.isAuthenticated
    val currentUserId: String? get() = authService.currentUserId
    val isOnline: StateFlow<Boolean> = networkMonitor?.isOnline ?: MutableStateFlow(true)

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

    val isEmailVerified: Boolean get() = authService.isEmailVerified

    suspend fun sendPasswordReset(email: String): Resource<Unit> =
        authService.sendPasswordResetEmail(email)

    suspend fun changePassword(currentPass: String, newPass: String): Resource<Unit> =
        authService.changePassword(currentPass, newPass)

    suspend fun updateProfile(profile: UserProfile): Resource<UserProfile> =
        authService.updateProfile(profile)

    suspend fun deleteAccount(reauthPassword: String? = null): Resource<Unit> {
        val result = authService.deleteAccount(reauthPassword)
        if (result is Resource.Success) {
            clearLocalUserCache()
        }
        return result
    }

    suspend fun clearLocalUserCache() {
        localDb?.cachedDataDao()?.clearDonors()
        localDb?.cachedDataDao()?.clearRequests()
        localDb?.cachedDataDao()?.clearAnnouncements()
    }

    fun signOut() {
        authService.signOut()
    }

    // --- Blood Requests & Pagination ---

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

    suspend fun fetchBloodRequestsPage(
        filterBloodGroup: BloodGroup? = null,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        lastSnapshot: DocumentSnapshot? = null,
        pageSize: Long = 25L
    ): Resource<PaginatedResult<BloodRequest>> =
        firestoreService.fetchBloodRequestsPage(
            filterBloodGroup = filterBloodGroup,
            divisionFilter = divisionFilter,
            districtFilter = districtFilter,
            upazilaFilter = upazilaFilter,
            areaQuery = areaQuery,
            lastSnapshot = lastSnapshot,
            pageSize = pageSize
        )

    suspend fun createBloodRequest(request: BloodRequest): Resource<String> {
        val activeUid = request.requesterUid.ifBlank { currentUserId ?: "" }
        val res = firestoreService.createBloodRequest(request.copy(requesterUid = activeUid))
        if (res is Resource.Success && (request.isEmergencyBroadcast || request.urgency == RequestUrgency.CRITICAL_EMERGENCY)) {
            runCatching {
                functionsService.sendEmergencyBroadcast(
                    requestId = res.data,
                    city = request.district.ifBlank { request.city },
                    bloodGroup = request.bloodGroup.display,
                    patientName = request.patientName,
                    hospitalName = request.hospitalName
                )
            }
        }
        return res
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

    // --- Public Donors Search & Pagination ---

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

    suspend fun fetchPublicDonorsPage(
        filterBloodGroup: BloodGroup? = null,
        includeCompatibleForRecipient: Boolean = false,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        onlyAvailable: Boolean = false,
        lastSnapshot: DocumentSnapshot? = null,
        pageSize: Long = 25L
    ): Resource<PaginatedResult<PublicDonorView>> =
        firestoreService.fetchPublicDonorsPage(
            filterBloodGroup = filterBloodGroup,
            divisionFilter = divisionFilter,
            districtFilter = districtFilter,
            upazilaFilter = upazilaFilter,
            areaQuery = areaQuery,
            onlyAvailable = onlyAvailable,
            lastSnapshot = lastSnapshot,
            pageSize = pageSize
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

    // --- Phase 4: Blood Banks, Ambulances & Blood Camps ---

    fun getBloodBanks(
        includeInactive: Boolean = false,
        onError: ((String) -> Unit)? = null
    ): Flow<List<BloodBank>> =
        firestoreService.observeBloodBanks(includeInactive, onError)

    fun getAmbulances(
        includeInactive: Boolean = false,
        onError: ((String) -> Unit)? = null
    ): Flow<List<AmbulanceService>> =
        firestoreService.observeAmbulances(includeInactive, onError)

    fun getBloodCamps(
        includeInactive: Boolean = false,
        onError: ((String) -> Unit)? = null
    ): Flow<List<BloodDonationCamp>> =
        firestoreService.observeBloodCamps(includeInactive, currentUserId, onError)

    suspend fun toggleCampRsvp(campId: String, currentlyRsvped: Boolean): Resource<Boolean> {
        val uid = currentUserId ?: return Resource.Error("ক্যাম্পে নিবন্ধন করতে অনুগ্রহ করে লগইন করুন।")
        val name = currentUserProfile.value?.name ?: "রক্তদাতা"
        val res = firestoreService.toggleCampRsvpInFirestore(campId, uid, name, currentlyRsvped)
        if (res is Resource.Success) {
            if (res.data) {
                localDb?.cachedDataDao()?.insertCampRsvps(
                    listOf(CachedCampRsvpEntity(campId = campId, uid = uid))
                )
            } else {
                localDb?.cachedDataDao()?.deleteCampRsvp(campId, uid)
            }
        }
        return res
    }

    suspend fun saveBloodBankAsAdmin(bank: BloodBank): Resource<BloodBank> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.saveBloodBankAsAdmin(bank, adminUid, adminEmail)
    }

    suspend fun deleteBloodBankAsAdmin(bankId: String, bankName: String = ""): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.deleteBloodBankAsAdmin(bankId, adminUid, adminEmail)
    }

    suspend fun saveAmbulanceAsAdmin(ambulance: AmbulanceService): Resource<AmbulanceService> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.saveAmbulanceAsAdmin(ambulance, adminUid, adminEmail)
    }

    suspend fun deleteAmbulanceAsAdmin(ambulanceId: String, title: String = ""): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.deleteAmbulanceAsAdmin(ambulanceId, adminUid, adminEmail)
    }

    suspend fun saveBloodCampAsAdmin(camp: BloodDonationCamp): Resource<BloodDonationCamp> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.saveBloodCampAsAdmin(camp, adminUid, adminEmail)
    }

    suspend fun deleteBloodCampAsAdmin(campId: String, title: String = ""): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.deleteBloodCampAsAdmin(campId, adminUid, adminEmail)
    }

    suspend fun importDefaultDirectoryDataAsAdmin(): Resource<Int> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.importDefaultDirectoryDataAsAdmin(adminUid, adminEmail)
    }

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
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
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
        adminEmail: String,
        rejectionReason: String = "",
        notes: String = ""
    ): Resource<Unit> =
        firestoreService.reviewVerificationTaskAsAdmin(task, approved, adminUid, adminEmail, rejectionReason, notes)

    suspend fun reviewVerificationTask(
        task: VerificationTask,
        approve: Boolean,
        reason: String = "",
        notes: String = ""
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        val fnRes = functionsService.reviewVerificationRequestServerSide(
            verificationRequestId = task.id,
            decision = if (approve) "APPROVED" else "REJECTED",
            reviewNotes = if (approve) notes else reason
        )
        if (fnRes is Resource.Success) {
            return Resource.Success(Unit)
        }
        return firestoreService.reviewVerificationTaskAsAdmin(task, approve, adminUid, adminEmail, reason, notes)
    }

    suspend fun verifyAdminStatusDetailed(forceRefreshToken: Boolean = true): AdminVerificationResult {
        val res = authService.verifyAdminStatusDetailed(forceRefreshToken)
        if (res.isVerifiedAdmin) {
            val method = buildList {
                if (res.hasAdminCustomClaim) add("অ্যাডমিন অনুমতি নিশ্চিত")
                if (res.hasAdminRegistryDoc) add("প্রশাসক তালিকা নিশ্চিত")
            }.joinToString(", ")
            firestoreService.recordAdminAccessCheck(res.uid, res.email, true, method)
        }
        return res
    }

    fun getAllVerificationTasks(onError: ((String) -> Unit)? = null): Flow<List<VerificationTask>> =
        firestoreService.observeAllVerificationTasks(onError)

    fun getAllUsersForAdmin(onError: ((String) -> Unit)? = null): Flow<List<UserProfile>> =
        firestoreService.observeAllUsersForAdmin(onError)

    suspend fun updateUserAccountStatusAsAdmin(
        targetUser: UserProfile,
        newStatus: AccountStatus,
        reason: String
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.updateUserAccountStatusAsAdmin(targetUser, newStatus, reason, adminUid, adminEmail)
    }

    suspend fun updateDonorStatusAsAdmin(
        targetUser: UserProfile,
        newAvailability: DonorAvailability,
        newVerificationStatus: VerificationStatus,
        notes: String
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.updateDonorStatusAsAdmin(targetUser, newAvailability, newVerificationStatus, notes, adminUid, adminEmail)
    }

    fun getAllBloodRequestsForAdmin(onError: ((String) -> Unit)? = null): Flow<List<BloodRequest>> =
        firestoreService.observeAllBloodRequestsForAdmin(onError)

    suspend fun updateBloodRequestAsAdmin(
        request: BloodRequest,
        newStatus: RequestStatus,
        adminNotes: String
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.updateBloodRequestAsAdmin(request, newStatus, adminNotes, adminUid, adminEmail)
    }

    suspend fun expireStaleRequestsAsAdmin(): Resource<Int> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.expireStaleRequestsAsAdmin(adminUid, adminEmail)
    }

    suspend fun dispatchEmergencyBroadcastAsAdmin(request: BloodRequest): Resource<Int> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        runCatching {
            functionsService.sendEmergencyBroadcast(
                requestId = request.id,
                city = request.district.ifBlank { request.city },
                bloodGroup = request.bloodGroup.display,
                patientName = request.patientName,
                hospitalName = request.hospitalName
            )
        }
        return firestoreService.dispatchEmergencyBroadcastAsAdmin(request, adminUid, adminEmail)
    }

    suspend fun verifyOrganizationDirectAsAdmin(
        org: Organization,
        verified: Boolean,
        reason: String
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.verifyOrganizationDirectAsAdmin(org, verified, reason, adminUid, adminEmail)
    }

    fun getAllDonationsForAdmin(onError: ((String) -> Unit)? = null): Flow<List<DonationRecord>> =
        firestoreService.observeAllDonationsForAdmin(onError)

    suspend fun verifyAndRecordDonation(record: DonationRecord): Resource<DonationRecord> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.verifyAndRecordDonationAsAdmin(record, adminUid, adminEmail)
    }

    // --- Phase 3: User Reports & Admin Report Review ---

    fun getReportsForAdmin(onError: ((String) -> Unit)? = null): Flow<List<UserReport>> =
        firestoreService.observeReportsForAdmin(onError)

    fun getMyReports(onError: ((String) -> Unit)? = null): Flow<List<UserReport>> {
        val uid = currentUserId ?: return emptyFlow()
        return firestoreService.observeMyReports(uid, onError)
    }

    suspend fun submitUserReport(
        targetUid: String,
        targetName: String,
        targetType: String,
        category: String,
        reasonBn: String,
        details: String,
        severity: String = "MEDIUM"
    ): Resource<String> {
        val uid = currentUserId ?: return Resource.Error("অভিযোগ জানাতে অনুগ্রহ করে লগইন করুন।")
        val name = currentUserProfile.value?.name ?: "ব্যবহারকারী"
        return firestoreService.submitUserReport(
            reporterUid = uid,
            reporterName = name,
            targetUid = targetUid,
            targetName = targetName,
            targetType = targetType,
            category = category,
            reasonBn = reasonBn,
            details = details,
            severity = severity
        )
    }

    suspend fun reviewReportAsAdmin(
        report: UserReport,
        newStatus: String,
        resolutionNotes: String,
        actionTaken: String
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.reviewReportAsAdmin(report, newStatus, resolutionNotes, actionTaken, adminUid, adminEmail)
    }

    fun getAnnouncementsForAdmin(onError: ((String) -> Unit)? = null): Flow<List<SystemAnnouncement>> =
        firestoreService.observeAnnouncementsForAdmin(onError)

    fun getPublishedAnnouncements(onError: ((String) -> Unit)? = null): Flow<List<SystemAnnouncement>> =
        firestoreService.observePublishedAnnouncements(onError)

    suspend fun saveOrPublishAnnouncementAsAdmin(
        titleBn: String,
        bodyBn: String,
        category: String,
        targetAudience: String,
        targetDivision: String,
        targetDistrict: String,
        publishNow: Boolean
    ): Resource<Int> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.saveOrPublishAnnouncementAsAdmin(
            titleBn, bodyBn, category, targetAudience, targetDivision, targetDistrict, publishNow, adminUid, adminEmail
        )
    }

    suspend fun getSystemConfigSettings(): SystemConfigSettings =
        firestoreService.fetchSystemConfigSettings()

    suspend fun updateSystemConfigSettingsAsAdmin(settings: SystemConfigSettings): Resource<SystemConfigSettings> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.updateSystemConfigSettingsAsAdmin(settings, adminUid, adminEmail)
    }

    fun getSecurityAlertsForAdmin(onError: ((String) -> Unit)? = null): Flow<List<SecurityAlertItem>> =
        firestoreService.observeSecurityAlertsForAdmin(onError)

    suspend fun reviewSecurityAlertAsAdmin(
        alert: SecurityAlertItem,
        resolutionNotes: String
    ): Resource<Unit> {
        val adminUid = currentUserId ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")
        val adminEmail = currentUserProfile.value?.email ?: "admin@roktosetu.org"
        return firestoreService.reviewSecurityAlertAsAdmin(alert, resolutionNotes, adminUid, adminEmail)
    }

    suspend fun getAdminRegistryList(): List<Map<String, Any>> =
        firestoreService.fetchAdminRegistryList()

    suspend fun runDatabaseProtectionChecks(isVerifiedAdmin: Boolean): List<DatabaseProtectionCheck> =
        firestoreService.runDatabaseProtectionChecks(currentUserId, isVerifiedAdmin)

    // --- Offline Room Cache & Local Announcement Dismissal ---

    private val inMemoryDismissedIds = MutableStateFlow<Set<String>>(emptySet())

    fun getCachedRequests(): Flow<List<CachedRequestEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedRequests()

    fun getCachedDonors(): Flow<List<CachedDonorEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedDonors()

    fun getCachedAnnouncements(): Flow<List<CachedAnnouncementEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedAnnouncements()

    fun getCachedBloodBanks(): Flow<List<CachedBloodBankEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedBloodBanks()

    fun getCachedAmbulances(): Flow<List<CachedAmbulanceEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedAmbulances()

    fun getCachedBloodCamps(): Flow<List<CachedBloodCampEntity>>? =
        localDb?.cachedDataDao()?.getAllCachedBloodCamps()

    fun getDismissedAnnouncementIds(): Flow<Set<String>> {
        val daoFlow = localDb?.cachedDataDao()?.getDismissedAnnouncementIds()
        return if (daoFlow != null) {
            combine(daoFlow, inMemoryDismissedIds) { dbList, memSet ->
                dbList.toSet() + memSet
            }
        } else {
            inMemoryDismissedIds
        }
    }

    suspend fun dismissAnnouncement(announcementId: String) {
        if (announcementId.isBlank()) return
        inMemoryDismissedIds.value = inMemoryDismissedIds.value + announcementId
        localDb?.cachedDataDao()?.insertDismissedAnnouncement(
            DismissedAnnouncementEntity(announcementId = announcementId)
        )
    }

    suspend fun cacheAnnouncementsLocally(announcements: List<SystemAnnouncement>) {
        val dao = localDb?.cachedDataDao() ?: return
        val entities = announcements.map {
            CachedAnnouncementEntity(
                id = it.id,
                titleBn = it.titleBn,
                bodyBn = it.bodyBn,
                category = it.category,
                targetAudience = it.targetAudience,
                targetDivision = it.targetDivision,
                targetDistrict = it.targetDistrict,
                status = it.status,
                createdByUid = it.createdByUid,
                createdByEmail = it.createdByEmail,
                createdAt = it.createdAt,
                publishedAt = it.publishedAt,
                recipientCount = it.recipientCount
            )
        }
        dao.clearAnnouncements()
        if (entities.isNotEmpty()) {
            dao.insertAnnouncements(entities)
        }
    }

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

    suspend fun cacheBloodBanksLocally(banks: List<BloodBank>) {
        val dao = localDb?.cachedDataDao() ?: return
        val entities = banks.map {
            CachedBloodBankEntity(
                id = it.id,
                name = it.name,
                nameBn = it.nameBn,
                organization = it.organization,
                address = it.address,
                city = it.city,
                district = it.district.ifBlank { it.city },
                phone = it.phone,
                emergencyHotline = it.emergencyHotline,
                is24Hours = it.is24Hours,
                availableGroupsCsv = it.availableGroups.joinToString(",") { bg -> bg.display },
                verified = it.verified,
                isActive = it.isActive,
                cachedAt = it.updatedAt
            )
        }
        dao.clearBloodBanks()
        if (entities.isNotEmpty()) {
            dao.insertBloodBanks(entities)
        }
    }

    suspend fun cacheAmbulancesLocally(ambulances: List<AmbulanceService>) {
        val dao = localDb?.cachedDataDao() ?: return
        val entities = ambulances.map {
            CachedAmbulanceEntity(
                id = it.id,
                title = it.title,
                titleBn = it.titleBn,
                provider = it.provider,
                coverageArea = it.coverageArea,
                district = it.district,
                city = it.city.ifBlank { it.district },
                phone = it.phone,
                type = it.type,
                isAvailable24x7 = it.isAvailable24x7,
                isActive = it.isActive,
                cachedAt = it.updatedAt
            )
        }
        dao.clearAmbulances()
        if (entities.isNotEmpty()) {
            dao.insertAmbulances(entities)
        }
    }

    suspend fun cacheBloodCampsLocally(camps: List<BloodDonationCamp>) {
        val dao = localDb?.cachedDataDao() ?: return
        val entities = camps.map {
            CachedBloodCampEntity(
                id = it.id,
                title = it.title,
                titleBn = it.titleBn,
                organizer = it.organizer,
                location = it.location,
                district = it.district,
                city = it.city.ifBlank { it.district },
                date = it.date,
                time = it.time,
                contactPhone = it.contactPhone,
                targetUnits = it.targetUnits,
                registeredCount = it.registeredCount,
                isUserRsvp = it.isUserRsvp,
                description = it.description,
                isActive = it.isActive,
                cachedAt = it.updatedAt
            )
        }
        dao.clearBloodCamps()
        if (entities.isNotEmpty()) {
            dao.insertBloodCamps(entities)
        }
    }

    companion object {
        fun CachedAnnouncementEntity.toSystemAnnouncement(): SystemAnnouncement = SystemAnnouncement(
            id = id,
            titleBn = titleBn,
            bodyBn = bodyBn,
            category = category,
            targetAudience = targetAudience,
            targetDivision = targetDivision,
            targetDistrict = targetDistrict,
            status = status,
            createdByUid = createdByUid,
            createdByEmail = createdByEmail,
            createdAt = createdAt,
            publishedAt = publishedAt,
            recipientCount = recipientCount
        )

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

        fun CachedBloodBankEntity.toBloodBank(): BloodBank = BloodBank(
            id = id,
            name = name,
            nameBn = nameBn,
            organization = organization,
            address = address,
            city = city,
            district = district,
            phone = phone,
            emergencyHotline = emergencyHotline,
            is24Hours = is24Hours,
            availableGroups = availableGroupsCsv.split(",").filter { it.isNotBlank() }.map { BloodGroup.fromDisplay(it) },
            verified = verified,
            isActive = isActive,
            updatedAt = cachedAt
        )

        fun CachedAmbulanceEntity.toAmbulanceService(): AmbulanceService = AmbulanceService(
            id = id,
            title = title,
            titleBn = titleBn,
            provider = provider,
            coverageArea = coverageArea,
            district = district,
            city = city,
            phone = phone,
            type = type,
            isAvailable24x7 = isAvailable24x7,
            isActive = isActive,
            updatedAt = cachedAt
        )

        fun CachedBloodCampEntity.toBloodDonationCamp(): BloodDonationCamp = BloodDonationCamp(
            id = id,
            title = title,
            titleBn = titleBn,
            organizer = organizer,
            location = location,
            district = district,
            city = city,
            date = date,
            time = time,
            contactPhone = contactPhone,
            targetUnits = targetUnits,
            registeredCount = registeredCount,
            isUserRsvp = isUserRsvp,
            description = description,
            isActive = isActive,
            updatedAt = cachedAt
        )
    }
}
