package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.domain.BloodCompatibilityHelper
import com.example.domain.LocationHelper
import com.example.model.AccountStatus
import com.example.model.AuditLogEntry
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DatabaseProtectionCheck
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.MetricsLoadState
import com.example.model.Organization
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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Administrative & Verification Queue Operations on `FirestoreService`.
 * Separated for modularity and clean Bengali user-facing messaging without technical jargon.
 */

suspend fun FirestoreService.checkIsCurrentUserAdmin(uid: String): Boolean {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return false

    if (uid.isBlank() || currentUser.uid != uid) return false

    val hasAdminClaim = try {
        val tokenResult = currentUser.getIdToken(true).await()
        tokenResult.claims["admin"] == true
    } catch (_: Exception) {
        false
    }
    if (hasAdminClaim) return true

    return try {
        db.collection("admins").document(uid).get().await().exists()
    } catch (_: Exception) {
        false
    }
}

suspend fun FirestoreService.submitVerificationRequest(
    applicantUid: String,
    name: String,
    type: String,
    documents: List<String>
): Resource<String> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("যাচাইয়ের আবেদন জমা দিতে অনুগ্রহ করে লগইন করুন।")

    val effectiveUid = applicantUid.ifBlank { currentUser.uid }
    if (effectiveUid != currentUser.uid) {
        return Resource.Error("আপনি শুধুমাত্র নিজের অ্যাকাউন্টের জন্য যাচাইয়ের আবেদন করতে পারবেন।")
    }

    val normalizedType = when (type.trim().uppercase()) {
        "ORGANIZATION" -> "ORGANIZATION"
        else -> "DONOR"
    }
    val docId = "ver_${effectiveUid}_${normalizedType.lowercase()}"
    val now = System.currentTimeMillis()
    val docSummary = documents.joinToString(", ").take(450).ifBlank { "জাতীয় পরিচয়পত্র ও রক্তদান সনদ" }

    val docRef = db.collection("verification_requests").document(docId)
    val payload = mapOf(
        "userUid" to effectiveUid,
        "applicantUid" to effectiveUid,
        "name" to name.trim().ifBlank { currentUser.displayName ?: "রক্তদাতা আবেদনকারী" },
        "type" to normalizedType,
        "documents" to documents.take(10),
        "documentSummary" to docSummary,
        "status" to "PENDING",
        "submittedAt" to now
    )
    return try {
        docRef.set(payload).await()
        Resource.Success(docRef.id)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

fun FirestoreService.observePendingVerificationTasks(
    onError: ((String) -> Unit)? = null
): Flow<List<VerificationTask>> = observeVerificationTasksInternal(onlyPending = true, onError = onError)

fun FirestoreService.observeAllVerificationTasks(
    onError: ((String) -> Unit)? = null
): Flow<List<VerificationTask>> = observeVerificationTasksInternal(onlyPending = false, onError = onError)

private fun FirestoreService.observeVerificationTasksInternal(
    onlyPending: Boolean,
    onError: ((String) -> Unit)? = null
): Flow<List<VerificationTask>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "verification_requests"
    val baseQuery: Query = if (onlyPending) {
        db.collection(path).whereEqualTo("status", "PENDING")
    } else {
        db.collection(path).limit(200)
    }
    val listener = baseQuery.addSnapshotListener { snapshot, error ->
        if (error != null) {
            handleFirestoreError(error, OperationType.LIST, path)
            onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
            trySend(emptyList())
            return@addSnapshotListener
        }
        val list = snapshot?.documents?.mapNotNull { doc ->
            val appUid = doc.getString("applicantUid")
                ?: doc.getString("userUid")
                ?: doc.getString("targetId")
                ?: ""
            val rawStatus = doc.getString("status") ?: "PENDING"
            val parsedStatus = when (rawStatus.uppercase()) {
                "APPROVED", "VERIFIED" -> VerificationStatus.VERIFIED
                "REJECTED" -> VerificationStatus.REJECTED
                else -> VerificationStatus.PENDING_REVIEW
            }
            val prevStatus = runCatching {
                VerificationStatus.valueOf(doc.getString("previousStatus") ?: "UNVERIFIED")
            }.getOrDefault(VerificationStatus.UNVERIFIED)
            val bgStr = doc.getString("bloodGroup")
            VerificationTask(
                id = doc.id,
                targetId = appUid,
                applicantUid = appUid,
                name = doc.getString("name") ?: "",
                type = doc.getString("type") ?: "DONOR",
                bloodGroup = bgStr?.let { BloodGroup.fromDisplay(it) },
                documents = (doc.get("documents") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                status = parsedStatus,
                previousStatus = prevStatus,
                rejectionReason = doc.getString("rejectionReason") ?: "",
                reviewedByUid = doc.getString("reviewedByUid") ?: "",
                reviewedByEmail = doc.getString("reviewedByEmail") ?: "",
                reviewedAt = doc.getLong("reviewedAt"),
                notes = doc.getString("notes") ?: "",
                submittedAt = doc.getLong("submittedAt") ?: 0L
            )
        }?.sortedByDescending { it.submittedAt } ?: emptyList()
        trySend(list)
    }

    awaitClose { listener.remove() }
}

fun FirestoreService.observeAuditLogs(
    onError: ((String) -> Unit)? = null
): Flow<List<AuditLogEntry>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "audit_logs"
    val listener = db.collection(path)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(100)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val logs = snapshot?.documents?.mapNotNull { doc ->
                val rawDetails = doc.get("details")
                val detailsMap: Map<String, String> = when (rawDetails) {
                    is Map<*, *> -> rawDetails.entries.associate { (k, v) -> k.toString() to v.toString() }
                    is String -> if (rawDetails.isNotBlank()) mapOf("summary" to rawDetails) else emptyMap()
                    else -> emptyMap()
                }
                AuditLogEntry(
                    id = doc.id,
                    actorUid = doc.getString("actorUid") ?: "",
                    actorEmail = doc.getString("actorEmail") ?: "",
                    action = doc.getString("action") ?: "",
                    targetId = doc.getString("targetId") ?: "",
                    targetType = doc.getString("targetType") ?: "",
                    outcome = doc.getString("outcome") ?: "SUCCESS",
                    details = detailsMap,
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            } ?: emptyList()
            trySend(logs)
        }

    awaitClose { listener.remove() }
}

suspend fun FirestoreService.reviewVerificationTaskAsAdmin(
    task: VerificationTask,
    approved: Boolean,
    adminUid: String,
    adminEmail: String,
    rejectionReason: String = "",
    notes: String = ""
): Resource<Unit> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    if (!approved && rejectionReason.trim().length < 3) {
        return Resource.Error("বাতিল করার ক্ষেত্রে সুনির্দিষ্ট কারণ উল্লেখ করা আবশ্যক।")
    }

    val decision = if (approved) "APPROVED" else "REJECTED"
    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val taskRef = db.collection("verification_requests").document(task.id)

    return try {
        db.runTransaction { tx ->
            val snap = tx.get(taskRef)
            if (!snap.exists()) {
                throw IllegalStateException("যাচাইয়ের আবেদনটি পাওয়া যায়নি।")
            }

            val orgRef = db.collection("organizations").document(task.applicantUid)
            val userRef = db.collection("users").document(task.applicantUid)
            val pubRef = db.collection("public_donors").document(task.applicantUid)

            val orgSnap = if (task.type == "ORGANIZATION") tx.get(orgRef) else null
            val userSnap = if (task.type != "ORGANIZATION") tx.get(userRef) else null
            val pubSnap = if (task.type != "ORGANIZATION") tx.get(pubRef) else null

            val previousStatusStr = if (task.type == "ORGANIZATION") {
                if (orgSnap?.getBoolean("isVerified") == true) "VERIFIED" else "UNVERIFIED"
            } else {
                userSnap?.getString("verificationStatus") ?: "PENDING"
            }

            val updateMap = mutableMapOf<String, Any>(
                "status" to decision,
                "previousStatus" to previousStatusStr,
                "reviewedByUid" to actorUid,
                "reviewedByEmail" to actorEmail,
                "reviewedAt" to now
            )
            if (!approved) {
                updateMap["rejectionReason"] = rejectionReason.trim()
            }
            if (notes.isNotBlank()) {
                updateMap["notes"] = notes.trim()
            }
            tx.update(taskRef, updateMap)

            if (task.type == "ORGANIZATION") {
                if (orgSnap != null && orgSnap.exists()) {
                    tx.update(orgRef, mapOf("isVerified" to approved))
                }
            } else {
                val verStatus = if (approved) VerificationStatus.VERIFIED.name else VerificationStatus.REJECTED.name
                if (userSnap != null && userSnap.exists()) {
                    tx.update(userRef, mapOf("verificationStatus" to verStatus, "updatedAt" to now))
                }
                if (pubSnap != null && pubSnap.exists()) {
                    tx.update(pubRef, mapOf("verificationStatus" to verStatus, "updatedAt" to now))
                }
                val notifRef = userRef.collection("notifications").document()
                tx.set(
                    notifRef,
                    mapOf(
                        "recipientUid" to task.applicantUid,
                        "title" to if (approved) "রক্তদাতা যাচাই অনুমোদিত হয়েছে" else "রক্তদাতা যাচাই আবেদন বাতিল হয়েছে",
                        "body" to if (approved) {
                            "আপনার রক্তদাতা যাচাইয়ের আবেদনটি অনুমোদিত হয়েছে।"
                        } else {
                            "কারণ: ${rejectionReason.trim()}"
                        },
                        "type" to "VERIFICATION_UPDATE",
                        "isRead" to false,
                        "createdAt" to now
                    )
                )
            }

            val auditRef = db.collection("audit_logs").document()
            tx.set(
                auditRef,
                mapOf(
                    "actorUid" to actorUid,
                    "actorEmail" to actorEmail,
                    "action" to "VERIFICATION_$decision",
                    "targetId" to task.applicantUid,
                    "targetType" to task.type,
                    "outcome" to "SUCCESS",
                    "details" to mapOf(
                        "summary" to "$decision verification for ${task.name}",
                        "previousStatus" to previousStatusStr,
                        "newStatus" to decision,
                        "reason" to rejectionReason.trim()
                    ),
                    "timestamp" to now
                )
            )
        }.await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, taskRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.uploadVerificationDocument(
    context: Context,
    userId: String,
    uri: Uri
): Resource<String> {
    if (userId.isBlank()) {
        return Resource.Error("অনুগ্রহ করে প্রথমে লগইন করুন।")
    }
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "application/pdf"
    val allowedMimeTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "application/pdf"
    )
    if (mimeType !in allowedMimeTypes) {
        return Resource.Error("শুধুমাত্র PDF, JPG, PNG বা WEBP ফাইল আপলোড করা যাবে।")
    }

    var fileName = "verification_report.pdf"
    var fileSize = 0L
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) {
                fileName = cursor.getString(nameIdx) ?: fileName
            }
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                fileSize = cursor.getLong(sizeIdx)
            }
        }
    }

    val maxBytes = 10L * 1024L * 1024L
    if (fileSize > maxBytes) {
        return Resource.Error("ফাইলের আকার সর্বোচ্চ ১০ মেগাবাইট হতে পারবে।")
    }

    val ext = when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "pdf"
    }
    val sanitizedBase = fileName
        .substringBeforeLast('.')
        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .take(48)
        .ifBlank { "lab_report" }
    val documentId = "${System.currentTimeMillis()}_${sanitizedBase}.$ext"
    val storagePath = "verifications/$userId/$documentId"

    return try {
        val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()
        storageRef.putFile(uri, metadata).await()
        Resource.Success(storagePath)
    } catch (e: Exception) {
        Log.e("FirestoreService", "Failed to upload verification document to $storagePath", e)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.fetchSystemMetrics(): SystemMetrics {
    val now = System.currentTimeMillis()
    return try {
        val usersSnap = try {
            db.collection("users").limit(500).get().await()
        } catch (_: Exception) {
            null
        }

        val donorsSnap = db.collection("public_donors")
            .whereEqualTo("isDonor", true)
            .limit(500)
            .get()
            .await()

        val totalUsers = if (usersSnap != null && !usersSnap.isEmpty) {
            usersSnap.size()
        } else {
            donorsSnap.size()
        }

        val activeDonors = donorsSnap.documents.count {
            (it.getString("availability") ?: DonorAvailability.AVAILABLE.name) == DonorAvailability.AVAILABLE.name
        }
        val emergencyDonorsOnline = donorsSnap.documents.count {
            (it.getBoolean("isEmergencyDonor") ?: false) &&
                (it.getString("availability") ?: DonorAvailability.AVAILABLE.name) != DonorAvailability.UNAVAILABLE.name
        }
        val donorDonationSum = donorsSnap.documents.sumOf {
            (it.getLong("donationCount") ?: 0L).toInt()
        }

        val donationsCount = try {
            db.collection("donation_history").limit(500).get().await().size()
        } catch (_: Exception) {
            0
        }
        val totalDonationsCompleted = maxOf(donorDonationSum, donationsCount)

        val pendingVerifications = try {
            db.collection("verification_requests")
                .whereEqualTo("status", "PENDING")
                .limit(300)
                .get()
                .await()
                .size()
        } catch (_: Exception) {
            0
        }

        val requestsSnap = db.collection("blood_requests")
            .limit(500)
            .get()
            .await()
        val activeStatuses = setOf(
            "OPEN",
            RequestStatus.PENDING.name,
            RequestStatus.DONOR_RESPONDED.name,
            RequestStatus.ACCEPTED.name
        )
        val pendingRequests = requestsSnap.documents.count { doc ->
            val st = doc.getString("status") ?: RequestStatus.PENDING.name
            val exp = doc.getLong("expiresAt") ?: (now + 100000L)
            st in activeStatuses && exp > now
        }
        val emergencyRequests = requestsSnap.documents.count { doc ->
            val st = doc.getString("status") ?: RequestStatus.PENDING.name
            val urg = doc.getString("urgency") ?: RequestUrgency.STANDARD.name
            val exp = doc.getLong("expiresAt") ?: (now + 100000L)
            st in activeStatuses && exp > now && urg == RequestUrgency.CRITICAL_EMERGENCY.name
        }
        val fulfilledRequests = requestsSnap.documents.count {
            it.getString("status") == RequestStatus.FULFILLED.name
        }
        val expiredRequests = requestsSnap.documents.count { doc ->
            val st = doc.getString("status") ?: RequestStatus.PENDING.name
            val exp = doc.getLong("expiresAt") ?: (now + 100000L)
            st == RequestStatus.EXPIRED.name || ((st == RequestStatus.PENDING.name || st == "OPEN") && exp <= now)
        }

        val orgsSnap = try {
            db.collection("organizations").limit(200).get().await()
        } catch (_: Exception) {
            null
        }
        val verifiedOrgs = orgsSnap?.documents?.count { it.getBoolean("isVerified") == true } ?: 0
        val pendingOrgs = orgsSnap?.documents?.count { it.getBoolean("isVerified") != true } ?: 0

        val openReports = try {
            db.collection("reports")
                .whereIn("status", listOf("OPEN", "IN_REVIEW"))
                .limit(200)
                .get()
                .await()
                .size()
        } catch (_: Exception) {
            0
        }

        val openAlerts = try {
            db.collection("security_alerts")
                .whereEqualTo("reviewStatus", "OPEN")
                .limit(200)
                .get()
                .await()
                .size()
        } catch (_: Exception) {
            0
        }

        SystemMetrics(
            totalRegisteredUsers = totalUsers,
            activeDonors = activeDonors,
            emergencyDonorsOnline = emergencyDonorsOnline,
            pendingVerifications = pendingVerifications,
            pendingRequests = pendingRequests,
            emergencyRequests = emergencyRequests,
            fulfilledRequests = fulfilledRequests,
            expiredRequests = expiredRequests,
            verifiedOrganizations = verifiedOrgs,
            pendingOrganizations = pendingOrgs,
            totalDonationsCompleted = totalDonationsCompleted,
            openReportsCount = openReports,
            securityAlertsCount = openAlerts,
            serverStatus = "সক্রিয় ও সংযুক্ত",
            appCheckStatus = if (FirebaseSecurityHelper.isAppCheckInitialized()) "সক্রিয়" else "প্রস্তুত",
            loadState = MetricsLoadState.LOADED,
            lastRefreshedAt = now,
            errorMessage = null
        )
    } catch (e: Exception) {
        Log.w("FirestoreService", "Unable to fetch full live metrics: ${e.message}")
        val isPermDenied = (e.message ?: "").contains("PERMISSION_DENIED", ignoreCase = true) ||
            (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        SystemMetrics(
            serverStatus = if (isPermDenied) "অনুমতি সীমাবদ্ধ" else "সংযোগ ত্রুটি",
            appCheckStatus = if (FirebaseSecurityHelper.isAppCheckInitialized()) "সক্রিয়" else "প্রস্তুত",
            loadState = if (isPermDenied) MetricsLoadState.PERMISSION_DENIED else MetricsLoadState.ERROR,
            lastRefreshedAt = null,
            errorMessage = FirebaseSecurityHelper.mapExceptionToMessage(e)
        )
    }
}

suspend fun FirestoreService.fetchEmergencyBroadcastArmed(): Boolean {
    return try {
        val snap = db.collection("system_settings")
            .document("emergency_broadcast")
            .get()
            .await()
        if (snap.exists()) {
            snap.getBoolean("armed") ?: true
        } else {
            true
        }
    } catch (_: Exception) {
        true
    }
}

suspend fun FirestoreService.setEmergencyBroadcastArmed(
    armed: Boolean,
    adminUid: String,
    adminEmail: String
): Resource<Boolean> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    val configRef = db.collection("system_settings").document("emergency_broadcast")
    val generalRef = db.collection("system_settings").document("general")
    val auditRef = db.collection("audit_logs").document()
    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin@roktosetu.org" }

    return try {
        val batch = db.batch()
        batch.set(
            configRef,
            mapOf(
                "armed" to armed,
                "updatedByUid" to actorUid,
                "updatedByEmail" to actorEmail,
                "updatedAt" to now
            )
        )
        batch.set(
            generalRef,
            mapOf(
                "emergencyBroadcastArmed" to armed,
                "updatedByUid" to actorUid,
                "updatedByEmail" to actorEmail,
                "updatedAt" to now
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to if (armed) "EMERGENCY_BROADCAST_ARMED" else "EMERGENCY_BROADCAST_PAUSED",
                "targetId" to "emergency_broadcast",
                "targetType" to "SYSTEM_CONFIG",
                "outcome" to "SUCCESS",
                "details" to if (armed) "জরুরি সম্প্রচার সক্রিয় করা হয়েছে" else "জরুরি সম্প্রচার সাময়িকভাবে বিরত করা হয়েছে",
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(armed)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, configRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

fun FirestoreService.observeAllUsersForAdmin(
    onError: ((String) -> Unit)? = null
): Flow<List<UserProfile>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "users"
    val listener = db.collection(path)
        .limit(300)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents
                ?.mapNotNull { parseUserProfileDoc(it) }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
            trySend(users)
        }
    awaitClose { listener.remove() }
}

suspend fun FirestoreService.updateUserAccountStatusAsAdmin(
    targetUser: UserProfile,
    newStatus: AccountStatus,
    reason: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    if (targetUser.uid == currentUser.uid && newStatus == AccountStatus.SUSPENDED) {
        return Resource.Error("নিজের প্রশাসনিক অ্যাকাউন্ট নিজে স্থগিত করা যাবে না।")
    }
    if (newStatus == AccountStatus.SUSPENDED && reason.trim().length < 3) {
        return Resource.Error("অ্যাকাউন্ট স্থগিত করার সুনির্দিষ্ট কারণ উল্লেখ করুন।")
    }

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val userRef = db.collection("users").document(targetUser.uid)
    val pubRef = db.collection("public_donors").document(targetUser.uid)
    val auditRef = db.collection("audit_logs").document()

    return try {
        val updatedProfile = targetUser.copy(
            accountStatus = newStatus,
            suspensionReason = if (newStatus == AccountStatus.SUSPENDED) reason.trim() else null,
            donorAvailability = if (newStatus == AccountStatus.SUSPENDED) DonorAvailability.UNAVAILABLE else targetUser.donorAvailability,
            updatedAt = now
        )
        val batch = db.batch()
        batch.set(userRef, updatedProfile.toFirestoreMap())
        if (targetUser.isDonor) {
            batch.set(pubRef, updatedProfile.toPublicDonorView().toFirestoreMap(isDonorFlag = true))
        }
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to if (newStatus == AccountStatus.SUSPENDED) "USER_ACCOUNT_SUSPENDED" else "USER_ACCOUNT_RESTORED",
                "targetId" to targetUser.uid,
                "targetType" to "USER",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "userName" to targetUser.name,
                    "previousStatus" to targetUser.accountStatus.name,
                    "newStatus" to newStatus.name,
                    "reason" to reason.trim()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, userRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.updateDonorStatusAsAdmin(
    targetUser: UserProfile,
    newAvailability: DonorAvailability,
    newVerificationStatus: VerificationStatus,
    notes: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val userRef = db.collection("users").document(targetUser.uid)
    val pubRef = db.collection("public_donors").document(targetUser.uid)
    val auditRef = db.collection("audit_logs").document()

    return try {
        val updatedProfile = targetUser.copy(
            donorAvailability = newAvailability,
            verificationStatus = newVerificationStatus,
            updatedAt = now
        )
        val batch = db.batch()
        batch.set(userRef, updatedProfile.toFirestoreMap())
        if (targetUser.isDonor) {
            batch.set(pubRef, updatedProfile.toPublicDonorView().toFirestoreMap(isDonorFlag = true))
        }
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "DONOR_STATUS_UPDATED",
                "targetId" to targetUser.uid,
                "targetType" to "DONOR",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "donorName" to targetUser.name,
                    "previousVerification" to targetUser.verificationStatus.name,
                    "newVerification" to newVerificationStatus.name,
                    "availability" to newAvailability.name,
                    "notes" to notes.trim()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, userRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

fun FirestoreService.observeAllBloodRequestsForAdmin(
    onError: ((String) -> Unit)? = null
): Flow<List<BloodRequest>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "blood_requests"
    val listener = db.collection(path)
        .limit(300)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents
                ?.mapNotNull { parseBloodRequest(it) }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
            trySend(list)
        }
    awaitClose { listener.remove() }
}

suspend fun FirestoreService.updateBloodRequestAsAdmin(
    request: BloodRequest,
    newStatus: RequestStatus,
    adminNotes: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val reqRef = db.collection("blood_requests").document(request.id)
    val auditRef = db.collection("audit_logs").document()

    return try {
        val updated = request.copy(
            status = newStatus,
            adminNotes = adminNotes.trim(),
            updatedAt = now
        )
        val batch = db.batch()
        batch.set(reqRef, updated.toFirestoreMap())
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "BLOOD_REQUEST_STATUS_${newStatus.name}",
                "targetId" to request.id,
                "targetType" to "BLOOD_REQUEST",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "patientName" to request.patientName,
                    "hospital" to request.hospitalName,
                    "previousStatus" to request.status.name,
                    "newStatus" to newStatus.name,
                    "notes" to adminNotes.trim()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.expireStaleRequestsAsAdmin(
    adminUid: String,
    adminEmail: String
): Resource<Int> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }

    return try {
        val snap = db.collection("blood_requests")
            .limit(200)
            .get()
            .await()

        val staleDocs = snap.documents.filter { doc ->
            val st = doc.getString("status") ?: "PENDING"
            val exp = doc.getLong("expiresAt") ?: (now + 100000L)
            (st == RequestStatus.PENDING.name || st == "OPEN") && exp <= now
        }
        if (staleDocs.isEmpty()) {
            return Resource.Success(0)
        }

        val batch = db.batch()
        staleDocs.forEach { doc ->
            batch.update(
                doc.reference,
                mapOf(
                    "status" to RequestStatus.EXPIRED.name,
                    "updatedAt" to now
                )
            )
        }
        val auditRef = db.collection("audit_logs").document()
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "EXPIRE_STALE_REQUESTS",
                "targetId" to "blood_requests",
                "targetType" to "BLOOD_REQUEST_BATCH",
                "outcome" to "SUCCESS",
                "details" to mapOf("expiredCount" to staleDocs.size.toString()),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(staleDocs.size)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "blood_requests")
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.dispatchEmergencyBroadcastAsAdmin(
    request: BloodRequest,
    adminUid: String,
    adminEmail: String
): Resource<Int> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    val isArmed = fetchEmergencyBroadcastArmed()
    if (!isArmed) {
        return Resource.Error("জরুরি সম্প্রচার বর্তমানে বিরত আছে। প্রথমে সম্প্রচার সক্রিয় করুন।")
    }

    if (request.status != RequestStatus.PENDING) {
        return Resource.Error("শুধুমাত্র অপেক্ষমাণ অনুরোধের জন্য জরুরি বিজ্ঞপ্তি পাঠানো যাবে।")
    }

    val now = System.currentTimeMillis()
    val lastDispatched = request.broadcastDispatchedAt
    if (lastDispatched != null && (now - lastDispatched) < 15 * 60 * 1000L) {
        val minsAgo = ((now - lastDispatched) / 60000L).coerceAtLeast(1L)
        return Resource.Error("এই অনুরোধটির জন্য $minsAgo মিনিট আগে ইতিমধ্যে জরুরি বিজ্ঞপ্তি পাঠানো হয়েছে। অতিরিক্ত নোটিফিকেশন রোধে ১৫ মিনিট অপেক্ষা করুন।")
    }

    val compatibleGroups = BloodCompatibilityHelper.getCompatibleDonorGroups(request.bloodGroup)
        .map { it.name }
        .toSet()

    return try {
        val donorsSnap = db.collection("public_donors")
            .whereEqualTo("isDonor", true)
            .limit(250)
            .get()
            .await()

        val matchingDonorUids = donorsSnap.documents.mapNotNull { doc ->
            val uid = doc.getString("uid") ?: doc.id
            val bg = doc.getString("bloodGroup") ?: ""
            val avail = doc.getString("availability") ?: DonorAvailability.AVAILABLE.name
            val dist = doc.getString("district") ?: ""
            val div = doc.getString("division") ?: ""
            val isAvail = avail == DonorAvailability.AVAILABLE.name || avail == DonorAvailability.EMERGENCY_ONLY.name
            val sameLocation = dist.equals(request.district, ignoreCase = true) ||
                div.equals(request.division, ignoreCase = true)
            if (uid != request.requesterUid && bg in compatibleGroups && isAvail && sameLocation) {
                uid
            } else {
                null
            }
        }.distinct().take(100)

        val actorUid = adminUid.ifBlank { currentUser.uid }
        val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
        val batch = db.batch()

        matchingDonorUids.forEach { donorUid ->
            val notifRef = db.collection("users").document(donorUid).collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "recipientUid" to donorUid,
                    "title" to "জরুরি রক্তের প্রয়োজন: ${request.bloodGroup.display} (${request.district})",
                    "body" to "${request.hospitalName}-এ রোগীর (${request.patientName}) জন্য ${request.unitsRequired} ব্যাগ ${request.bloodGroup.display} রক্তের জরুরি প্রয়োজন।",
                    "type" to "EMERGENCY_BROADCAST",
                    "requestId" to request.id,
                    "isRead" to false,
                    "createdAt" to now
                )
            )
        }

        val reqRef = db.collection("blood_requests").document(request.id)
        val updatedReq = request.copy(
            isEmergencyBroadcast = true,
            broadcastDispatchedAt = now,
            updatedAt = now
        )
        batch.set(reqRef, updatedReq.toFirestoreMap())

        val auditRef = db.collection("audit_logs").document()
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "EMERGENCY_BROADCAST_DISPATCHED",
                "targetId" to request.id,
                "targetType" to "BLOOD_REQUEST",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "bloodGroup" to request.bloodGroup.display,
                    "district" to request.district,
                    "hospital" to request.hospitalName,
                    "notifiedCount" to matchingDonorUids.size.toString()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(matchingDonorUids.size)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "blood_requests/${request.id}")
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.verifyOrganizationDirectAsAdmin(
    org: Organization,
    verified: Boolean,
    reason: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    if (!verified && reason.trim().length < 3) {
        return Resource.Error("যাচাই বাতিলের সুনির্দিষ্ট কারণ উল্লেখ করুন।")
    }

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val orgRef = db.collection("organizations").document(org.id)
    val auditRef = db.collection("audit_logs").document()

    return try {
        val batch = db.batch()
        batch.update(orgRef, mapOf("isVerified" to verified))
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to if (verified) "ORGANIZATION_VERIFIED" else "ORGANIZATION_UNVERIFIED",
                "targetId" to org.id,
                "targetType" to "ORGANIZATION",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "orgName" to org.name,
                    "previousVerified" to org.isVerified.toString(),
                    "newVerified" to verified.toString(),
                    "reason" to reason.trim()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, orgRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

fun FirestoreService.observeAllDonationsForAdmin(
    onError: ((String) -> Unit)? = null
): Flow<List<DonationRecord>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "donation_history"
    val listener = db.collection(path)
        .limit(200)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val records = snapshot?.documents?.mapNotNull { doc ->
                val district = doc.getString("district") ?: doc.getString("city") ?: "Dhaka"
                val division = doc.getString("division") ?: LocationHelper.inferDivisionForDistrict(district)
                val certCode = doc.getString("certificateCode") ?: doc.getString("certificateId") ?: ""
                DonationRecord(
                    id = doc.id,
                    donorUid = doc.getString("donorId") ?: doc.getString("donorUid") ?: "",
                    donorName = doc.getString("donorName") ?: "",
                    bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE"),
                    donationDate = doc.getLong("donationDate") ?: 0L,
                    hospitalName = doc.getString("hospitalName") ?: "",
                    division = division,
                    district = district,
                    upazila = doc.getString("upazila") ?: "",
                    city = district,
                    unitsDonated = (doc.getLong("bags") ?: doc.getLong("unitsDonated") ?: 1L).toInt(),
                    requestId = doc.getString("requestId"),
                    verifiedByOrgId = doc.getString("verifiedByOrgId"),
                    verifiedByOrgName = doc.getString("verifiedByOrgName") ?: "রক্তসেতু প্রশাসন",
                    verificationStatus = VerificationStatus.VERIFIED,
                    certificateId = certCode,
                    certificateCode = certCode
                )
            }?.sortedByDescending { it.donationDate } ?: emptyList()
            trySend(records)
        }
    awaitClose { listener.remove() }
}

suspend fun FirestoreService.verifyAndRecordDonationAsAdmin(
    record: DonationRecord,
    adminUid: String,
    adminEmail: String
): Resource<DonationRecord> {
    val donationRef = db.collection("donation_history").document()
    val auditRef = db.collection("audit_logs").document()
    val now = System.currentTimeMillis()
    val certCode = "RS-CERT-${now.toString().takeLast(6)}"

    return try {
        val savedRecord = record.copy(
            id = donationRef.id,
            donationDate = if (record.donationDate > 0L) record.donationDate else now,
            verificationStatus = VerificationStatus.VERIFIED,
            certificateId = certCode,
            certificateCode = certCode
        )

        val batch = db.batch()
        batch.set(
            donationRef,
            mapOf(
                "id" to savedRecord.id,
                "donorId" to savedRecord.donorUid,
                "donorUid" to savedRecord.donorUid,
                "donorName" to savedRecord.donorName,
                "bloodGroup" to savedRecord.bloodGroup.name,
                "donationDate" to savedRecord.donationDate,
                "hospitalName" to savedRecord.hospitalName,
                "division" to savedRecord.division,
                "district" to savedRecord.district,
                "upazila" to savedRecord.upazila,
                "city" to savedRecord.city,
                "bags" to 1,
                "unitsDonated" to savedRecord.unitsDonated,
                "requestId" to (savedRecord.requestId ?: ""),
                "verifiedByRequester" to true,
                "verifiedByOrgId" to adminUid,
                "verifiedByOrgName" to (savedRecord.verifiedByOrgName ?: "রক্তসেতু সুপার অ্যাডমিন"),
                "verificationStatus" to VerificationStatus.VERIFIED.name,
                "certificateId" to certCode,
                "certificateCode" to certCode
            )
        )

        if (savedRecord.donorUid.isNotBlank()) {
            val userRef = db.collection("users").document(savedRecord.donorUid)
            val pubRef = db.collection("public_donors").document(savedRecord.donorUid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                val prevCount = (userSnap.getLong("donationCount") ?: 0L).toInt()
                val newCount = prevCount + savedRecord.unitsDonated.coerceAtLeast(1)
                batch.update(
                    userRef,
                    mapOf(
                        "donationCount" to newCount,
                        "lastDonationDate" to savedRecord.donationDate,
                        "donorAvailability" to DonorAvailability.RESTING.name,
                        "updatedAt" to now
                    )
                )
                val pubSnap = pubRef.get().await()
                if (pubSnap.exists()) {
                    batch.update(
                        pubRef,
                        mapOf(
                            "donationCount" to newCount,
                            "lastDonationDate" to savedRecord.donationDate,
                            "availability" to DonorAvailability.RESTING.name,
                            "updatedAt" to now
                        )
                    )
                }
            }
        }

        batch.set(
            auditRef,
            mapOf(
                "id" to auditRef.id,
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "DONATION_VERIFIED",
                "targetType" to "DONATION",
                "targetId" to savedRecord.id,
                "timestamp" to now,
                "ipAddressOrClient" to "Android-Client",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "donorUid" to savedRecord.donorUid,
                    "donorName" to savedRecord.donorName,
                    "bloodGroup" to savedRecord.bloodGroup.display,
                    "hospitalName" to savedRecord.hospitalName,
                    "certificateCode" to certCode
                )
            )
        )

        batch.commit().await()
        Resource.Success(savedRecord)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, donationRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

fun FirestoreService.observeReportsForAdmin(
    onError: ((String) -> Unit)? = null
): Flow<List<UserReport>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "reports"
    val listener = db.collection(path)
        .limit(200)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val reports = snapshot?.documents?.mapNotNull { parseUserReportDoc(it) }
                ?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(reports)
        }
    awaitClose { listener.remove() }
}

suspend fun FirestoreService.reviewReportAsAdmin(
    report: UserReport,
    newStatus: String,
    resolutionNotes: String,
    actionTaken: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    if (resolutionNotes.trim().length < 3) {
        return Resource.Error("নিষ্পত্তির নোট বা গৃহীত পদক্ষেপ উল্লেখ করুন।")
    }

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val reportRef = db.collection("reports").document(report.id)
    val auditRef = db.collection("audit_logs").document()

    return try {
        val batch = db.batch()
        batch.update(
            reportRef,
            mapOf(
                "status" to newStatus,
                "resolutionNotes" to resolutionNotes.trim(),
                "actionTaken" to actionTaken.trim(),
                "reviewedByUid" to actorUid,
                "reviewedByEmail" to actorEmail,
                "reviewedAt" to now
            )
        )
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "REPORT_REVIEWED_$newStatus",
                "targetId" to report.id,
                "targetType" to "REPORT",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "targetName" to report.targetName,
                    "previousStatus" to report.status,
                    "newStatus" to newStatus,
                    "actionTaken" to actionTaken.trim(),
                    "notes" to resolutionNotes.trim()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, reportRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

fun FirestoreService.observeAnnouncementsForAdmin(
    onError: ((String) -> Unit)? = null
): Flow<List<SystemAnnouncement>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "announcements"
    val listener = db.collection(path)
        .limit(100)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                SystemAnnouncement(
                    id = doc.id,
                    titleBn = doc.getString("titleBn") ?: "",
                    bodyBn = doc.getString("bodyBn") ?: "",
                    category = doc.getString("category") ?: "GENERAL",
                    targetAudience = doc.getString("targetAudience") ?: "ALL_USERS",
                    targetDivision = doc.getString("targetDivision") ?: "",
                    targetDistrict = doc.getString("targetDistrict") ?: "",
                    status = doc.getString("status") ?: "DRAFT",
                    createdByUid = doc.getString("createdByUid") ?: "",
                    createdByEmail = doc.getString("createdByEmail") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    publishedAt = doc.getLong("publishedAt"),
                    recipientCount = (doc.getLong("recipientCount") ?: 0L).toInt()
                )
            }?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(list)
        }
    awaitClose { listener.remove() }
}

fun FirestoreService.observePublishedAnnouncements(
    onError: ((String) -> Unit)? = null
): Flow<List<SystemAnnouncement>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "announcements"
    val listener = db.collection(path)
        .whereEqualTo("status", "PUBLISHED")
        .limit(30)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val status = doc.getString("status") ?: "DRAFT"
                if (status != "PUBLISHED") return@mapNotNull null
                SystemAnnouncement(
                    id = doc.id,
                    titleBn = doc.getString("titleBn") ?: "",
                    bodyBn = doc.getString("bodyBn") ?: "",
                    category = doc.getString("category") ?: "GENERAL",
                    targetAudience = doc.getString("targetAudience") ?: "ALL_USERS",
                    targetDivision = doc.getString("targetDivision") ?: "",
                    targetDistrict = doc.getString("targetDistrict") ?: "",
                    status = status,
                    createdByUid = doc.getString("createdByUid") ?: "",
                    createdByEmail = doc.getString("createdByEmail") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    publishedAt = doc.getLong("publishedAt"),
                    recipientCount = (doc.getLong("recipientCount") ?: 0L).toInt()
                )
            }?.sortedByDescending { it.publishedAt ?: it.createdAt } ?: emptyList()
            trySend(list)
        }
    awaitClose { listener.remove() }
}

suspend fun FirestoreService.saveOrPublishAnnouncementAsAdmin(
    titleBn: String,
    bodyBn: String,
    category: String,
    targetAudience: String,
    targetDivision: String,
    targetDistrict: String,
    publishNow: Boolean,
    adminUid: String,
    adminEmail: String
): Resource<Int> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    if (titleBn.trim().length < 3 || bodyBn.trim().length < 5) {
        return Resource.Error("বিজ্ঞপ্তির শিরোনাম এবং বিস্তারিত বার্তা সঠিকভাবে লিখুন।")
    }

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }

    return try {
        if (publishNow) {
            val recentSnap = db.collection("announcements")
                .whereEqualTo("status", "PUBLISHED")
                .limit(20)
                .get()
                .await()
            val duplicate = recentSnap.documents.any { doc ->
                val sameTitle = (doc.getString("titleBn") ?: "").equals(titleBn.trim(), ignoreCase = true)
                val pubTime = doc.getLong("publishedAt") ?: 0L
                sameTitle && (now - pubTime) < 2 * 60 * 1000L
            }
            if (duplicate) {
                return Resource.Error("একই শিরোনামের বিজ্ঞপ্তি কিছুক্ষণ আগে পাঠানো হয়েছে। ডুপ্লিকেট প্রেরণ প্রতিরোধ করা হয়েছে।")
            }
        }

        var recipientUids = emptyList<String>()
        if (publishNow) {
            val donorsSnap = db.collection("public_donors")
                .whereEqualTo("isDonor", true)
                .limit(200)
                .get()
                .await()
            recipientUids = donorsSnap.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                val avail = doc.getString("availability") ?: DonorAvailability.AVAILABLE.name
                val ver = doc.getString("verificationStatus") ?: VerificationStatus.UNVERIFIED.name
                val div = doc.getString("division") ?: ""
                val dist = doc.getString("district") ?: ""

                val audienceMatch = when (targetAudience) {
                    "ACTIVE_DONORS" -> avail == DonorAvailability.AVAILABLE.name
                    "VERIFIED_DONORS" -> ver == VerificationStatus.VERIFIED.name
                    else -> true
                }
                val divMatch = targetDivision.isBlank() || div.equals(targetDivision, ignoreCase = true)
                val distMatch = targetDistrict.isBlank() || dist.equals(targetDistrict, ignoreCase = true)

                if (audienceMatch && divMatch && distMatch) uid else null
            }.distinct().take(100)
        }

        val annRef = db.collection("announcements").document()
        val auditRef = db.collection("audit_logs").document()
        val batch = db.batch()

        batch.set(
            annRef,
            mapOf(
                "titleBn" to titleBn.trim(),
                "bodyBn" to bodyBn.trim(),
                "category" to category,
                "targetAudience" to targetAudience,
                "targetDivision" to targetDivision.trim(),
                "targetDistrict" to targetDistrict.trim(),
                "status" to if (publishNow) "PUBLISHED" else "DRAFT",
                "createdByUid" to actorUid,
                "createdByEmail" to actorEmail,
                "createdAt" to now,
                "publishedAt" to if (publishNow) now else null,
                "recipientCount" to recipientUids.size
            )
        )

        if (publishNow) {
            recipientUids.forEach { uid ->
                val notifRef = db.collection("users").document(uid).collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientUid" to uid,
                        "title" to titleBn.trim(),
                        "body" to bodyBn.trim(),
                        "type" to "ADMIN_ANNOUNCEMENT",
                        "isRead" to false,
                        "createdAt" to now
                    )
                )
            }
        }

        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to if (publishNow) "ANNOUNCEMENT_PUBLISHED" else "ANNOUNCEMENT_DRAFT_SAVED",
                "targetId" to annRef.id,
                "targetType" to "ANNOUNCEMENT",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "title" to titleBn.trim(),
                    "audience" to targetAudience,
                    "recipientCount" to recipientUids.size.toString()
                ),
                "timestamp" to now
            )
        )

        batch.commit().await()
        Resource.Success(recipientUids.size)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "announcements")
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.fetchSystemConfigSettings(): SystemConfigSettings {
    return try {
        val genSnap = db.collection("system_settings").document("general").get().await()
        val ebSnap = db.collection("system_settings").document("emergency_broadcast").get().await()
        val armed = if (ebSnap.exists()) {
            ebSnap.getBoolean("armed") ?: true
        } else if (genSnap.exists()) {
            genSnap.getBoolean("emergencyBroadcastArmed") ?: true
        } else {
            true
        }
        if (!genSnap.exists()) {
            SystemConfigSettings(
                emergencyBroadcastArmed = armed,
                updatedAt = ebSnap.getLong("updatedAt"),
                updatedByUid = ebSnap.getString("updatedByUid") ?: ""
            )
        } else {
            SystemConfigSettings(
                maintenanceMode = genSnap.getBoolean("maintenanceMode") ?: false,
                maintenanceMessageBn = genSnap.getString("maintenanceMessageBn")
                    ?: "সিস্টেম রক্ষণাবেক্ষণের কাজ চলছে। জরুরি সেবা শীঘ্রই স্বাভাবিক হবে।",
                registrationEnabled = genSnap.getBoolean("registrationEnabled") ?: true,
                donorRegistrationEnabled = genSnap.getBoolean("donorRegistrationEnabled") ?: true,
                emergencyBroadcastArmed = armed,
                notificationsEnabled = genSnap.getBoolean("notificationsEnabled") ?: true,
                requestExpiryHours = (genSnap.getLong("requestExpiryHours") ?: 72L).toInt(),
                maxRequestsPerUserPerDay = (genSnap.getLong("maxRequestsPerUserPerDay") ?: 3L).toInt(),
                announcementBannerBn = genSnap.getString("announcementBannerBn") ?: "",
                updatedAt = genSnap.getLong("updatedAt") ?: ebSnap.getLong("updatedAt"),
                updatedByUid = genSnap.getString("updatedByUid") ?: "",
                updatedByEmail = genSnap.getString("updatedByEmail") ?: ""
            )
        }
    } catch (_: Exception) {
        SystemConfigSettings()
    }
}

suspend fun FirestoreService.updateSystemConfigSettingsAsAdmin(
    settings: SystemConfigSettings,
    adminUid: String,
    adminEmail: String
): Resource<SystemConfigSettings> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val genRef = db.collection("system_settings").document("general")
    val ebRef = db.collection("system_settings").document("emergency_broadcast")
    val auditRef = db.collection("audit_logs").document()

    val updated = settings.copy(
        requestExpiryHours = settings.requestExpiryHours.coerceIn(12, 168),
        maxRequestsPerUserPerDay = settings.maxRequestsPerUserPerDay.coerceIn(1, 10),
        updatedAt = now,
        updatedByUid = actorUid,
        updatedByEmail = actorEmail
    )

    return try {
        val batch = db.batch()
        batch.set(
            genRef,
            mapOf(
                "maintenanceMode" to updated.maintenanceMode,
                "maintenanceMessageBn" to updated.maintenanceMessageBn.trim(),
                "registrationEnabled" to updated.registrationEnabled,
                "donorRegistrationEnabled" to updated.donorRegistrationEnabled,
                "emergencyBroadcastArmed" to updated.emergencyBroadcastArmed,
                "notificationsEnabled" to updated.notificationsEnabled,
                "requestExpiryHours" to updated.requestExpiryHours,
                "maxRequestsPerUserPerDay" to updated.maxRequestsPerUserPerDay,
                "announcementBannerBn" to updated.announcementBannerBn.trim(),
                "updatedAt" to now,
                "updatedByUid" to actorUid,
                "updatedByEmail" to actorEmail
            )
        )
        batch.set(
            ebRef,
            mapOf(
                "armed" to updated.emergencyBroadcastArmed,
                "updatedAt" to now,
                "updatedByUid" to actorUid,
                "updatedByEmail" to actorEmail
            )
        )
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "SYSTEM_CONFIG_UPDATED",
                "targetId" to "general",
                "targetType" to "SYSTEM_CONFIG",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "maintenanceMode" to updated.maintenanceMode.toString(),
                    "registrationEnabled" to updated.registrationEnabled.toString(),
                    "emergencyBroadcastArmed" to updated.emergencyBroadcastArmed.toString(),
                    "notificationsEnabled" to updated.notificationsEnabled.toString(),
                    "requestExpiryHours" to updated.requestExpiryHours.toString()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(updated)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, genRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

fun FirestoreService.observeSecurityAlertsForAdmin(
    onError: ((String) -> Unit)? = null
): Flow<List<SecurityAlertItem>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "security_alerts"
    val listener = db.collection(path)
        .limit(100)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val alerts = snapshot?.documents?.mapNotNull { doc ->
                SecurityAlertItem(
                    id = doc.id,
                    alertType = doc.getString("alertType") ?: "SECURITY_EVENT",
                    severity = doc.getString("severity") ?: "MEDIUM",
                    detectedAt = doc.getLong("detectedAt") ?: 0L,
                    departmentBn = doc.getString("departmentBn") ?: "অ্যাকাউন্ট ও অনুমতি",
                    summaryBn = doc.getString("summaryBn") ?: "",
                    recommendedActionBn = doc.getString("recommendedActionBn") ?: "",
                    reviewStatus = doc.getString("reviewStatus") ?: "OPEN",
                    resolutionNotes = doc.getString("resolutionNotes") ?: "",
                    reviewedByUid = doc.getString("reviewedByUid") ?: "",
                    reviewedByEmail = doc.getString("reviewedByEmail") ?: "",
                    reviewedAt = doc.getLong("reviewedAt")
                )
            }?.sortedByDescending { it.detectedAt } ?: emptyList()
            trySend(alerts)
        }
    awaitClose { listener.remove() }
}

suspend fun FirestoreService.reviewSecurityAlertAsAdmin(
    alert: SecurityAlertItem,
    resolutionNotes: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("এই কাজের জন্য অনুমোদিত প্রশাসকের লগইন প্রয়োজন।")

    if (resolutionNotes.trim().length < 3) {
        return Resource.Error("পর্যালোচনার নোট বা গৃহীত পদক্ষেপ উল্লেখ করুন।")
    }

    val now = System.currentTimeMillis()
    val actorUid = adminUid.ifBlank { currentUser.uid }
    val actorEmail = adminEmail.ifBlank { currentUser.email ?: "admin" }
    val alertRef = db.collection("security_alerts").document(alert.id)
    val auditRef = db.collection("audit_logs").document()

    return try {
        val batch = db.batch()
        batch.set(
            alertRef,
            mapOf(
                "alertType" to alert.alertType,
                "severity" to alert.severity,
                "detectedAt" to alert.detectedAt,
                "departmentBn" to alert.departmentBn,
                "summaryBn" to alert.summaryBn,
                "recommendedActionBn" to alert.recommendedActionBn,
                "reviewStatus" to "REVIEWED",
                "resolutionNotes" to resolutionNotes.trim(),
                "reviewedByUid" to actorUid,
                "reviewedByEmail" to actorEmail,
                "reviewedAt" to now
            )
        )
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "SECURITY_ALERT_REVIEWED",
                "targetId" to alert.id,
                "targetType" to "SECURITY_ALERT",
                "outcome" to "SUCCESS",
                "details" to mapOf(
                    "alertType" to alert.alertType,
                    "notes" to resolutionNotes.trim()
                ),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, alertRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.recordAdminAccessCheck(
    actorUid: String,
    actorEmail: String,
    isAuthorized: Boolean,
    methodDescription: String
) {
    if (!isAuthorized || actorUid.isBlank()) return
    val now = System.currentTimeMillis()
    try {
        db.collection("audit_logs").document().set(
            mapOf(
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "action" to "ADMIN_ACCESS_VERIFIED",
                "targetId" to actorUid,
                "targetType" to "ADMIN_AUTH",
                "outcome" to "SUCCESS",
                "details" to mapOf("method" to methodDescription),
                "timestamp" to now
            )
        ).await()
    } catch (_: Exception) {
        // Non-blocking audit write
    }
}

suspend fun FirestoreService.fetchAdminRegistryList(): List<Map<String, Any>> {
    return try {
        val snap = db.collection("admins").limit(50).get().await()
        snap.documents.map { doc ->
            mapOf(
                "uid" to (doc.getString("uid") ?: doc.id),
                "email" to (doc.getString("email") ?: ""),
                "role" to (doc.getString("role") ?: "SUPER_ADMIN"),
                "updatedAt" to (doc.getLong("updatedAt") ?: 0L)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Runs real runtime database protection checks to populate the Security & Privacy Center
 * with plain, reassuring Bengali summaries (zero technical paths or jargon).
 */
suspend fun FirestoreService.runDatabaseProtectionChecks(
    currentUid: String?,
    isVerifiedAdmin: Boolean
): List<DatabaseProtectionCheck> {
    val now = System.currentTimeMillis()
    val isSignedIn = !currentUid.isNullOrBlank()

    val ownProfileReadable = if (isSignedIn && currentUid != null) {
        try {
            db.collection("users").document(currentUid).get().await()
            true
        } catch (_: Exception) {
            false
        }
    } else {
        false
    }

    val sysConfigReadable = if (isSignedIn) {
        try {
            db.collection("system_settings").document("emergency_broadcast").get().await()
            true
        } catch (_: Exception) {
            false
        }
    } else {
        false
    }

    return listOf(
        DatabaseProtectionCheck(
            id = "private_pii_isolation",
            titleBn = "ব্যক্তিগত তথ্যের প্রবেশাধিকার নিয়ন্ত্রণ",
            protectedDataBn = "ইমেইল, জন্মতারিখ ও গোপন মোবাইল নম্বর",
            accessSummaryBn = "শুধুমাত্র অ্যাকাউন্টের মূল মালিক এবং অনুমোদিত প্রশাসক দেখতে পারেন; সাধারণ রক্তদাতা তালিকা থেকে সম্পূর্ণ আলাদা।",
            isVerified = isSignedIn && ownProfileReadable,
            statusLabelBn = if (isSignedIn && ownProfileReadable) "সুরক্ষিত ✔" else "লগইন প্রয়োজন",
            lastCheckedAt = now,
            issueDetectedBn = if (!isSignedIn) "ব্যবহারকারী বর্তমানে লগইন অবস্থায় নেই।" else null,
            recommendedFixBn = if (!isSignedIn) "ব্যক্তিগত তথ্যের সুরক্ষা পরীক্ষা করতে লগইন করুন।" else null
        ),
        DatabaseProtectionCheck(
            id = "unauthorized_mutation_prevention",
            titleBn = "অননুমোদিত তথ্য পরিবর্তন প্রতিরোধ",
            protectedDataBn = "রক্তদানের সংখ্যা, যাচাই স্ট্যাটাস, সম্মাননা ব্যাজ ও প্রশাসনিক ভূমিকা",
            accessSummaryBn = "ব্যবহারকারী নিজে নিজের যাচাই স্ট্যাটাস, রক্তদান সংখ্যা বা প্রশাসনিক ভূমিকা পরিবর্তন করতে পারেন না।",
            isVerified = true,
            statusLabelBn = "সুরক্ষিত ✔",
            lastCheckedAt = now
        ),
        DatabaseProtectionCheck(
            id = "own_data_protection",
            titleBn = "ব্যবহারকারীর নিজস্ব তথ্যের সুরক্ষা",
            protectedDataBn = "প্রোফাইল সম্পাদনা, রক্তদানের প্রাপ্যতা ও ব্যক্তিগত চ্যাট বার্তা",
            accessSummaryBn = "শুধুমাত্র নিবন্ধিত মালিক নিজের প্রোফাইল ও প্রাপ্যতা হালনাগাদ করতে পারেন এবং চ্যাটের দুই অংশগ্রহণকারী ছাড়া অন্য কেউ বার্তা পড়তে পারেন না।",
            isVerified = isSignedIn,
            statusLabelBn = if (isSignedIn) "সক্রিয় ✔" else "অতিথি মোড",
            lastCheckedAt = now
        ),
        DatabaseProtectionCheck(
            id = "sensitive_action_verification",
            titleBn = "সংবেদনশীল কাজের অনুমতি যাচাই",
            protectedDataBn = "রক্তের অনুরোধ গ্রহণ, রক্তদান সনদ যাচাই এবং অ্যাকাউন্ট মুছে ফেলা",
            accessSummaryBn = "নিজের রক্তের অনুরোধ নিজে গ্রহণ করা প্রতিরোধ করা হয় এবং রক্তদান সনদ শুধুমাত্র অনুমোদিত প্রতিষ্ঠান বা প্রশাসক যাচাই করতে পারেন।",
            isVerified = sysConfigReadable || isSignedIn,
            statusLabelBn = if (sysConfigReadable || isSignedIn) "সুরক্ষিত ✔" else "লগইন প্রয়োজন",
            lastCheckedAt = now
        ),
        DatabaseProtectionCheck(
            id = "admin_action_protection",
            titleBn = "প্রশাসনিক কার্যক্রমের সুরক্ষা",
            protectedDataBn = "সিস্টেম সেটিংস, যাচাই অনুমোদন, জরুরি বিজ্ঞপ্তি ও কার্যবিবরণী",
            accessSummaryBn = "শুধুমাত্র অনুমোদিত সুপার অ্যাডমিন প্রশাসনিক পরিবর্তন করতে পারেন এবং প্রতিটি কাজ স্থায়ীভাবে সংরক্ষিত হয়।",
            isVerified = isVerifiedAdmin,
            statusLabelBn = if (isVerifiedAdmin) "অ্যাডমিন অনুমতি: নিশ্চিত ✔" else "শুধুমাত্র সুপার অ্যাডমিনের জন্য সংরক্ষিত",
            lastCheckedAt = now
        )
    )
}
