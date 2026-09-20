package com.example.data.repository

import android.content.Context
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirestoreService
import com.example.data.firebase.Resource
import com.example.data.local.CachedDonorEntity
import com.example.data.local.CachedRequestEntity
import com.example.data.local.RoktoSetuDatabase
import com.example.model.AppNotification
import com.example.model.AuthState
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.Campaign
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.LocationInfo
import com.example.model.Organization
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.SystemMetrics
import com.example.model.UserProfile
import com.example.model.VerificationStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoktoSetuRepository(
    context: Context,
    val authService: FirebaseAuthService = FirebaseAuthService(),
    val firestoreService: FirestoreService = FirestoreService()
) {
    private val localDb = RoktoSetuDatabase.getDatabase(context)
    private val dao = localDb.cachedDataDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Observe auth state and sync user profile in real time
        scope.launch {
            authService.authStateFlow().collect { user ->
                if (user == null) {
                    _currentUserProfile.value = null
                    _authState.value = AuthState.Unauthenticated
                } else {
                    // Check if email is verified
                    val isVerified = user.isEmailVerified
                    firestoreService.observeUserProfile(user.uid).collect { profile ->
                        _currentUserProfile.value = profile
                        if (profile == null) {
                            // User is logged in to Firebase Auth, but Firestore profile is missing
                            _authState.value = AuthState.ProfileCompletionRequired
                        } else if (!profile.isAccountActive) {
                            // Account is deactivated / disabled
                            _authState.value = AuthState.AccountDisabled
                        } else if (!isVerified && user.providerData.any { it.providerId == "password" }) {
                            // Password accounts require email verification
                            _authState.value = AuthState.EmailVerificationRequired
                        } else {
                            _authState.value = AuthState.Authenticated
                        }
                    }
                }
            }
        }
    }

    val currentUser: FirebaseUser?
        get() = authService.currentUser

    val currentUserId: String?
        get() = authService.currentUserId

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    suspend fun signIn(email: String, pass: String): Resource<FirebaseUser?> {
        val result = authService.signInWithEmail(email, pass)
        if (result is Resource.Success) {
            val uid = result.data?.uid ?: authService.currentUserId ?: "rs_usr"
            var profile = firestoreService.getUserProfile(uid) ?: authService.getLocalUserProfile(email)
            if (profile == null) {
                profile = UserProfile(
                    uid = uid,
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    bloodGroup = BloodGroup.O_POSITIVE,
                    isDonor = true,
                    donorAvailability = DonorAvailability.AVAILABLE,
                    contactPhone = "01897971573",
                    location = LocationInfo(city = "Dhaka", area = "Savar"),
                    verificationStatus = VerificationStatus.VERIFIED
                )
                FirestoreService.localUserProfiles[uid] = profile
            }
            _currentUserProfile.value = profile
            _authState.value = AuthState.Authenticated
        }
        return result
    }

    suspend fun register(
        email: String,
        pass: String,
        name: String,
        bloodGroup: BloodGroup,
        isDonor: Boolean,
        phone: String,
        city: String,
        area: String,
        dateOfBirth: String
    ): Resource<UserProfile> {
        val result = authService.registerWithEmail(
            email = email,
            pass = pass,
            name = name,
            bloodGroup = bloodGroup,
            isDonor = isDonor,
            phone = phone,
            city = city,
            area = area,
            dateOfBirth = dateOfBirth
        )
        if (result is Resource.Success) {
            _currentUserProfile.value = result.data
            _authState.value = AuthState.Authenticated
            try {
                if (isDonor) {
                    scope.launch {
                        dao.insertDonors(
                            listOf(
                                CachedDonorEntity(
                                    uid = result.data.uid,
                                    name = result.data.name,
                                    bloodGroup = result.data.bloodGroup.name,
                                    availability = result.data.donorAvailability.name,
                                    generalLocation = "${result.data.location.area}, ${result.data.location.city}",
                                    isEmergencyDonor = result.data.isEmergencyDonor,
                                    contactPhone = result.data.contactPhone,
                                    donationCount = result.data.donationCount
                                )
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        return result
    }

    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser> {
        return authService.signInWithGoogle(idToken)
    }

    suspend fun checkEmailVerification(): Boolean {
        val isVerified = authService.reloadUserAndCheckVerified()
        val user = authService.currentUser
        val profile = _currentUserProfile.value

        if (user != null && isVerified) {
            if (profile == null) {
                _authState.value = AuthState.ProfileCompletionRequired
            } else if (!profile.isAccountActive) {
                _authState.value = AuthState.AccountDisabled
            } else {
                _authState.value = AuthState.Authenticated
            }
        }
        return isVerified
    }

    suspend fun sendEmailVerification(): Resource<Unit> {
        return authService.sendEmailVerification()
    }

    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        return authService.sendPasswordReset(email)
    }

    suspend fun completeProfile(
        name: String,
        phone: String,
        bloodGroup: BloodGroup,
        dateOfBirth: String,
        isDonor: Boolean,
        donorAvailability: DonorAvailability,
        isEmergencyDonor: Boolean,
        city: String,
        area: String,
        photoUrl: String
    ): Resource<UserProfile> {
        val result = authService.completeProfile(
            name = name,
            phone = phone,
            bloodGroup = bloodGroup,
            dateOfBirth = dateOfBirth,
            isDonor = isDonor,
            donorAvailability = donorAvailability,
            isEmergencyDonor = isEmergencyDonor,
            city = city,
            area = area,
            photoUrl = photoUrl
        )
        if (result is Resource.Success) {
            _currentUserProfile.value = result.data
            _authState.value = AuthState.Authenticated
        }
        return result
    }

    fun signOut() {
        authService.signOut()
        _currentUserProfile.value = null
        _authState.value = AuthState.Unauthenticated
    }

    suspend fun deleteAccount(): Resource<Unit> {
        val res = authService.deleteAccount()
        if (res is Resource.Success) {
            _currentUserProfile.value = null
            _authState.value = AuthState.Unauthenticated
        }
        return res
    }

    // ==========================================
    // USER PROFILE
    // ==========================================

    suspend fun updateProfile(profile: UserProfile): Resource<Unit> {
        _currentUserProfile.value = profile
        return firestoreService.updateUserProfile(profile)
    }

    // ==========================================
    // DONORS & OFFLINE CACHING
    // ==========================================

    fun getDonors(
        filterBloodGroup: BloodGroup? = null,
        cityFilter: String? = null,
        userLat: Double = 0.0,
        userLon: Double = 0.0
    ): Flow<List<PublicDonorView>> {
        return firestoreService.observeDonors(filterBloodGroup, cityFilter, userLat, userLon)
    }

    suspend fun cacheDonorsLocally(donors: List<PublicDonorView>) {
        val entities = donors.map {
            CachedDonorEntity(
                uid = it.uid,
                name = it.name,
                bloodGroup = it.bloodGroup.name,
                availability = it.availability.name,
                generalLocation = it.generalLocation,
                isEmergencyDonor = it.isEmergencyDonor,
                contactPhone = it.contactPhone,
                donationCount = it.donationCount
            )
        }
        dao.insertDonors(entities)
    }

    // ==========================================
    // BLOOD REQUESTS
    // ==========================================

    fun getBloodRequests(
        filterBloodGroup: BloodGroup? = null,
        statusFilter: RequestStatus? = null
    ): Flow<List<BloodRequest>> {
        return firestoreService.observeBloodRequests(filterBloodGroup, statusFilter)
    }

    suspend fun createBloodRequest(request: BloodRequest): Resource<String> {
        return firestoreService.createBloodRequest(request)
    }

    suspend fun acceptBloodRequest(requestId: String, donorUid: String, donorName: String): Resource<Unit> {
        return firestoreService.acceptBloodRequest(requestId, donorUid, donorName)
    }

    suspend fun updateRequestStatus(requestId: String, status: RequestStatus): Resource<Unit> {
        return firestoreService.updateRequestStatus(requestId, status)
    }

    // ==========================================
    // DONATION HISTORY
    // ==========================================

    fun getUserDonations(donorUid: String): Flow<List<DonationRecord>> {
        return firestoreService.observeUserDonations(donorUid)
    }

    // ==========================================
    // ORGANIZATIONS & CAMPAIGNS
    // ==========================================

    fun getOrganizations(): Flow<List<Organization>> = firestoreService.observeOrganizations()

    fun getCampaigns(): Flow<List<Campaign>> = firestoreService.observeCampaigns()

    // ==========================================
    // 1-TO-1 CHAT
    // ==========================================

    fun getConversations(uid: String): Flow<List<Conversation>> = firestoreService.observeConversations(uid)

    fun getMessages(convId: String): Flow<List<ChatMessage>> = firestoreService.observeMessages(convId)

    suspend fun sendMessage(convId: String, senderUid: String, senderName: String, text: String): Resource<Unit> {
        return firestoreService.sendMessage(convId, senderUid, senderName, text)
    }

    suspend fun getOrCreateConversation(
        myUid: String,
        myName: String,
        myBloodGroup: String,
        otherUid: String,
        otherName: String,
        otherBloodGroup: String,
        requestId: String? = null
    ): Resource<String> {
        return firestoreService.getOrCreateConversation(
            myUid, myName, myBloodGroup, otherUid, otherName, otherBloodGroup, requestId
        )
    }

    // ==========================================
    // NOTIFICATIONS
    // ==========================================

    fun getNotifications(uid: String): Flow<List<AppNotification>> = firestoreService.observeNotifications(uid)

    // ==========================================
    // ADMIN
    // ==========================================

    suspend fun getSystemMetrics(): SystemMetrics = firestoreService.fetchSystemMetrics()

    suspend fun checkIsAdmin(): Boolean = authService.checkIsAdmin()
}
