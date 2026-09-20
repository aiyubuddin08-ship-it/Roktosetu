package com.example.model

/**
 * Single source of truth for the Application's Authentication and Session State.
 */
sealed interface AuthState {
    /** Checking initial authentication state or performing background reload */
    data object Loading : AuthState

    /** No active user session */
    data object Unauthenticated : AuthState

    /** Authenticated user with verified email and completed active profile */
    data object Authenticated : AuthState

    /** User registered but has not yet verified their email address */
    data object EmailVerificationRequired : AuthState

    /** User authenticated (e.g. via Google) but lacks a completed Firestore profile */
    data object ProfileCompletionRequired : AuthState

    /** User account has been deactivated or disabled by an administrator */
    data object AccountDisabled : AuthState

    /** Authentication or session verification encountered a fatal error */
    data class Error(val message: String) : AuthState
}

/**
 * Registration form input validation result.
 */
data class RegistrationValidationResult(
    val isValid: Boolean,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val phoneError: String? = null,
    val dobError: String? = null,
    val termsError: String? = null
)
