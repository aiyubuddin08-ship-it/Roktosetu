package com.example.base

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Before
import org.robolectric.Shadows.shadowOf

abstract class FirestoreEmulatorTestBase {

    protected lateinit var auth: FirebaseAuth
    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var projectId: String
    protected lateinit var databaseId: String

    @Before
    open fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        projectId = context.getString(R.string.project_id)
        // Cloud Firestore Emulator enforces rules on the (default) database instance
        databaseId = "(default)"

        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(context.getString(R.string.google_app_id))
                .setApiKey(context.getString(R.string.google_api_key))
                .build()
            FirebaseApp.initializeApp(context, options)
        }

        auth = FirebaseAuth.getInstance()
        try {
            auth.useEmulator("127.0.0.1", 9099)
        } catch (_: IllegalStateException) {
            // Emulator already configured on singleton
        }

        val app = FirebaseApp.getInstance()
        firestore = FirebaseFirestore.getInstance(app)

        try {
            firestore.useEmulator("127.0.0.1", 8085)
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
        } catch (_: IllegalStateException) {
            // Settings already applied
        }

        clearEmulatorData()
    }

    @After
    open fun tearDown() {
        try {
            awaitTask(firestore.terminate())
            awaitTask(firestore.clearPersistence())
        } catch (_: Exception) {
        }
        FirebaseApp.clearInstancesForTest()
    }

    protected fun signInTestUser(email: String = "testuser@roktosetu.org", password: String = "Pass1234!"): String {
        val signUpJson = """{"email":"$email","password":"$password","returnSecureToken":true}"""
        val conn = (URL("http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 5000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        conn.outputStream.use { it.write(signUpJson.toByteArray(Charsets.UTF_8)) }
        conn.responseCode
        conn.disconnect()

        val credential = EmailAuthProvider.getCredential(email, password)
        val result: AuthResult = awaitTask(auth.signInWithCredential(credential))
        return result.user!!.uid
    }

    protected fun clearEmulatorData() {
        val dbUrl = "http://127.0.0.1:8085/emulator/v1/projects/$projectId/databases/$databaseId/documents"
        try {
            val dbConn = (URL(dbUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5000
                readTimeout = 5000
            }
            dbConn.responseCode
            dbConn.disconnect()
        } catch (_: Exception) {
        }

        val authUrl = "http://127.0.0.1:9099/emulator/v1/projects/$projectId/accounts"
        try {
            val authConn = (URL(authUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5000
                readTimeout = 5000
            }
            authConn.responseCode
            authConn.disconnect()
        } catch (_: Exception) {
        }
    }

    protected fun <T> awaitTask(task: Task<T>, timeoutSeconds: Long = 10): T {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds)
        while (!task.isComplete) {
            if (System.currentTimeMillis() > deadline) {
                throw TimeoutException("Task timed out after $timeoutSeconds seconds")
            }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        if (task.isSuccessful) {
            return task.result
        } else {
            throw task.exception ?: RuntimeException("Task failed with unknown exception")
        }
    }
}
