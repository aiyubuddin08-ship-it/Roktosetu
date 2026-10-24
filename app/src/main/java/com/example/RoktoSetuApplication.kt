package com.example

import android.app.Application
import com.example.data.firebase.FirebaseSecurityHelper

/**
 * Application entry point for RoktoSetu.
 * Initializes Firebase App Check (Play Integrity in release, Debug provider in debug builds)
 * and sets up high-priority emergency notification channels at process startup.
 */
class RoktoSetuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseSecurityHelper.initializeAppCheckAndChannels(this)
    }
}
