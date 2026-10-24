package com.example.data.firebase

import com.example.model.AmbulanceService
import com.example.model.BloodBank
import com.example.model.BloodDonationCamp
import com.example.model.BloodGroup
import com.example.model.UserReport
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Phase 3 (User Report Submission & Rate-Limiting) and
 * Phase 4 (Blood Banks, Ambulances, Blood Donation Camps & Persistent RSVPs)
 * operations on `FirestoreService`.
 */

// =========================================================================
// Phase 3: User Complaint / Report Submission & History (/reports)
// =========================================================================

val ALLOWED_REPORT_CATEGORIES = setOf(
    "FAKE_REQUEST",
    "HARASSMENT",
    "WRONG_INFO",
    "OTHER",
    "MISUSE",
    "SPAM"
)

val ALLOWED_REPORT_TARGET_TYPES = setOf(
    "USER",
    "REQUEST",
    "DONOR",
    "CHAT",
    "GENERAL"
)

/**
 * Submits a user complaint/report to `/reports/{reportId}`.
 * Enforces:
 * - Self-report prevention (`targetUid != reporterUid`)
 * - Details length validation (10–500 characters)
 * - Category & targetType validation
 * - 24-hour rate limit against repeated reports on the same target by the same user
 */
suspend fun FirestoreService.submitUserReport(
    reporterUid: String,
    reporterName: String,
    targetUid: String,
    targetName: String,
    targetType: String,
    category: String,
    reasonBn: String,
    details: String,
    severity: String = "MEDIUM"
): Resource<String> {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    } ?: return Resource.Error("অভিযোগ জমা দিতে অনুগ্রহ করে লগইন করুন।")

    val effectiveReporterUid = currentUser.uid
    val cleanTargetUid = targetUid.trim().ifBlank { "general_report" }

    if (cleanTargetUid == effectiveReporterUid) {
        return Resource.Error("আপনি নিজেকে রিপোর্ট করতে পারবেন না।")
    }

    val cleanDetails = details.trim()
    if (cleanDetails.length < 10) {
        return Resource.Error("অভিযোগের বিবরণ কমপক্ষে ১০ অক্ষরের হতে হবে।")
    }
    if (cleanDetails.length > 500) {
        return Resource.Error("অভিযোগের বিবরণ সর্বোচ্চ ৫০০ অক্ষরের হতে পারবে।")
    }

    val normalizedCategory = category.trim().uppercase().let {
        if (it in ALLOWED_REPORT_CATEGORIES) it else "OTHER"
    }
    val normalizedTargetType = when (targetType.trim().uppercase()) {
        "BLOOD_REQUEST", "REQUEST" -> "REQUEST"
        "DONOR" -> "DONOR"
        "CHAT" -> "CHAT"
        "USER" -> "USER"
        else -> "GENERAL"
    }

    val now = System.currentTimeMillis()
    val twentyFourHoursAgo = now - 24 * 60 * 60 * 1000L

    // Rate-limit check: prevent duplicate reports on the same target within 24 hours
    try {
        val ownRecentSnap = db.collection("reports")
            .whereEqualTo("reporterUid", effectiveReporterUid)
            .limit(50)
            .get()
            .await()

        val alreadyReportedRecently = ownRecentSnap.documents.any { doc ->
            val docTarget = doc.getString("targetUid") ?: ""
            val createdAt = doc.getLong("createdAt") ?: 0L
            docTarget == cleanTargetUid && createdAt >= twentyFourHoursAgo
        }
        if (alreadyReportedRecently) {
            return Resource.Error("আপনি গত ২৪ ঘণ্টায় এই বিষয়ে ইতিমধ্যে একটি রিপোর্ট জমা দিয়েছেন। আমাদের টিম এটি পর্যালোচনা করছে।")
        }
    } catch (_: Exception) {
        // Proceed if offline or index warming up; rules still enforce schema
    }

    val effectiveReasonBn = reasonBn.trim().ifBlank {
        when (normalizedCategory) {
            "FAKE_REQUEST" -> "ভুয়া রিকোয়েস্ট"
            "HARASSMENT" -> "অসদাচরণ"
            "WRONG_INFO" -> "ভুল তথ্য"
            else -> "অন্যান্য"
        }
    }

    val docRef = db.collection("reports").document()
    val payload = mapOf(
        "reporterUid" to effectiveReporterUid,
        "reporterName" to reporterName.trim().ifBlank { currentUser.displayName ?: "ব্যবহারকারী" },
        "targetUid" to cleanTargetUid,
        "targetName" to targetName.trim().ifBlank { "সাধারণ লক্ষ্য" },
        "targetType" to normalizedTargetType,
        "category" to normalizedCategory,
        "reasonBn" to effectiveReasonBn,
        "details" to cleanDetails,
        "severity" to severity,
        "status" to "OPEN",
        "createdAt" to now
    )

    return try {
        docRef.set(payload).await()
        Resource.Success(docRef.id)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

/**
 * Observes only the current signed-in user's own submitted reports (`/reports` where `reporterUid == uid`).
 */
fun FirestoreService.observeMyReports(
    reporterUid: String,
    onError: ((String) -> Unit)? = null
): Flow<List<UserReport>> = callbackFlow {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser == null || reporterUid.isBlank()) {
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    val path = "reports"
    val listener = db.collection(path)
        .whereEqualTo("reporterUid", reporterUid)
        .limit(50)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { parseUserReportDoc(it) }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
            trySend(list)
        }

    awaitClose { listener.remove() }
}

internal fun parseUserReportDoc(doc: DocumentSnapshot): UserReport? {
    return try {
        UserReport(
            id = doc.id,
            reporterUid = doc.getString("reporterUid") ?: "",
            reporterName = doc.getString("reporterName") ?: "",
            targetUid = doc.getString("targetUid") ?: "",
            targetName = doc.getString("targetName") ?: "",
            targetType = doc.getString("targetType") ?: "USER",
            category = doc.getString("category") ?: "OTHER",
            reasonBn = doc.getString("reasonBn") ?: doc.getString("reason") ?: "",
            details = doc.getString("details") ?: "",
            severity = doc.getString("severity") ?: "MEDIUM",
            status = doc.getString("status") ?: "OPEN",
            resolutionNotes = doc.getString("resolutionNotes") ?: "",
            actionTaken = doc.getString("actionTaken") ?: "",
            reviewedByUid = doc.getString("reviewedByUid") ?: "",
            reviewedByEmail = doc.getString("reviewedByEmail") ?: "",
            reviewedAt = doc.getLong("reviewedAt"),
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    } catch (_: Exception) {
        null
    }
}

// =========================================================================
// Phase 4: Blood Banks, Ambulances & Blood Camps (/blood_banks, /ambulances, /blood_camps)
// =========================================================================

fun FirestoreService.observeBloodBanks(
    includeInactive: Boolean = false,
    onError: ((String) -> Unit)? = null
): Flow<List<BloodBank>> = callbackFlow {
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

    val path = "blood_banks"
    val listener = db.collection(path)
        .limit(200)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val isActive = doc.getBoolean("isActive") ?: true
                if (!includeInactive && !isActive) return@mapNotNull null
                val rawGroups = (doc.get("availableGroups") as? List<*>)?.mapNotNull {
                    (it as? String)?.let { g -> BloodGroup.fromDisplay(g) }
                } ?: listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE)
                val city = doc.getString("city") ?: doc.getString("district") ?: "Dhaka"
                val district = doc.getString("district") ?: city
                val phone = doc.getString("phone") ?: doc.getString("contactPhone") ?: ""
                BloodBank(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    nameBn = doc.getString("nameBn") ?: doc.getString("name") ?: "",
                    organization = doc.getString("organization") ?: "",
                    address = doc.getString("address") ?: "",
                    city = city,
                    district = district,
                    phone = phone,
                    emergencyHotline = doc.getString("emergencyHotline") ?: phone,
                    is24Hours = doc.getBoolean("is24Hours") ?: true,
                    availableGroups = rawGroups,
                    verified = doc.getBoolean("verified") ?: true,
                    isActive = isActive,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }?.sortedBy { it.name } ?: emptyList()
            trySend(list)
        }
    awaitClose { listener.remove() }
}

fun FirestoreService.observeAmbulances(
    includeInactive: Boolean = false,
    onError: ((String) -> Unit)? = null
): Flow<List<AmbulanceService>> = callbackFlow {
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

    val path = "ambulances"
    val listener = db.collection(path)
        .limit(200)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val isActive = doc.getBoolean("isActive") ?: true
                if (!includeInactive && !isActive) return@mapNotNull null
                val district = doc.getString("district") ?: doc.getString("city") ?: "Dhaka"
                val provider = doc.getString("provider") ?: doc.getString("providerName") ?: ""
                val title = doc.getString("title") ?: provider
                AmbulanceService(
                    id = doc.id,
                    title = title,
                    titleBn = doc.getString("titleBn") ?: title,
                    provider = provider,
                    coverageArea = doc.getString("coverageArea") ?: district,
                    district = district,
                    city = doc.getString("city") ?: district,
                    phone = doc.getString("phone") ?: "",
                    type = doc.getString("type") ?: "AC",
                    isAvailable24x7 = doc.getBoolean("isAvailable24x7") ?: true,
                    isActive = isActive,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }?.sortedBy { it.title } ?: emptyList()
            trySend(list)
        }
    awaitClose { listener.remove() }
}

fun FirestoreService.observeBloodCamps(
    includeInactive: Boolean = false,
    currentUid: String? = null,
    onError: ((String) -> Unit)? = null
): Flow<List<BloodDonationCamp>> = callbackFlow {
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

    val effectiveUid = currentUid ?: currentUser.uid
    val path = "blood_camps"
    val listener = db.collection(path)
        .limit(200)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }
            val docs = snapshot?.documents ?: emptyList()
            val baseCamps = docs.mapNotNull { doc ->
                val isActive = doc.getBoolean("isActive") ?: true
                if (!includeInactive && !isActive) return@mapNotNull null
                val district = doc.getString("district") ?: doc.getString("city") ?: "Dhaka"
                BloodDonationCamp(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    titleBn = doc.getString("titleBn") ?: doc.getString("title") ?: "",
                    organizer = doc.getString("organizer") ?: "",
                    location = doc.getString("location") ?: "",
                    district = district,
                    city = doc.getString("city") ?: district,
                    date = doc.getString("date") ?: "",
                    time = doc.getString("time") ?: "",
                    contactPhone = doc.getString("contactPhone") ?: "",
                    targetUnits = (doc.getLong("targetUnits") ?: 100L).toInt(),
                    registeredCount = (doc.getLong("registeredCount") ?: 0L).toInt(),
                    isUserRsvp = false,
                    description = doc.getString("description") ?: "",
                    isActive = isActive,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }

            // Emit immediately with base camps, then enrich with real RSVP counts & user RSVP status
            trySend(baseCamps)

            if (baseCamps.isNotEmpty()) {
                val enriched = mutableListOf<BloodDonationCamp>()
                var completed = 0
                baseCamps.forEach { camp ->
                    db.collection("blood_camps")
                        .document(camp.id)
                        .collection("rsvps")
                        .get()
                        .addOnSuccessListener { rsvpSnap ->
                            val rsvpDocs = rsvpSnap.documents
                            val hasUserRsvp = effectiveUid.isNotBlank() && rsvpDocs.any { it.id == effectiveUid }
                            val totalCount = maxOf(camp.registeredCount, rsvpDocs.size)
                            synchronized(enriched) {
                                enriched.add(
                                    camp.copy(
                                        registeredCount = totalCount,
                                        isUserRsvp = hasUserRsvp
                                    )
                                )
                                completed++
                                if (completed == baseCamps.size) {
                                    trySend(enriched.sortedBy { it.id })
                                }
                            }
                        }
                        .addOnFailureListener {
                            synchronized(enriched) {
                                enriched.add(camp)
                                completed++
                                if (completed == baseCamps.size) {
                                    trySend(enriched.sortedBy { it.id })
                                }
                            }
                        }
                }
            }
        }
    awaitClose { listener.remove() }
}

/**
 * Toggles a user's persistent RSVP in `/blood_camps/{campId}/rsvps/{uid}`.
 * Users only create or delete their own RSVP document; they never write `registeredCount` directly.
 */
suspend fun FirestoreService.toggleCampRsvpInFirestore(
    campId: String,
    uid: String,
    userName: String,
    currentlyRsvped: Boolean
): Resource<Boolean> {
    if (campId.isBlank() || uid.isBlank()) {
        return Resource.Error("ক্যাম্পে অংশগ্রহণ নিশ্চিত করতে অনুগ্রহ করে লগইন করুন।")
    }
    val rsvpRef = db.collection("blood_camps")
        .document(campId)
        .collection("rsvps")
        .document(uid)

    return try {
        val existingSnap = rsvpRef.get().await()
        val shouldDelete = currentlyRsvped || existingSnap.exists()
        if (shouldDelete) {
            rsvpRef.delete().await()
            Resource.Success(false)
        } else {
            val payload = mapOf(
                "uid" to uid,
                "campId" to campId,
                "userName" to userName.trim().ifBlank { "স্বেচ্ছাসেবী" },
                "createdAt" to System.currentTimeMillis()
            )
            rsvpRef.set(payload).await()
            Resource.Success(true)
        }
    } catch (e: Exception) {
        handleFirestoreError(e, if (currentlyRsvped) OperationType.DELETE else OperationType.CREATE, rsvpRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

// =========================================================================
// Admin CRUD for Blood Banks, Ambulances, and Blood Camps
// =========================================================================

suspend fun FirestoreService.saveBloodBankAsAdmin(
    bank: BloodBank,
    adminUid: String,
    adminEmail: String
): Resource<BloodBank> {
    if (bank.name.trim().length < 2) {
        return Resource.Error("ব্লাড ব্যাংকের নাম কমপক্ষে ২ অক্ষরের হতে হবে।")
    }
    if (!FirebaseSecurityHelper.isValidEmergencyOrContactPhone(bank.phone)) {
        return Resource.Error("সঠিক ফোন বা জরুরি হটলাইন নম্বর দিন।")
    }
    val now = System.currentTimeMillis()
    val docRef = if (bank.id.isNotBlank()) {
        db.collection("blood_banks").document(bank.id)
    } else {
        db.collection("blood_banks").document()
    }
    val saved = bank.copy(
        id = docRef.id,
        district = bank.district.ifBlank { bank.city.ifBlank { "Dhaka" } },
        city = bank.city.ifBlank { bank.district.ifBlank { "Dhaka" } },
        updatedAt = now,
        createdAt = if (bank.createdAt > 0L) bank.createdAt else now
    )
    val auditRef = db.collection("audit_logs").document()
    return try {
        val batch = db.batch()
        batch.set(docRef, saved.toFirestoreMap())
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "BLOOD_BANK_SAVED",
                "targetId" to saved.id,
                "targetType" to "BLOOD_BANK",
                "outcome" to "SUCCESS",
                "details" to mapOf("name" to saved.nameBn.ifBlank { saved.name }, "district" to saved.district),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(saved)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.deleteBloodBankAsAdmin(
    bankId: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    if (bankId.isBlank()) return Resource.Error("ব্লাড ব্যাংক আইডি পাওয়া যায়নি।")
    val docRef = db.collection("blood_banks").document(bankId)
    val auditRef = db.collection("audit_logs").document()
    val now = System.currentTimeMillis()
    return try {
        val batch = db.batch()
        batch.delete(docRef)
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "BLOOD_BANK_DELETED",
                "targetId" to bankId,
                "targetType" to "BLOOD_BANK",
                "outcome" to "SUCCESS",
                "details" to mapOf("bankId" to bankId),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.saveAmbulanceAsAdmin(
    ambulance: AmbulanceService,
    adminUid: String,
    adminEmail: String
): Resource<AmbulanceService> {
    if (ambulance.title.trim().length < 2 && ambulance.provider.trim().length < 2) {
        return Resource.Error("অ্যাম্বুলেন্স সেবার নাম কমপক্ষে ২ অক্ষরের হতে হবে।")
    }
    if (!FirebaseSecurityHelper.isValidEmergencyOrContactPhone(ambulance.phone)) {
        return Resource.Error("সঠিক ফোন বা জরুরি হটলাইন নম্বর দিন।")
    }
    val now = System.currentTimeMillis()
    val docRef = if (ambulance.id.isNotBlank()) {
        db.collection("ambulances").document(ambulance.id)
    } else {
        db.collection("ambulances").document()
    }
    val saved = ambulance.copy(
        id = docRef.id,
        district = ambulance.district.ifBlank { ambulance.city.ifBlank { "Dhaka" } },
        city = ambulance.city.ifBlank { ambulance.district.ifBlank { "Dhaka" } },
        updatedAt = now,
        createdAt = if (ambulance.createdAt > 0L) ambulance.createdAt else now
    )
    val auditRef = db.collection("audit_logs").document()
    return try {
        val batch = db.batch()
        batch.set(docRef, saved.toFirestoreMap())
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "AMBULANCE_SAVED",
                "targetId" to saved.id,
                "targetType" to "AMBULANCE",
                "outcome" to "SUCCESS",
                "details" to mapOf("title" to saved.titleBn.ifBlank { saved.title }, "district" to saved.district),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(saved)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.deleteAmbulanceAsAdmin(
    ambulanceId: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    if (ambulanceId.isBlank()) return Resource.Error("অ্যাম্বুলেন্স আইডি পাওয়া যায়নি।")
    val docRef = db.collection("ambulances").document(ambulanceId)
    val auditRef = db.collection("audit_logs").document()
    val now = System.currentTimeMillis()
    return try {
        val batch = db.batch()
        batch.delete(docRef)
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "AMBULANCE_DELETED",
                "targetId" to ambulanceId,
                "targetType" to "AMBULANCE",
                "outcome" to "SUCCESS",
                "details" to mapOf("ambulanceId" to ambulanceId),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.saveBloodCampAsAdmin(
    camp: BloodDonationCamp,
    adminUid: String,
    adminEmail: String
): Resource<BloodDonationCamp> {
    if (camp.title.trim().length < 2) {
        return Resource.Error("ক্যাম্পের শিরোনাম কমপক্ষে ২ অক্ষরের হতে হবে।")
    }
    if (!FirebaseSecurityHelper.isValidEmergencyOrContactPhone(camp.contactPhone)) {
        return Resource.Error("সঠিক যোগাযোগের ফোন নম্বর দিন।")
    }
    val now = System.currentTimeMillis()
    val docRef = if (camp.id.isNotBlank()) {
        db.collection("blood_camps").document(camp.id)
    } else {
        db.collection("blood_camps").document()
    }
    val saved = camp.copy(
        id = docRef.id,
        district = camp.district.ifBlank { camp.city.ifBlank { "Dhaka" } },
        city = camp.city.ifBlank { camp.district.ifBlank { "Dhaka" } },
        updatedAt = now,
        createdAt = if (camp.createdAt > 0L) camp.createdAt else now
    )
    val auditRef = db.collection("audit_logs").document()
    return try {
        val batch = db.batch()
        batch.set(docRef, saved.toFirestoreMap())
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "BLOOD_CAMP_SAVED",
                "targetId" to saved.id,
                "targetType" to "BLOOD_CAMP",
                "outcome" to "SUCCESS",
                "details" to mapOf("title" to saved.titleBn.ifBlank { saved.title }, "district" to saved.district),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(saved)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

suspend fun FirestoreService.deleteBloodCampAsAdmin(
    campId: String,
    adminUid: String,
    adminEmail: String
): Resource<Unit> {
    if (campId.isBlank()) return Resource.Error("ক্যাম্প আইডি পাওয়া যায়নি।")
    val docRef = db.collection("blood_camps").document(campId)
    val auditRef = db.collection("audit_logs").document()
    val now = System.currentTimeMillis()
    return try {
        val batch = db.batch()
        batch.delete(docRef)
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "BLOOD_CAMP_DELETED",
                "targetId" to campId,
                "targetType" to "BLOOD_CAMP",
                "outcome" to "SUCCESS",
                "details" to mapOf("campId" to campId),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}

/**
 * Default seed data for Blood Banks, Ambulances, and Blood Donation Camps with deterministic IDs
 * so "ডিফল্ট ডেটা আমদানি করুন" in Admin Settings is idempotent and never creates duplicates.
 */
object DefaultDirectorySeedData {
    val defaultBloodBanks = listOf(
        BloodBank(
            id = "bb-1",
            name = "Quantum Lab & Blood Bank",
            nameBn = "কোয়ান্টাম ল্যাব ও ব্লাড ব্যাংক",
            organization = "Quantum Foundation",
            address = "31/V Shilpacharya Zainul Abedin Sarak, Shantinagar",
            city = "Dhaka",
            district = "Dhaka",
            phone = "+8801714010869",
            emergencyHotline = "+88029351969",
            is24Hours = true,
            availableGroups = BloodGroup.entries,
            verified = true
        ),
        BloodBank(
            id = "bb-2",
            name = "Bangladesh Red Crescent Blood Center",
            nameBn = "বাংলাদেশ রেড ক্রিসেন্ট ব্লাড সেন্টার",
            organization = "Red Crescent Society",
            address = "7/5 Aurangzeb Road, Mohammadpur",
            city = "Dhaka",
            district = "Dhaka",
            phone = "+88029116563",
            emergencyHotline = "+8801811458537",
            is24Hours = true,
            availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE, BloodGroup.O_NEGATIVE),
            verified = true
        ),
        BloodBank(
            id = "bb-3",
            name = "Sandhani Central Blood Transfusion",
            nameBn = "সন্ধানী কেন্দ্রীয় রক্ত পরিসঞ্চালন কেন্দ্র",
            organization = "Sandhani (DMCH Unit)",
            address = "Dhaka Medical College Hospital Compound, Shahbagh",
            city = "Dhaka",
            district = "Dhaka",
            phone = "+88029668690",
            emergencyHotline = "+8801711828294",
            is24Hours = true,
            availableGroups = BloodGroup.entries,
            verified = true
        ),
        BloodBank(
            id = "bb-4",
            name = "Badhan Blood Transfusion Center",
            nameBn = "বাঁধন রক্তদান কেন্দ্র (টিএসসি, ঢাবি)",
            organization = "Badhan (Voluntary Blood Donors Organization)",
            address = "Ground Floor, TSC, University of Dhaka",
            city = "Dhaka",
            district = "Dhaka",
            phone = "+8801534982674",
            emergencyHotline = "+88029661900",
            is24Hours = false,
            availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE),
            verified = true
        ),
        BloodBank(
            id = "bb-5",
            name = "Chattogram Medical College Sandhani Blood Bank",
            nameBn = "চট্টগ্রাম মেডিকেল কলেজ সন্ধানী ব্লাড ব্যাংক",
            organization = "Sandhani CMC Unit",
            address = "K.B. Fazlul Kader Road, Panchlaish",
            city = "Chattogram",
            district = "Chattogram",
            phone = "+88031619400",
            emergencyHotline = "+8801819345678",
            is24Hours = true,
            availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.B_NEGATIVE),
            verified = true
        ),
        BloodBank(
            id = "bb-6",
            name = "Rajshahi Medical College Blood Transfusion Department",
            nameBn = "রাজশাহী মেডিকেল কলেজ রক্ত পরিসঞ্চালন বিভাগ",
            organization = "RMCH Government Blood Bank",
            address = "Laxmipur, Rajpara",
            city = "Rajshahi",
            district = "Rajshahi",
            phone = "+880721772150",
            emergencyHotline = "+8801715009911",
            is24Hours = true,
            availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE),
            verified = true
        ),
        BloodBank(
            id = "bb-7",
            name = "Sylhet MAG Osmani Medical Blood Center",
            nameBn = "সিলেট এমএজি ওসমানী মেডিকেল ব্লাড সেন্টার",
            organization = "SOMCH & Mujib Jahan Red Crescent",
            address = "Medical College Road, Chowhatta",
            city = "Sylhet",
            district = "Sylhet",
            phone = "+880821713667",
            emergencyHotline = "+8801711334455",
            is24Hours = true,
            availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE),
            verified = true
        )
    )

    val defaultAmbulances = listOf(
        AmbulanceService(
            id = "amb-1",
            title = "National Emergency Service 999",
            titleBn = "জাতীয় জরুরি সেবা ৯৯৯",
            provider = "Government of Bangladesh",
            coverageArea = "All Bangladesh (সারাদেশ)",
            district = "Dhaka",
            city = "Dhaka",
            phone = "999",
            type = "Emergency Dispatch",
            isAvailable24x7 = true
        ),
        AmbulanceService(
            id = "amb-2",
            title = "Fire Service & Civil Defence Ambulance",
            titleBn = "ফায়ার সার্ভিস ও সিভিল ডিফেন্স অ্যাম্বুলেন্স",
            provider = "FSCD Rescue Division",
            coverageArea = "Nationwide (সকল বিভাগ)",
            district = "Dhaka",
            city = "Dhaka",
            phone = "16163",
            type = "Emergency Life Support",
            isAvailable24x7 = true
        ),
        AmbulanceService(
            id = "amb-3",
            title = "Anjuman Mufidul Islam Free Ambulance",
            titleBn = "আঞ্জুমান মুফিদুল ইসলাম ফ্রি অ্যাম্বুলেন্স",
            provider = "Anjuman Mufidul Islam",
            coverageArea = "Dhaka & Major Cities",
            district = "Dhaka",
            city = "Dhaka",
            phone = "+88029336611",
            type = "Free Humanitarian",
            isAvailable24x7 = true
        ),
        AmbulanceService(
            id = "amb-4",
            title = "Red Crescent Emergency Cardiac & ICU Ambulance",
            titleBn = "রেড ক্রিসেন্ট আইসিইউ অ্যাম্বুলেন্স",
            provider = "Bangladesh Red Crescent",
            coverageArea = "Dhaka & Chattogram",
            district = "Dhaka",
            city = "Dhaka",
            phone = "+88029358799",
            type = "ICU & Cardiac",
            isAvailable24x7 = true
        ),
        AmbulanceService(
            id = "amb-5",
            title = "Shasthyo Batayon Health Helpline",
            titleBn = "স্বাস্থ্য বাতায়ন ১৬২৬৩",
            provider = "DGHS Ministry of Health",
            coverageArea = "All Bangladesh (২৪/৭ ডাক্তার পরামর্শ)",
            district = "Dhaka",
            city = "Dhaka",
            phone = "16263",
            type = "24/7 Medical Tele-Help",
            isAvailable24x7 = true
        )
    )

    val defaultCamps = listOf(
        BloodDonationCamp(
            id = "camp-1",
            title = "DU TSC Voluntary Blood Drive & Grouping Camp",
            titleBn = "ঢাকা বিশ্ববিদ্যালয় টিএসসি স্বেচ্ছায় রক্তদান ও গ্রুপিং ক্যাম্প",
            organizer = "বাঁধন (Badhan - DU Zone)",
            location = "TSC Courtyard, University of Dhaka",
            district = "Dhaka",
            city = "Dhaka",
            date = "25 May 2026",
            time = "9:00 AM - 4:00 PM",
            contactPhone = "+8801534982674",
            targetUnits = 150,
            registeredCount = 68,
            isUserRsvp = false,
            description = "বিনামূল্যে রক্তের গ্রুপ নির্ণয়, হিমোগ্লোবিন পরীক্ষা এবং থ্যালাসেমিয়া রোগীদের জন্য রক্ত সংগ্রহ কর্মসূচি।"
        ),
        BloodDonationCamp(
            id = "camp-2",
            title = "Thalassemia Aid & Safe Blood Awareness Camp",
            titleBn = "থ্যালাসেমিয়া সহায়তা ও নিরাপদ রক্তদান ক্যাম্প",
            organizer = "কোয়ান্টাম ফাউন্ডেশন ও সন্ধানী",
            location = "Shantinagar Quantum Auditorium, Dhaka",
            district = "Dhaka",
            city = "Dhaka",
            date = "02 June 2026",
            time = "10:00 AM - 5:00 PM",
            contactPhone = "+8801714010869",
            targetUnits = 200,
            registeredCount = 114,
            isUserRsvp = false,
            description = "থ্যালাসেমিয়া আক্রান্ত শিশুদের নিয়মিত রক্তের চাহিদা পূরণে বিশেষ রক্তদান ক্যাম্প।"
        ),
        BloodDonationCamp(
            id = "camp-3",
            title = "Chattogram Youth Red Crescent Blood Camp",
            titleBn = "চট্টগ্রাম যুব রেড ক্রিসেন্ট রক্তদান কর্মসূচি",
            organizer = "Red Crescent Youth Chattogram",
            location = "GEC Convention Hall Gate, Chattogram",
            district = "Chattogram",
            city = "Chattogram",
            date = "14 June 2026",
            time = "9:30 AM - 3:30 PM",
            contactPhone = "+8801819345678",
            targetUnits = 120,
            registeredCount = 52,
            isUserRsvp = false,
            description = "বিশ্ব রক্তদাতা দিবস উপলক্ষে বিশেষ সম্মাননা ও স্বেচ্ছায় রক্তদান উৎসব।"
        )
    )
}

/**
 * One-time idempotent import of default Blood Banks, Ambulances, and Blood Donation Camps
 * into Firestore using fixed IDs (`bb-1`..`bb-7`, `amb-1`..`amb-5`, `camp-1`..`camp-3`).
 */
suspend fun FirestoreService.importDefaultDirectoryDataAsAdmin(
    adminUid: String,
    adminEmail: String
): Resource<Int> {
    val now = System.currentTimeMillis()
    return try {
        val batch = db.batch()
        var totalImported = 0

        DefaultDirectorySeedData.defaultBloodBanks.forEach { bank ->
            val ref = db.collection("blood_banks").document(bank.id)
            batch.set(ref, bank.copy(createdAt = now, updatedAt = now).toFirestoreMap())
            totalImported++
        }

        DefaultDirectorySeedData.defaultAmbulances.forEach { amb ->
            val ref = db.collection("ambulances").document(amb.id)
            batch.set(ref, amb.copy(createdAt = now, updatedAt = now).toFirestoreMap())
            totalImported++
        }

        DefaultDirectorySeedData.defaultCamps.forEach { camp ->
            val ref = db.collection("blood_camps").document(camp.id)
            batch.set(ref, camp.copy(createdAt = now, updatedAt = now).toFirestoreMap())
            totalImported++
        }

        val auditRef = db.collection("audit_logs").document()
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to adminEmail,
                "action" to "DEFAULT_DIRECTORY_DATA_IMPORTED",
                "targetId" to "blood_banks_ambulances_camps",
                "targetType" to "SYSTEM_SEED",
                "outcome" to "SUCCESS",
                "details" to mapOf("importedCount" to totalImported.toString()),
                "timestamp" to now
            )
        )
        batch.commit().await()
        Resource.Success(totalImported)
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "blood_banks")
        Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
    }
}
