package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.model.AuthState
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.model.RegistrationValidationResult
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val resendCooldownSeconds: Int = 0
)

class AuthViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthState> = repository.authState
    val currentUserProfile: StateFlow<UserProfile?> = repository.currentUserProfile

    /**
     * Sign In with Email & Password.
     */
    fun signIn(email: String, pass: String, onSuccess: () -> Unit = {}) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter both email and password")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.signIn(trimmedEmail, pass)) {
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
     * Validates and registers a new user with strict password and form rules.
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
        city: String,
        area: String,
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
                ?: "Please review your input"
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
                city = city.trim(),
                area = area.trim(),
                dateOfBirth = dob.trim()
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Account created! Verification email sent."
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

    /**
     * Completes registration input validation.
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
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "Please provide a valid email address"
            isValid = false
        }

        val trimmedPhone = phone.trim()
        if (trimmedPhone.length < 8) {
            phoneError = "Please enter a valid phone number"
            isValid = false
        }

        if (pass.length < 8) {
            passError = "Password must be at least 8 characters"
            isValid = false
        } else if (!pass.any { it.isUpperCase() }) {
            passError = "Password must contain at least one uppercase letter"
            isValid = false
        } else if (!pass.any { it.isLowerCase() }) {
            passError = "Password must contain at least one lowercase letter"
            isValid = false
        } else if (!pass.any { it.isDigit() }) {
            passError = "Password must contain at least one number"
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
     * Signs in with Google Id Token.
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
     * Checks email verification status by reloading user from Firebase Auth.
     */
    fun checkEmailVerification(onVerified: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val isVerified = repository.checkEmailVerification()
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (isVerified) {
                _uiState.value = _uiState.value.copy(successMessage = "Email verified successfully!")
                onVerified()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Email not yet verified. Please click the link in your inbox.")
            }
        }
    }

    /**
     * Resends email verification with cooldown protection.
     */
    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.sendEmailVerification()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Verification email resent! Please check your inbox and spam folder."
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
     * Sends Password Reset Email.
     */
    fun sendPasswordReset(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid account email")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            when (val result = repository.sendPasswordReset(trimmed)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "If an account exists for $trimmed, password reset instructions have been sent."
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
     * Completes Profile for newly authenticated users (e.g. Google Sign-In).
     */
    fun completeProfile(
        name: String,
        phone: String,
        bloodGroup: BloodGroup,
        dob: String,
        isDonor: Boolean,
        donorAvailability: DonorAvailability,
        isEmergencyDonor: Boolean,
        city: String,
        area: String,
        photoUrl: String = "",
        onSuccess: () -> Unit = {}
    ) {
        if (name.trim().length < 2 || phone.trim().length < 8) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please provide your full name and valid contact phone")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.completeProfile(
                name = name,
                phone = phone,
                bloodGroup = bloodGroup,
                dateOfBirth = dob,
                isDonor = isDonor,
                donorAvailability = donorAvailability,
                isEmergencyDonor = isEmergencyDonor,
                city = city,
                area = area,
                photoUrl = photoUrl
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Profile setup complete! Welcome to RoktoSetu."
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
                        successMessage = "Profile updated successfully"
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

    fun signOut(onSignedOut: () -> Unit = {}) {
        repository.signOut()
        _uiState.value = AuthUiState()
        onSignedOut()
    }

    fun deleteAccount(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.deleteAccount()) {
                is Resource.Success -> {
                    _uiState.value = AuthUiState(successMessage = "Account deleted successfully.")
                    onDeleted()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
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
