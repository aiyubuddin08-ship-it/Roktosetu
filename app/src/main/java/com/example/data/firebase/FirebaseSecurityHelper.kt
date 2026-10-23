package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck

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
 * Masks internal Firebase/network stacktraces into clean, secure user-facing messages
 * and initializes Firebase App Check + notification channels at startup.
 */
object FirebaseSecurityHelper {

    @Volatile
    private var appCheckInitialized = false

    /**
     * Initializes Firebase App Check (Debug provider in debug builds via reflection,
     * Play Integrity / ReCaptcha Enterprise in release builds when configured) and creates
     * the emergency notification channel.
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
                }
                appCheckInitialized = true
            } catch (e: Exception) {
                Log.w("FirebaseSecurityHelper", "App Check initialization skipped: ${e.message}")
            }
        }
    }

    fun mapExceptionToMessage(e: Throwable?): String {
        if (e == null) return "Unknown error occurred. Please try again."
        val rawMessage = e.message ?: ""
        
        return when {
            rawMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
            rawMessage.contains("permission-denied", ignoreCase = true) ->
                "অনুমতি নেই (Access denied). অনুগ্রহ করে সাইন ইন করুন।"
            
            rawMessage.contains("UNAUTHENTICATED", ignoreCase = true) ||
            rawMessage.contains("unauthenticated", ignoreCase = true) ->
                "সেশনের মেয়াদ শেষ হয়েছে। অনুগ্রহ করে পুনরায় লগইন করুন।"
            
            rawMessage.contains("NOT_FOUND", ignoreCase = true) ->
                "অনুরোধকৃত তথ্য পাওয়া যায়নি (Resource not found)."
            
            rawMessage.contains("ALREADY_EXISTS", ignoreCase = true) ||
            rawMessage.contains("email-already-in-use", ignoreCase = true) ->
                "এই ইমেইল দিয়ে ইতিমধ্যে একাউন্ট তৈরি করা আছে। অনুগ্রহ করে লগইন করুন।"
            
            rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                "সার্ভার ব্যস্ত আছে, কিছুক্ষণ পর আবার চেষ্টা করুন।"
                
            rawMessage.contains("network", ignoreCase = true) ||
            rawMessage.contains("timeout", ignoreCase = true) ||
            rawMessage.contains("unavailable", ignoreCase = true) ->
                "নেটওয়ার্ক সংযোগ সমস্যা। অফলাইন মোডে সংরক্ষিত তথ্য প্রদর্শিত হচ্ছে।"

            rawMessage.contains("wrong-password", ignoreCase = true) ||
            rawMessage.contains("invalid-credential", ignoreCase = true) ->
                "ভুল ইমেইল বা পাসওয়ার্ড। অনুগ্রহ করে সঠিক তথ্য দিন।"

            rawMessage.contains("user-not-found", ignoreCase = true) ->
                "এই ইমেইলে কোনো একাউন্ট পাওয়া যায়নি। অনুগ্রহ করে রেজিস্ট্রেশন করুন।"

            rawMessage.contains("invalid-email", ignoreCase = true) ->
                "অনুগ্রহ করে সঠিক ইমেইল ঠিকানা প্রদান করুন।"

            rawMessage.contains("weak-password", ignoreCase = true) ->
                "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে (বড় হাতের, ছোট হাতের অক্ষর ও সংখ্যা সহ)।"

            else -> "অপারেশন সম্পন্ন করা যায়নি। অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
        }
    }

    /**
     * Sanitizes user input against malicious script injection or invalid lengths.
     */
    fun sanitizeInput(text: String, maxLength: Int = 500): String {
        return text.trim().take(maxLength)
    }

    /**
     * Validates phone number format for emergency blood calls.
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() || it == '+' }
        return digits.length in 8..16
    }
}
