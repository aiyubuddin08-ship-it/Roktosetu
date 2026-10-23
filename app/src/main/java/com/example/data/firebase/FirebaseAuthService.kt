package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.domain.LocationHelper
import com.example.model.AuthState
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.model.LocationInfo
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VerificationStatus
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Production Authentication & Profile Lifecycle Service.
 *
 * Security & Integrity Guarantees:
 * - Relies exclusively on `FirebaseAuth.currentUser` as the single source of session truth.
 * - Never stores passwords, hashes, or fake sessions locally in SharedPreferences or memory.
 * - Uses real Firebase Auth (`createUserWithEmailAndPassword`, `signInWithEmailAndPassword`,
 *   `sendEmailVerification`, `sendPasswordResetEmail`) and Google Sign-In (`GoogleAuthProvider`).
 * - Determines administrator privileges strictly via Firebase Auth Custom Claim (`admin == true`)
 *   or existence of `/admins/{uid}` in the custom Firestore database.
 * - Never assigns VERIFIED, SUPER_ADMIN, or privileged statuses to newly registered user accounts.
 */
class FirebaseAuthService(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: SharedPreferences? = null
) {
    constructor(context: Context) : this(
        auth = FirebaseAuth.getInstance(),
        firestore = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        prefs = context.applicationContext.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            _currentUserProfile.value = null
            _authState.value = AuthState.Unauthenticated
        } else {
            serviceScope.launch {
                syncUserSession(user)
            }
        }
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAuthenticated: Boolean
        get() = auth.currentUser != null && _authState.value is AuthState.Authenticated

    init {
        // Clean up any legacy local session keys if present from older versions
        prefs?.edit()?.clear()?.apply()
        auth.addAuthStateListener(authStateListener)
    }

    /**
     * Checks whether a FirebaseUser authenticated via Email/Password still needs to verify their email.
     */
    private fun requiresEmailVerification(user: FirebaseUser): Boolean {
        val hasGoogleProvider = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        val hasPasswordProvider = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        return hasPasswordProvider && !hasGoogleProvider && !user.isEmailVerified
    }

    /**
     * Synchronizes the current FirebaseUser session with `/users/{uid}` in Firestore.
     */
    suspend fun syncUserSession(firebaseUser: FirebaseUser? = auth.currentUser) {
        if (firebaseUser == null) {
            _currentUserProfile.value = null
            _authState.value = AuthState.Unauthenticated
            return
        }

        try {
            try {
                firebaseUser.reload().await()
            } catch (e: FirebaseAuthInvalidUserException) {
                throw e
            } catch (_: Exception) {
                // Keep cached FirebaseUser token if device is temporarily offline during cold start
            }

            val refreshedUser = auth.currentUser ?: firebaseUser
            val profile = fetchUserProfileFromFirestore(refreshedUser.uid, forceRefreshToken = true)

            if (requiresEmailVerification(refreshedUser)) {
                _currentUserProfile.value = profile ?: UserProfile(
                    uid = refreshedUser.uid,
                    name = refreshedUser.displayName ?: "",
                    email = refreshedUser.email ?: ""
                )
                _authState.value = AuthState.EmailVerificationRequired
                return
            }

            if (profile == null || profile.contactPhone.isBlank()) {
                _currentUserProfile.value = profile ?: UserProfile(
                    uid = refreshedUser.uid,
                    name = refreshedUser.displayName ?: "",
                    email = refreshedUser.email ?: "",
                    photoUrl = refreshedUser.photoUrl?.toString() ?: ""
                )
                _authState.value = AuthState.ProfileCompletionRequired
                return
            }

            _currentUserProfile.value = profile
            _authState.value = AuthState.Authenticated
            syncFcmToken(refreshedUser.uid, profile.fcmToken)
        } catch (e: FirebaseAuthInvalidUserException) {
            if (e.errorCode == "ERROR_USER_DISABLED") {
                _authState.value = AuthState.AccountDisabled
            } else {
                auth.signOut()
                _currentUserProfile.value = null
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            val cached = _currentUserProfile.value
            if (cached != null && cached.uid == firebaseUser.uid) {
                _authState.value = if (requiresEmailVerification(firebaseUser)) {
                    AuthState.EmailVerificationRequired
                } else {
                    AuthState.Authenticated
                }
            } else {
                _authState.value = AuthState.Error(mapFirebaseException(e))
            }
        }
    }

    /**
     * Fetches the current device FCM registration token and persists it to `/users/{uid}.fcmToken`
     * if it differs from the stored token.
     */
    suspend fun syncFcmToken(uid: String = currentUserId ?: "", existingToken: String? = null) {
        if (uid.isBlank()) return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank() && token != existingToken) {
                firestore.collection("users").document(uid)
                    .update(
                        mapOf(
                            "fcmToken" to token.take(500),
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                _currentUserProfile.value = _currentUserProfile.value?.copy(fcmToken = token.take(500))
            }
        } catch (_: Exception) {
            // Non-blocking: token will also be synced on next onNewToken callback
        }
    }

    /**
     * Checks whether the currently authenticated Firebase Auth user has administrator privileges
     * strictly via Custom Claim (`admin == true`) after refreshing the ID token, or via
     * `/admins/{uid}` document existence in Firestore.
     */
    suspend fun isCurrentUserAdmin(forceRefreshToken: Boolean = true): Boolean {
        val user = auth.currentUser ?: return false
        val hasAdminClaim = try {
            val tokenResult = user.getIdToken(forceRefreshToken).await()
            tokenResult.claims["admin"] == true
        } catch (_: Exception) {
            false
        }
        if (hasAdminClaim) return true

        return try {
            firestore.collection("admins").document(user.uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Authenticates a registered user account via Firebase Authentication (`signInWithEmailAndPassword`).
     */
    suspend fun signInWithEmail(email: String, pass: String): Resource<UserProfile> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.isBlank()) {
            return Resource.Error("ইমেইল এবং পাসওয়ার্ড খালি রাখা যাবে না (Email and password must not be empty).")
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
            val user = authResult.user
                ?: return Resource.Error("লগইন ব্যর্থ হয়েছে: ইউজার সেশন পাওয়া যায়নি (Sign-in failed: No user returned).")

            syncUserSession(user)

            if (_authState.value is AuthState.AccountDisabled) {
                return Resource.Error("এই অ্যাকাউন্টটি অ্যাডমিন কর্তৃক নিষ্ক্রিয় করা হয়েছে (This account has been disabled by an administrator).")
            }

            val resolvedProfile = _currentUserProfile.value ?: UserProfile(
                uid = user.uid,
                name = user.displayName ?: "",
                email = user.email ?: cleanEmail
            )
            Resource.Success(resolvedProfile)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e), e)
        }
    }

    /**
     * Registers a new unprivileged user account (`role = USER`, `verificationStatus = UNVERIFIED`,
     * `donationCount = 0`, `isEmergencyDonor = false`) using real Firebase Auth and atomic Firestore batch.
     */
    suspend fun registerWithEmail(
        email: String,
        pass: String,
        name: String,
        bloodGroup: BloodGroup,
        isDonor: Boolean,
        phone: String,
        division: String,
        district: String,
        upazila: String,
        area: String,
        dateOfBirth: String = ""
    ): Resource<UserProfile> {
        val cleanEmail = email.trim()
        val cleanName = name.trim()
        val cleanPhone = phone.trim()

        if (cleanEmail.isBlank() || pass.isBlank() || cleanName.isBlank()) {
            return Resource.Error("নাম, ইমেইল এবং পাসওয়ার্ড আবশ্যক (Name, email, and password are required).")
        }

        val validationError = validatePasswordRules(pass)
        if (validationError != null) {
            return Resource.Error(validationError)
        }

        val user = try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
            authResult.user ?: return Resource.Error("অ্যাকাউন্ট তৈরি ব্যর্থ হয়েছে (Registration failed: No user returned).")
        } catch (e: Exception) {
            return Resource.Error(mapFirebaseException(e), e)
        }

        try {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
            user.updateProfile(profileUpdates).await()
        } catch (_: Exception) {
            // Non-fatal if displayName metadata update fails; primary name is stored in Firestore
        }

        try {
            user.sendEmailVerification().await()
        } catch (_: Exception) {
            // User can resend verification email from EmailVerificationScreen
        }

        val effectiveDistrict = district.trim().ifBlank { "Dhaka" }
        val effectiveDivision = division.trim().ifBlank {
            LocationHelper.inferDivisionForDistrict(effectiveDistrict)
        }
        val now = System.currentTimeMillis()

        // Newly created accounts NEVER receive verified, admin, or emergency donor privileges
        val newProfile = UserProfile(
            uid = user.uid,
            name = cleanName,
            email = cleanEmail,
            bloodGroup = bloodGroup,
            dateOfBirth = dateOfBirth.trim(),
            isDonor = isDonor,
            donorAvailability = if (isDonor) DonorAvailability.AVAILABLE else DonorAvailability.UNAVAILABLE,
            isEmergencyDonor = false,
            contactPhone = cleanPhone,
            showPhonePublicly = false,
            location = LocationInfo(
                division = effectiveDivision,
                district = effectiveDistrict,
                upazila = upazila.trim(),
                area = area.trim(),
                city = effectiveDistrict
            ),
            verificationStatus = VerificationStatus.UNVERIFIED,
            role = UserRole.USER,
            donationCount = 0,
            createdAt = now,
            updatedAt = now
        )

        return try {
            saveUserProfileAndPublicDonor(newProfile, isCreate = true)
            _currentUserProfile.value = newProfile
            _authState.value = if (requiresEmailVerification(user)) {
                AuthState.EmailVerificationRequired
            } else {
                AuthState.Authenticated
            }
            Resource.Success(newProfile)
        } catch (e: Exception) {
            _currentUserProfile.value = newProfile
            _authState.value = AuthState.ProfileCompletionRequired
            Resource.Error(
                "অ্যাকাউন্ট তৈরি হয়েছে কিন্তু প্রোফাইল সংরক্ষণ ব্যর্থ হয়েছে। অনুগ্রহ করে প্রোফাইল তথ্য পুনরায় সংরক্ষণ করুন। (${mapFirebaseException(e)})",
                e
            )
        }
    }

    /**
     * Authenticates a user with a Google ID Token obtained via Jetpack CredentialManager.
     */
    suspend fun signInWithGoogleCredential(idToken: String): Resource<UserProfile> {
        if (idToken.isBlank()) {
            return Resource.Error("Google সাইন-ইন টোকেন পাওয়া যায়নি (Invalid Google ID token).")
        }
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Resource.Error("Google Sign-In failed: No user returned.")
            val existing = fetchUserProfileFromFirestore(user.uid, forceRefreshToken = true)
            if (existing != null && existing.contactPhone.isNotBlank()) {
                _currentUserProfile.value = existing
                _authState.value = AuthState.Authenticated
                Resource.Success(existing)
            } else {
                val partialProfile = existing ?: UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    verificationStatus = VerificationStatus.UNVERIFIED,
                    role = UserRole.USER,
                    donationCount = 0
                )
                _currentUserProfile.value = partialProfile
                _authState.value = AuthState.ProfileCompletionRequired
                Resource.Success(partialProfile)
            }
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e), e)
        }
    }

    /**
     * Completes profile setup for newly authenticated users (e.g., after Google Sign-In or failed initial Firestore write).
     */
    suspend fun completeUserProfile(
        name: String,
        phone: String,
        bloodGroup: BloodGroup,
        dateOfBirth: String,
        isDonor: Boolean,
        donorAvailability: DonorAvailability,
        isEmergencyDonor: Boolean,
        division: String,
        district: String,
        upazila: String,
        area: String,
        photoUrl: String = ""
    ): Resource<UserProfile> {
        val currentUser = auth.currentUser
            ?: return Resource.Error("আপনি লগইন করা নেই। অনুগ্রহ করে পুনরায় লগইন করুন (Not authenticated. Please sign in again).")

        val existing = fetchUserProfileFromFirestore(currentUser.uid)

        val effectiveDistrict = district.trim().ifBlank { "Dhaka" }
        val effectiveDivision = division.trim().ifBlank {
            LocationHelper.inferDivisionForDistrict(effectiveDistrict)
        }
        val now = System.currentTimeMillis()

        val completedProfile = UserProfile(
            uid = currentUser.uid,
            name = name.trim(),
            email = currentUser.email ?: existing?.email ?: "",
            bloodGroup = bloodGroup,
            dateOfBirth = dateOfBirth.trim(),
            isDonor = isDonor,
            donorAvailability = donorAvailability,
            lastDonationDate = existing?.lastDonationDate,
            isEmergencyDonor = if (existing != null) isEmergencyDonor else false,
            contactPhone = phone.trim(),
            showPhonePublicly = existing?.showPhonePublicly ?: false,
            location = LocationInfo(
                division = effectiveDivision,
                district = effectiveDistrict,
                upazila = upazila.trim(),
                area = area.trim(),
                city = effectiveDistrict
            ),
            verificationStatus = existing?.verificationStatus ?: VerificationStatus.UNVERIFIED,
            role = existing?.role ?: UserRole.USER,
            organizationId = existing?.organizationId,
            donationCount = existing?.donationCount ?: 0,
            badges = existing?.badges ?: emptyList(),
            photoUrl = photoUrl.ifBlank { currentUser.photoUrl?.toString() ?: "" },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        return try {
            saveUserProfileAndPublicDonor(completedProfile, isCreate = (existing == null))
            _currentUserProfile.value = completedProfile
            _authState.value = if (requiresEmailVerification(currentUser)) {
                AuthState.EmailVerificationRequired
            } else {
                AuthState.Authenticated
            }
            Resource.Success(completedProfile)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e), e)
        }
    }

    suspend fun sendEmailVerification(): Resource<Unit> {
        val user = auth.currentUser
            ?: return Resource.Error("কোনো লগইন করা ইউজার পাওয়া যায়নি (No signed-in user found).")
        return try {
            user.sendEmailVerification().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e), e)
        }
    }

    suspend fun checkEmailVerification(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.reload().await()
            val refreshed = auth.currentUser ?: user
            if (refreshed.isEmailVerified) {
                syncUserSession(refreshed)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sends a password reset email via Firebase Auth.
     * Prevents user enumeration by returning `Resource.Success` even when `ERROR_USER_NOT_FOUND` occurs.
     */
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Resource.Error("অনুগ্রহ করে একটি সঠিক ইমেইল ঠিকানা দিন (Please enter a valid email address).")
        }
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Resource.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            // Return Success to prevent user enumeration attacks
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e), e)
        }
    }

    suspend fun updateProfile(updated: UserProfile): Resource<UserProfile> {
        val user = auth.currentUser
            ?: return Resource.Error("প্রোফাইল আপডেট করতে লগইন করা আবশ্যক (You must be signed in to update your profile).")
        if (user.uid != updated.uid) {
            return Resource.Error("অননুমোদিত প্রোফাইল আপডেট প্রচেষ্টা (Unauthorized profile update attempt).")
        }
        return try {
            val timestamped = updated.copy(updatedAt = System.currentTimeMillis())
            saveUserProfileAndPublicDonor(timestamped, isCreate = false)
            _currentUserProfile.value = timestamped
            Resource.Success(timestamped)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e), e)
        }
    }

    /**
     * Atomically writes the private user document in `/users/{uid}` and the sanitized
     * public donor record in `/public_donors/{uid}` (or removes `/public_donors/{uid}` if not a donor).
     */
    private suspend fun saveUserProfileAndPublicDonor(profile: UserProfile, isCreate: Boolean) {
        val userRef = firestore.collection("users").document(profile.uid)
        val publicDonorRef = firestore.collection("public_donors").document(profile.uid)
        val opType = if (isCreate) OperationType.CREATE else OperationType.UPDATE

        try {
            val batch = firestore.batch()
            batch.set(userRef, profile.toFirestoreMap())
            if (profile.isDonor) {
                val publicView = profile.toPublicDonorView()
                batch.set(publicDonorRef, publicView.toFirestoreMap(isDonorFlag = true))
            } else {
                batch.delete(publicDonorRef)
            }
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, opType, userRef.path)
            throw e
        }
    }

    private suspend fun fetchUserProfileFromFirestore(
        uid: String,
        forceRefreshToken: Boolean = false
    ): UserProfile? {
        val docRef = firestore.collection("users").document(uid)
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) return null

            val locMap = doc.get("location") as? Map<*, *>
            val district = doc.getString("district")
                ?: (locMap?.get("district") as? String)
                ?: doc.getString("city")
                ?: (locMap?.get("city") as? String)
                ?: "Dhaka"
            val division = doc.getString("division")
                ?: (locMap?.get("division") as? String)
                ?: LocationHelper.inferDivisionForDistrict(district)
            val upazila = doc.getString("upazila")
                ?: (locMap?.get("upazila") as? String)
                ?: ""
            val area = doc.getString("area")
                ?: (locMap?.get("area") as? String)
                ?: ""

            val docEmail = doc.getString("email") ?: auth.currentUser?.email ?: ""
            val isServerAdmin = isCurrentUserAdmin(forceRefreshToken = forceRefreshToken)

            val storedRole = runCatching {
                UserRole.valueOf(doc.getString("role") ?: "USER")
            }.getOrDefault(UserRole.USER)

            UserProfile(
                uid = uid,
                name = doc.getString("name") ?: "",
                email = docEmail,
                bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE"),
                dateOfBirth = doc.getString("dateOfBirth") ?: "",
                isDonor = doc.getBoolean("isDonor") ?: true,
                donorAvailability = runCatching {
                    DonorAvailability.valueOf(doc.getString("donorAvailability") ?: "AVAILABLE")
                }.getOrDefault(DonorAvailability.AVAILABLE),
                lastDonationDate = doc.getLong("lastDonationDate"),
                isEmergencyDonor = doc.getBoolean("isEmergencyDonor") ?: false,
                contactPhone = doc.getString("contactPhone") ?: "",
                showPhonePublicly = doc.getBoolean("showPhonePublicly") ?: false,
                location = LocationInfo(
                    division = division,
                    district = district,
                    upazila = upazila,
                    area = area,
                    city = district
                ),
                verificationStatus = if (isServerAdmin) {
                    VerificationStatus.VERIFIED
                } else {
                    runCatching {
                        VerificationStatus.valueOf(doc.getString("verificationStatus") ?: "UNVERIFIED")
                    }.getOrDefault(VerificationStatus.UNVERIFIED)
                },
                role = if (isServerAdmin) UserRole.SUPER_ADMIN else storedRole,
                organizationId = doc.getString("organizationId"),
                donationCount = (doc.getLong("donationCount") ?: 0L).toInt(),
                badges = (doc.get("badges") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                photoUrl = doc.getString("photoUrl") ?: "",
                fcmToken = doc.getString("fcmToken") ?: "",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            null
        }
    }

    /**
     * Deletes the user's notifications (`/users/{uid}/notifications/{notificationId}`), public donor listing (`/public_donors/{uid}`),
     * private profile (`/users/{uid}`), and Firebase Auth user account.
     * Supports password re-authentication when required by Firebase security policy.
     */
    suspend fun deleteAccount(reauthPassword: String? = null): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("অ্যাকাউন্ট মুছে ফেলতে লগইন করা আবশ্যক (Not signed in).")
        val uid = user.uid

        return try {
            if (!reauthPassword.isNullOrBlank()) {
                val userEmail = user.email
                    ?: return Resource.Error("পুনরায় যাচাইয়ের জন্য ইমেইল পাওয়া যায়নি (Email not found for re-authentication).")
                val credential = EmailAuthProvider.getCredential(userEmail, reauthPassword)
                user.reauthenticate(credential).await()
            }

            // Delete user's notifications subcollection documents first
            val notifCollection = firestore.collection("users").document(uid).collection("notifications")
            val notifSnapshot = notifCollection.get().await()

            val batch = firestore.batch()
            for (doc in notifSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(firestore.collection("public_donors").document(uid))
            batch.delete(firestore.collection("users").document(uid))
            batch.commit().await()

            user.delete().await()

            prefs?.edit()?.clear()?.apply()
            _currentUserProfile.value = null
            _authState.value = AuthState.Unauthenticated
            Resource.Success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Resource.Error(mapFirebaseException(e), e)
        }
    }

    fun signOut() {
        prefs?.edit()?.clear()?.apply()
        _currentUserProfile.value = null
        _authState.value = AuthState.Unauthenticated
        auth.signOut()
    }

    companion object {
        private const val AUTH_PREFS_NAME = "roktosetu_auth_session_prefs"

        fun validatePasswordRules(pass: String): String? {
            return when {
                pass.length < 8 ->
                    "Password must be at least 8 characters (পাসওয়ার্ড অন্তত ৮ অক্ষরের হতে হবে)"
                !pass.any { it.isUpperCase() } ->
                    "Password must contain at least one uppercase letter (অন্তত একটি বড় হাতের অক্ষর থাকতে হবে)"
                !pass.any { it.isLowerCase() } ->
                    "Password must contain at least one lowercase letter (অন্তত একটি ছোট হাতের অক্ষর থাকতে হবে)"
                !pass.any { it.isDigit() } ->
                    "Password must contain at least one number (অন্তত একটি সংখ্যা থাকতে হবে)"
                else -> null
            }
        }

        fun isRecentLoginRequired(e: Throwable?): Boolean {
            if (e == null) return false
            if (e is FirebaseAuthRecentLoginRequiredException) return true
            val msg = e.message ?: ""
            return msg.contains("CREDENTIAL_TOO_OLD_LOGIN_AGAIN", ignoreCase = true) ||
                msg.contains("requires recent authentication", ignoreCase = true)
        }

        fun mapFirebaseException(e: Throwable): String {
            return when (e) {
                is FirebaseAuthWeakPasswordException ->
                    "পাসওয়ার্ড দুর্বল। অন্তত ৮ অক্ষর, বড় হাতের, ছোট হাতের অক্ষর এবং সংখ্যা ব্যবহার করুন (Password must be at least 8 characters with uppercase, lowercase, and numbers)."
                is FirebaseAuthInvalidCredentialsException ->
                    "ইমেইল বা পাসওয়ার্ড সঠিক নয়। অনুগ্রহ করে যাচাই করে আবার চেষ্টা করুন (Invalid email or password. Please check your credentials and try again)."
                is FirebaseAuthInvalidUserException -> when (e.errorCode) {
                    "ERROR_USER_DISABLED" ->
                        "এই অ্যাকাউন্টটি অ্যাডমিন কর্তৃক নিষ্ক্রিয় করা হয়েছে (This account has been disabled by an administrator)."
                    "ERROR_USER_NOT_FOUND" ->
                        "এই ইমেইলে কোনো অ্যাকাউন্ট পাওয়া যায়নি। অনুগ্রহ করে রেজিস্টার করুন (No account found with this email address. Please register first)."
                    else ->
                        "আপনার সেশন মেয়াদোত্তীর্ণ হয়েছে। অনুগ্রহ করে আবার লগইন করুন (Account session is no longer valid. Please sign in again)."
                }
                is FirebaseAuthUserCollisionException ->
                    "এই ইমেইল দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট রয়েছে। অনুগ্রহ করে লগইন বা পাসওয়ার্ড রিসেট করুন (An account already exists with this email address. Please sign in or reset your password)."
                is FirebaseAuthRecentLoginRequiredException ->
                    "নিরাপত্তার স্বার্থে পুনরায় পাসওয়ার্ড দিয়ে যাচাই করুন অথবা পুনরায় লগইন করুন (For security reasons, please re-authenticate before performing this sensitive action)."
                is FirebaseNetworkException ->
                    "ইন্টারনেট সংযোগ ব্যর্থ হয়েছে। আপনার নেটওয়ার্ক চেক করে আবার চেষ্টা করুন (Network connection failed. Please check your internet connection and try again)."
                else -> {
                    val msg = e.localizedMessage ?: "একটি অপ্রত্যাশিত ত্রুটি ঘটেছে (An unexpected authentication error occurred)."
                    when {
                        msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true) ||
                            msg.contains("blocked all requests", ignoreCase = true) ->
                            "অতিরিক্ত প্রচেষ্টার কারণে সাময়িকভাবে ব্লক করা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন (Too many attempts detected. Please wait a few minutes before trying again)."
                        msg.contains("network", ignoreCase = true) ->
                            "নেটওয়ার্ক ত্রুটি ঘটেছে। আপনার ইন্টারনেট সংযোগ যাচাই করুন (Network error occurred. Please check your internet connection)."
                        else -> msg
                    }
                }
            }
        }
    }
}
