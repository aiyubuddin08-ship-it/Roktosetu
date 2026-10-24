package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirebaseSecurityHelper
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.model.AuthState
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.model.RegistrationValidationResult
import com.example.model.UserProfile
import com.example.model.UserReport
import com.example.model.VerificationStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val resendCooldownSeconds: Int = 0,
    val requiresReauthForDelete: Boolean = false,
    val myReports: List<UserReport> = emptyList()
)

class AuthViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthState> = repository.authState
    val currentUserProfile: StateFlow<UserProfile?> = repository.currentUserProfile

    private var cooldownJob: Job? = null
    private var myReportsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.currentUserProfile.collect { profile ->
                myReportsJob?.cancel()
                if (profile != null) {
                    myReportsJob = viewModelScope.launch {
                        repository.getMyReports().collect { reports ->
                            _uiState.value = _uiState.value.copy(myReports = reports)
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(myReports = emptyList())
                }
            }
        }
    }

    private fun isValidEmailFormat(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        val pattern = android.util.Patterns.EMAIL_ADDRESS
        return pattern?.matcher(trimmed)?.matches()
            ?: Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(trimmed)
    }

    private fun startResendCooldown(seconds: Int = 60) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(resendCooldownSeconds = remaining)
                delay(1000L)
            }
            _uiState.value = _uiState.value.copy(resendCooldownSeconds = 0)
        }
    }

    /**
     * Sign In with Email & Password.
     */
    fun signIn(
        email: String,
        pass: String,
        onSuccess: () -> Unit = {},
        onAdminSuccess: (() -> Unit)? = null
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "অনুগ্রহ করে ইমেইল এবং পাসওয়ার্ড উভয়ই লিখুন (Please enter both email and password)"
            )
            return
        }
        if (!isValidEmailFormat(trimmedEmail)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "অনুগ্রহ করে একটি সঠিক ইমেইল ঠিকানা দিন (Please enter a valid email address)"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.signIn(trimmedEmail, pass)) {
                is Resource.Success -> {
                    val profile = result.data
                    val isAdminUser = profile.role == com.example.model.UserRole.SUPER_ADMIN ||
                        profile.role == com.example.model.UserRole.ORGANIZATION_ADMIN
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = if (isAdminUser) "অ্যাডমিন লগইন সফল হয়েছে! (Admin login successful)" else null
                    )
                    if (isAdminUser && onAdminSuccess != null) {
                        onAdminSuccess()
                    } else {
                        onSuccess()
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Validates and registers a new user with strict password and Bangladeshi phone rules.
     */
    fun register(
        name: String,
        email: String,
        pass: String,
        confirmPass: String,
        phone: String,
        bloodGroup: BloodGroup,
        dob: String,
        isDonor: Boolean,
        termsAccepted: Boolean,
        division: String = "Dhaka",
        district: String = "Dhaka",
        upazila: String = "",
        city: String = district,
        area: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val validation = validateRegistrationInput(name, email, pass, confirmPass, phone, termsAccepted)
        if (!validation.isValid) {
            val errorMsg = validation.nameError
                ?: validation.emailError
                ?: validation.phoneError
                ?: validation.passwordError
                ?: validation.confirmPasswordError
                ?: validation.termsError
                ?: "অনুগ্রহ করে আপনার তথ্যগুলো যাচাই করুন (Please review your input)"
            _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.register(
                email = email.trim(),
                pass = pass,
                name = name.trim(),
                bloodGroup = bloodGroup,
                isDonor = isDonor,
                phone = phone.trim(),
                division = division.trim().ifBlank { "Dhaka" },
                district = district.trim().ifBlank { city.trim().ifBlank { "Dhaka" } },
                upazila = upazila.trim(),
                city = city.trim().ifBlank { district.trim().ifBlank { "Dhaka" } },
                area = area.trim(),
                dateOfBirth = dob.trim()
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "অ্যাকাউন্ট তৈরি হয়েছে! ভেরিফিকেশন ইমেইল পাঠানো হয়েছে। (Account created! Verification email sent.)"
                    )
                    startResendCooldown(60)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Completes registration input validation using the shared password policy and BD phone regex.
     */
    fun validateRegistrationInput(
        name: String,
        email: String,
        pass: String,
        confirmPass: String,
        phone: String,
        termsAccepted: Boolean
    ): RegistrationValidationResult {
        var isValid = true
        var nameError: String? = null
        var emailError: String? = null
        var phoneError: String? = null
        var passError: String? = null
        var confirmPassError: String? = null
        var termsError: String? = null

        if (name.trim().length < 2) {
            nameError = "Full name must be at least 2 characters"
            isValid = false
        }

        val trimmedEmail = email.trim()
        if (!isValidEmailFormat(trimmedEmail)) {
            emailError = "Please provide a valid email address"
            isValid = false
        }

        val trimmedPhone = phone.trim()
        if (!FirebaseSecurityHelper.isValidPhoneNumber(trimmedPhone)) {
            phoneError = "অনুগ্রহ করে সঠিক বাংলাদেশি মোবাইল নম্বর দিন (যেমন 017XXXXXXXX বা +88017XXXXXXXX)"
            isValid = false
        }

        val passwordRuleError = FirebaseAuthService.validatePasswordRules(pass)
        if (passwordRuleError != null) {
            passError = passwordRuleError
            isValid = false
        }

        if (pass != confirmPass) {
            confirmPassError = "Passwords do not match"
            isValid = false
        }

        if (!termsAccepted) {
            termsError = "You must agree to the Terms of Service & Privacy Policy"
            isValid = false
        }

        return RegistrationValidationResult(
            isValid = isValid,
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            passwordError = passError,
            confirmPasswordError = confirmPassError,
            termsError = termsError
        )
    }

    /**
     * Signs in with Google Id Token via Jetpack CredentialManager.
     */
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Checks email verification status by reloading user.
     */
    fun checkEmailVerification(onVerified: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val isVerified = repository.checkEmailVerification()
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (isVerified) {
                cooldownJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    resendCooldownSeconds = 0,
                    successMessage = "ইমেইল সফলভাবে যাচাই হয়েছে! (Email verified successfully!)"
                )
                onVerified()
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "ইমেইল এখনো যাচাই হয়নি। আপনার ইনবক্সের লিংকে ক্লিক করে পুনরায় চেষ্টা করুন। (Email not yet verified. Please click the link in your inbox.)"
                )
            }
        }
    }

    /**
     * Resends email verification with 60-second cooldown protection.
     */
    fun resendVerificationEmail() {
        if (_uiState.value.resendCooldownSeconds > 0) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.sendEmailVerification()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "ভেরিফিকেশন ইমেইল পুনরায় পাঠানো হয়েছে! ইনবক্স ও স্প্যাম ফোল্ডার চেক করুন। (Verification email resent!)"
                    )
                    startResendCooldown(60)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Sends Password Reset Email with user-enumeration-safe neutral confirmation message.
     */
    fun sendPasswordReset(email: String) {
        val trimmed = email.trim()
        if (!isValidEmailFormat(trimmed)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "অনুগ্রহ করে একটি সঠিক ইমেইল ঠিকানা দিন (Please enter a valid account email)"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            when (val result = repository.sendPasswordReset(trimmed)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "ইমেইল নিবন্ধিত থাকলে লিংক পাঠানো হয়েছে (If this email is registered, a password reset link has been sent.)"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Completes Profile for newly authenticated users (e.g. Google Sign-In or profile recovery).
     */
    fun completeProfile(
        name: String,
        phone: String,
        bloodGroup: BloodGroup,
        dob: String,
        isDonor: Boolean,
        donorAvailability: DonorAvailability,
        isEmergencyDonor: Boolean = false,
        division: String = "Dhaka",
        district: String = "Dhaka",
        upazila: String = "",
        city: String = district,
        area: String = "",
        photoUrl: String = "",
        onSuccess: () -> Unit = {}
    ) {
        if (name.trim().length < 2 || !FirebaseSecurityHelper.isValidPhoneNumber(phone.trim())) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "অনুগ্রহ করে আপনার পূর্ণ নাম এবং সঠিক বাংলাদেশি মোবাইল নম্বর দিন (যেমন 017XXXXXXXX)"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.completeProfile(
                name = name.trim(),
                phone = phone.trim(),
                bloodGroup = bloodGroup,
                dateOfBirth = dob.trim(),
                isDonor = isDonor,
                donorAvailability = donorAvailability,
                isEmergencyDonor = isEmergencyDonor,
                division = division.trim().ifBlank { "Dhaka" },
                district = district.trim().ifBlank { city.trim().ifBlank { "Dhaka" } },
                upazila = upazila.trim(),
                city = city.trim().ifBlank { district.trim().ifBlank { "Dhaka" } },
                area = area.trim(),
                photoUrl = photoUrl
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "প্রোফাইল সম্পন্ন হয়েছে! রক্তসেতুতে স্বাগতম। (Profile setup complete! Welcome to RoktoSetu.)"
                    )
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.updateProfile(profile)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "প্রোফাইল সফলভাবে আপডেট হয়েছে (Profile updated successfully)"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun toggleDonorAvailability(isAvailable: Boolean) {
        val current = currentUserProfile.value ?: return
        val updated = current.copy(
            donorAvailability = if (isAvailable) DonorAvailability.AVAILABLE else DonorAvailability.UNAVAILABLE
        )
        updateProfile(updated)
    }

    fun setDonorAvailability(availability: DonorAvailability) {
        val current = currentUserProfile.value ?: return
        updateProfile(current.copy(donorAvailability = availability))
    }

    fun submitVerificationRequest(documents: List<String>, type: String = "DONOR_CARD") {
        val current = currentUserProfile.value ?: return
        if (current.verificationStatus == VerificationStatus.VERIFIED) {
            _uiState.value = _uiState.value.copy(errorMessage = "আপনার প্রোফাইল ইতিমধ্যে যাচাইকৃত (Your donor profile is already verified).")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.submitVerificationTask(
                targetId = current.uid,
                type = type,
                name = current.name,
                bloodGroup = current.bloodGroup,
                documents = documents
            )) {
                is Resource.Success -> {
                    if (current.verificationStatus != VerificationStatus.PENDING) {
                        repository.updateProfile(current.copy(verificationStatus = VerificationStatus.PENDING))
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "ভেরিফিকেশন আবেদন অ্যাডমিন রিভিউয়ের জন্য জমা হয়েছে। (Verification request submitted for administrator review.)"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun uploadAndSubmitVerificationRequest(
        context: Context,
        fileUri: Uri?,
        fallbackDocumentRef: String,
        type: String = "DONOR_CARD"
    ) {
        val current = currentUserProfile.value ?: return
        if (current.verificationStatus == VerificationStatus.VERIFIED) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "আপনার প্রোফাইল ইতিমধ্যে যাচাইকৃত (Your donor profile is already verified)."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val resolvedDocPath = if (fileUri != null) {
                when (val uploadRes = repository.uploadVerificationDocument(context, current.uid, fileUri)) {
                    is Resource.Success -> uploadRes.data
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = uploadRes.message)
                        return@launch
                    }
                    is Resource.Loading -> return@launch
                }
            } else {
                fallbackDocumentRef.trim()
            }

            if (resolvedDocPath.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "অনুগ্রহ করে ল্যাব রিপোর্ট ফাইল নির্বাচন করুন বা রেফারেন্স দিন। (Please select a lab report file or provide a reference.)"
                )
                return@launch
            }

            when (val res = repository.submitVerificationTask(
                targetId = current.uid,
                type = type,
                name = current.name,
                bloodGroup = current.bloodGroup,
                documents = listOf(resolvedDocPath)
            )) {
                is Resource.Success -> {
                    if (current.verificationStatus != VerificationStatus.PENDING) {
                        repository.updateProfile(current.copy(verificationStatus = VerificationStatus.PENDING))
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "ভেরিফিকেশন ডকুমেন্ট ও আবেদন অ্যাডমিন রিভিউয়ের জন্য জমা হয়েছে। (Verification document & request submitted for review.)"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Phase 3: Submit a user report against a fake request, donor/user misconduct, or chat issue.
     */
    fun submitUserReport(
        targetUid: String,
        targetName: String,
        targetType: String,
        category: String,
        reasonBn: String,
        details: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            when (
                val res = repository.submitUserReport(
                    targetUid = targetUid,
                    targetName = targetName,
                    targetType = targetType,
                    category = category,
                    reasonBn = reasonBn,
                    details = details
                )
            ) {
                is Resource.Success -> {
                    val thankYou = "ধন্যবাদ! আপনার অভিযোগটি সফলভাবে গ্রহণ করা হয়েছে। প্রশাসক দল শীঘ্রই এটি পর্যালোচনা করবে।"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = thankYou
                    )
                    onResult(true, thankYou)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.message
                    )
                    onResult(false, res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun reportError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        cooldownJob?.cancel()
        viewModelScope.launch {
            repository.clearLocalUserCache()
        }
        repository.signOut()
        _uiState.value = AuthUiState()
        onSignedOut()
    }

    fun deleteAccount(reauthPassword: String? = null, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.deleteAccount(reauthPassword)) {
                is Resource.Success -> {
                    cooldownJob?.cancel()
                    _uiState.value = AuthUiState(
                        successMessage = "অ্যাকাউন্ট সফলভাবে মুছে ফেলা হয়েছে। (Account deleted successfully.)"
                    )
                    onDeleted()
                }
                is Resource.Error -> {
                    val needsReauth = FirebaseAuthService.isRecentLoginRequired(res.throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        requiresReauthForDelete = needsReauth,
                        errorMessage = res.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    val isEmailVerified: Boolean
        get() = repository.isEmailVerified

    fun changePassword(
        currentPass: String,
        newPass: String,
        confirmPass: String,
        onSuccess: () -> Unit = {}
    ) {
        if (currentPass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "অনুগ্রহ করে আপনার বর্তমান পাসওয়ার্ড দিন।")
            return
        }
        val ruleError = FirebaseAuthService.validatePasswordRules(newPass)
        if (ruleError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = ruleError)
            return
        }
        if (newPass != confirmPass) {
            _uiState.value = _uiState.value.copy(errorMessage = "নতুন পাসওয়ার্ড দুটি মিলছে না।")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            when (val res = repository.changePassword(currentPass, newPass)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "আপনার পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে।"
                    )
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissReauthDialog() {
        _uiState.value = _uiState.value.copy(requiresReauthForDelete = false)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

class AuthViewModelFactory(private val repository: RoktoSetuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
