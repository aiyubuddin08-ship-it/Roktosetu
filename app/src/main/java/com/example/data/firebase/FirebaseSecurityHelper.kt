package com.example.data.firebase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Standardized result wrapper for operations throughout the application.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val code: String? = null
    ) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

/**
 * Security and privacy sanitization helper.
 * Masks internal errors into clean, natural Bengali user-facing messages
 * and initializes App Check (Play Integrity in release, Debug provider in debug) + notification channels.
 */
object FirebaseSecurityHelper {

    @Volatile
    private var appCheckInitialized = false

    private val BD_MOBILE_REGEX = Regex("^(?:\\+?880|0)1[3-9]\\d{8}$")

    fun isAppCheckInitialized(): Boolean = appCheckInitialized

    /**
     * Initializes Firebase App Check (Play Integrity in release builds, Debug provider in debug builds)
     * and creates the emergency notification channel.
     */
    fun initializeAppCheckAndChannels(context: Context) {
        val appContext = context.applicationContext
        RoktoSetuMessagingService.ensureNotificationChannel(appContext)
        if (appCheckInitialized) return

        synchronized(this) {
            if (appCheckInitialized) return
            try {
                FirebaseApp.initializeApp(appContext)
                val firebaseAppCheck = FirebaseAppCheck.getInstance()
                if (BuildConfig.DEBUG) {
                    val debugFactoryClass = Class.forName(
                        "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
                    )
                    val getInstanceMethod = debugFactoryClass.getMethod("getInstance")
                    val factory = getInstanceMethod.invoke(null) as? AppCheckProviderFactory
                    if (factory != null) {
                        firebaseAppCheck.installAppCheckProviderFactory(factory)
                    }
                } else {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }
                appCheckInitialized = true
            } catch (e: Exception) {
                Log.w("FirebaseSecurityHelper", "App Check initialization skipped: ${e.message}")
            }
        }
    }

    /**
     * Checks whether runtime notification permission is granted on Android 13+.
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun mapExceptionToMessage(e: Throwable?): String {
        if (e == null) return "অপ্রত্যাশিত ত্রুটি ঘটেছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
        val rawMessage = e.message ?: ""

        return when {
            rawMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
            rawMessage.contains("permission-denied", ignoreCase = true) ->
                "এই কাজের জন্য আপনার প্রয়োজনীয় অনুমতি নেই। অনুগ্রহ করে অনুমোদিত অ্যাকাউন্টে লগইন করুন।"

            rawMessage.contains("UNAUTHENTICATED", ignoreCase = true) ||
            rawMessage.contains("unauthenticated", ignoreCase = true) ->
                "আপনার সেশনের মেয়াদ শেষ হয়েছে। অনুগ্রহ করে পুনরায় লগইন করুন।"

            rawMessage.contains("NOT_FOUND", ignoreCase = true) ->
                "অনুরোধকৃত তথ্য খুঁজে পাওয়া যায়নি।"

            rawMessage.contains("ALREADY_EXISTS", ignoreCase = true) ||
            rawMessage.contains("email-already-in-use", ignoreCase = true) ->
                "এই ইমেইল দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট রয়েছে। অনুগ্রহ করে লগইন করুন।"

            rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                "সার্ভার সাময়িকভাবে ব্যস্ত আছে, কিছুক্ষণ পর আবার চেষ্টা করুন।"

            rawMessage.contains("network", ignoreCase = true) ||
            rawMessage.contains("timeout", ignoreCase = true) ||
            rawMessage.contains("unavailable", ignoreCase = true) ->
                "ইন্টারনেট সংযোগে সমস্যা হয়েছে। আপনার নেটওয়ার্ক যাচাই করে আবার চেষ্টা করুন।"

            rawMessage.contains("wrong-password", ignoreCase = true) ||
            rawMessage.contains("invalid-credential", ignoreCase = true) ->
                "ভুল ইমেইল বা পাসওয়ার্ড। অনুগ্রহ করে সঠিক তথ্য দিন।"

            rawMessage.contains("user-not-found", ignoreCase = true) ->
                "এই ইমেইলে কোনো অ্যাকাউন্ট পাওয়া যায়নি। অনুগ্রহ করে নিবন্ধন করুন।"

            rawMessage.contains("invalid-email", ignoreCase = true) ->
                "অনুগ্রহ করে সঠিক ইমেইল ঠিকানা প্রদান করুন।"

            rawMessage.contains("weak-password", ignoreCase = true) ->
                "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে (বড় হাতের, ছোট হাতের অক্ষর ও সংখ্যা সহ)।"

            else -> "কার্যক্রমটি সম্পন্ন করা যায়নি। অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
        }
    }

    /**
     * Sanitizes user input against malicious script injection or invalid lengths.
     */
    fun sanitizeInput(text: String, maxLength: Int = 500): String {
        return text.trim().take(maxLength)
    }

    /**
     * Normalizes a phone string by stripping spaces, hyphens, and parentheses.
     */
    fun normalizePhoneNumber(phone: String): String {
        return phone.trim().replace(Regex("[\\s\\-()]"), "")
    }

    /**
     * Validates Bangladeshi mobile phone number format: `^(?:\+?880|0)1[3-9]\d{8}$`
     * (e.g., 01712345678, +8801812345678, 8801912345678).
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val cleaned = normalizePhoneNumber(phone)
        return BD_MOBILE_REGEX.matches(cleaned)
    }

    /**
     * Validates phone or hotline number for institutional directories (blood banks, ambulances, camps),
     * accepting standard Bangladeshi mobile numbers as well as national hotlines (e.g. 999, 16263) and landlines.
     */
    fun isValidEmergencyOrContactPhone(phone: String): Boolean {
        val cleaned = normalizePhoneNumber(phone)
        if (BD_MOBILE_REGEX.matches(cleaned)) return true
        val digits = cleaned.filter { it.isDigit() || it == '+' }
        return digits.length in 3..16 && digits.any { it.isDigit() }
    }
}
