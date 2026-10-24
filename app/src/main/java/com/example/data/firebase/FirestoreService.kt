package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.domain.BloodCompatibilityHelper
import com.example.domain.LocationHelper
import com.example.model.AccountStatus
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.Campaign
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.LocationInfo
import com.example.model.Organization
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VerificationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

data class PaginatedResult<T>(
    val items: List<T>,
    val lastSnapshot: DocumentSnapshot?,
    val hasMore: Boolean
)

/**
 * Standardized structured JSON error logger for Firestore security rule and query diagnostics.
 */
fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

/**
 * Production Firestore Data Layer Service.
 *
 * - Always connects to the provisioned custom database (`R.string.firestore_database_id`).
 * - Supports server-side query filters, `area_lower` prefix search, and cursor pagination (25 per page).
 * - Uses atomic Firestore transactions for blood request acceptance.
 */
class FirestoreService(
    internal val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val DEFAULT_PAGE_SIZE = 25L
    }

    // =========================================================================
    // 1. Emergency Blood Requests (/blood_requests)
    // =========================================================================

    fun observeBloodRequests(
        filterBloodGroup: BloodGroup? = null,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        pageSize: Long = 100L,
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
        var query: Query = db.collection(path)
        if (!districtFilter.isNullOrBlank()) {
            query = query.whereEqualTo("district", districtFilter.trim())
        }
        if (!areaQuery.isNullOrBlank() && areaQuery.trim().length >= 2 && districtFilter.isNullOrBlank()) {
            val cleanArea = areaQuery.trim().lowercase()
            query = query
                .whereGreaterThanOrEqualTo("area_lower", cleanArea)
                .whereLessThanOrEqualTo("area_lower", cleanArea + "\uf8ff")
        }
        query = query.limit(pageSize)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }

            val now = System.currentTimeMillis()
            val list = snapshot?.documents?.mapNotNull { doc ->
                parseBloodRequest(doc)
            }?.filter { req ->
                val isNotExpired = req.status != RequestStatus.EXPIRED &&
                    req.status != RequestStatus.CANCELLED &&
                    (req.expiresAt <= 0L || req.expiresAt > now || req.status != RequestStatus.PENDING)
                val matchesBlood = filterBloodGroup == null || req.bloodGroup == filterBloodGroup
                val matchesLoc = LocationHelper.matchesLocationFilter(
                    itemDivision = req.division,
                    itemDistrict = req.district.ifBlank { req.city },
                    itemUpazila = req.upazila,
                    itemArea = "${req.area} ${req.hospitalName} ${req.hospitalAddress}",
                    divisionFilter = divisionFilter,
                    districtFilter = districtFilter,
                    upazilaFilter = upazilaFilter,
                    areaQuery = areaQuery
                )
                isNotExpired && matchesBlood && matchesLoc
            }?.sortedWith(
                compareByDescending<BloodRequest> { it.urgency.priorityScore }
                    .thenByDescending { it.createdAt }
            ) ?: emptyList()

            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Fetches a paginated page (default 25 items) of blood requests using server-side filters
     * and `startAfter(lastDocument)` cursor pagination.
     */
    suspend fun fetchBloodRequestsPage(
        filterBloodGroup: BloodGroup? = null,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        lastSnapshot: DocumentSnapshot? = null,
        pageSize: Long = DEFAULT_PAGE_SIZE
    ): Resource<PaginatedResult<BloodRequest>> {
        val path = "blood_requests"
        return try {
            var query: Query = db.collection(path)
            if (filterBloodGroup != null) {
                query = query.whereEqualTo("bloodGroup", filterBloodGroup.name)
            }
            if (!divisionFilter.isNullOrBlank()) {
                query = query.whereEqualTo("division", divisionFilter.trim())
            }
            if (!districtFilter.isNullOrBlank()) {
                query = query.whereEqualTo("district", districtFilter.trim())
            }
            if (!upazilaFilter.isNullOrBlank()) {
                query = query.whereEqualTo("upazila", upazilaFilter.trim())
            }
            if (!areaQuery.isNullOrBlank()) {
                val q = areaQuery.trim().lowercase()
                query = query
                    .whereGreaterThanOrEqualTo("area_lower", q)
                    .whereLessThanOrEqualTo("area_lower", q + "\uf8ff")
            }
            if (lastSnapshot != null) {
                query = query.startAfter(lastSnapshot)
            }
            val snap = query.limit(pageSize).get().await()
            val docs = snap.documents
            val items = docs.mapNotNull { parseBloodRequest(it) }
            Resource.Success(
                PaginatedResult(
                    items = items,
                    lastSnapshot = docs.lastOrNull(),
                    hasMore = docs.size.toLong() >= pageSize
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    suspend fun createBloodRequest(request: BloodRequest): Resource<String> {
        val currentUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return Resource.Error("রক্তের অনুরোধ জানাতে অনুগ্রহ করে লগইন করুন।")

        if (request.patientName.trim().length < 2) {
            return Resource.Error("রোগীর নাম কমপক্ষে ২ অক্ষরের হতে হবে।")
        }
        if (request.hospitalName.trim().length < 2) {
            return Resource.Error("হাসপাতাল বা ক্লিনিকের নাম আবশ্যক।")
        }
        if (!FirebaseSecurityHelper.isValidPhoneNumber(request.contactPhone)) {
            return Resource.Error("সঠিক বাংলাদেশী মোবাইল নম্বর দিন (যেমন: 017XXXXXXXX)।")
        }
        if (request.unitsRequired !in 1..15) {
            return Resource.Error("প্রয়োজনীয় রক্তের ব্যাগ ১ থেকে ১৫ এর মধ্যে হতে হবে।")
        }

        val docRef = if (request.id.isNotBlank()) {
            db.collection("blood_requests").document(request.id)
        } else {
            db.collection("blood_requests").document()
        }

        val now = System.currentTimeMillis()
        val effectiveDistrict = request.district.trim().ifBlank { request.city.trim().ifBlank { "Dhaka" } }
        val effectiveDivision = request.division.trim().ifBlank {
            LocationHelper.inferDivisionForDistrict(effectiveDistrict)
        }

        val normalizedRequest = request.copy(
            id = docRef.id,
            requesterUid = currentUid,
            status = RequestStatus.PENDING,
            division = effectiveDivision,
            district = effectiveDistrict,
            city = effectiveDistrict,
            contactPhone = FirebaseSecurityHelper.normalizePhoneNumber(request.contactPhone),
            acceptedDonorUid = null,
            acceptedDonorName = null,
            createdAt = now,
            updatedAt = now,
            expiresAt = now + 72 * 3600 * 1000L
        )

        return try {
            docRef.set(normalizedRequest.toFirestoreMap()).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    /**
     * Atomically commits a donor to an open/pending blood request using a single client-side
     * Firestore Transaction aligned with `/firestore.rules`.
     */
    suspend fun acceptBloodRequest(
        requestId: String,
        donorUid: String,
        donorName: String,
        donorBloodGroup: BloodGroup? = null
    ): Resource<Unit> {
        val docRef = db.collection("blood_requests").document(requestId)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw FirebaseFirestoreException(
                        "রক্তের অনুরোধটি আর বিদ্যমান নেই।",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val currentStatus = snapshot.getString("status") ?: "OPEN"
                val requesterUid = snapshot.getString("requesterUid")
                    ?: snapshot.getString("requesterId")
                    ?: ""
                val requestedBloodStr = snapshot.getString("bloodGroup") ?: "O_POSITIVE"
                val requestedBlood = BloodGroup.fromDisplay(requestedBloodStr)

                if (requesterUid == donorUid) {
                    throw IllegalStateException("আপনি নিজের রক্তের অনুরোধ নিজে গ্রহণ করতে পারবেন না।")
                }

                if (currentStatus !in listOf(RequestStatus.PENDING.name, "OPEN", RequestStatus.DONOR_RESPONDED.name)) {
                    val alreadyAssigned = snapshot.getString("acceptedDonorUid")
                    if (alreadyAssigned == donorUid) {
                        return@runTransaction
                    }
                    throw IllegalStateException("এই অনুরোধটিতে ইতিমধ্যে অন্য রক্তদাতা সাড়া দিয়েছেন বা এটি সম্পন্ন হয়েছে।")
                }

                if (donorBloodGroup != null && !BloodCompatibilityHelper.isCompatible(donorBloodGroup, requestedBlood)) {
                    throw IllegalStateException(
                        "আপনার রক্তের গ্রুপ (${donorBloodGroup.display}) রোগীর রক্তের গ্রুপের (${requestedBlood.display}) সাথে সামঞ্জস্যপূর্ণ নয়।"
                    )
                }

                val existingAccepted = (snapshot.get("respondedDonorUids") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: (snapshot.get("acceptedDonorIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                    ?: emptyList()
                if (existingAccepted.contains(donorUid)) {
                    return@runTransaction
                }

                val updatedAccepted = existingAccepted + donorUid
                val currentPledged = (snapshot.getLong("unitsFulfilled") ?: snapshot.getLong("bagsPledged") ?: 0L).toInt()
                val unitsRequired = (snapshot.getLong("unitsRequired") ?: snapshot.getLong("bagsNeeded") ?: 1L).toInt()
                val newPledged = currentPledged + 1
                val newStatus = if (newPledged >= unitsRequired) RequestStatus.ACCEPTED.name else RequestStatus.DONOR_RESPONDED.name

                transaction.update(
                    docRef,
                    mapOf(
                        "status" to newStatus,
                        "acceptedDonorUid" to donorUid,
                        "acceptedDonorName" to donorName.trim().ifBlank { "স্বেচ্ছাসেবী রক্তদাতা" },
                        "respondedDonorUids" to updatedAccepted,
                        "unitsFulfilled" to newPledged,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Resource.Error(e.localizedMessage ?: "রক্তের অনুরোধ গ্রহণ করা যায়নি।", e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Resource<Unit> {
        val docRef = db.collection("blood_requests").document(requestId)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw FirebaseFirestoreException(
                        "রক্তের অনুরোধটি পাওয়া যায়নি।",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val currentStatusStr = snapshot.getString("status") ?: "PENDING"
                val currentStatus = if (currentStatusStr == "OPEN") {
                    RequestStatus.PENDING
                } else {
                    runCatching { RequestStatus.valueOf(currentStatusStr) }
                        .getOrDefault(RequestStatus.PENDING)
                }

                if (currentStatus in listOf(RequestStatus.FULFILLED, RequestStatus.CANCELLED, RequestStatus.EXPIRED)) {
                    throw IllegalStateException("ইতিমধ্যে সম্পন্ন বা বাতিল হওয়া অনুরোধ পরিবর্তন করা যাবে না।")
                }

                if (newStatus == RequestStatus.FULFILLED) {
                    val acceptedDonor = snapshot.getString("acceptedDonorUid")
                    val responders = (snapshot.get("respondedDonorUids") as? List<*>)
                        ?: (snapshot.get("acceptedDonorIds") as? List<*>)
                        ?: emptyList<Any>()
                    if (acceptedDonor.isNullOrBlank() && responders.isEmpty()) {
                        throw IllegalStateException("রক্তদাতা সাড়া দেওয়ার পরই কেবল অনুরোধটি সম্পন্ন হিসেবে চিহ্নিত করা যাবে।")
                    }
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "status" to newStatus.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Resource.Error(e.localizedMessage ?: "স্ট্যাটাস হালনাগাদ করা যায়নি।", e)
        }
    }

    internal fun parseBloodRequest(doc: DocumentSnapshot): BloodRequest? {
        return try {
            val district = doc.getString("district") ?: doc.getString("city") ?: "Dhaka"
            val division = doc.getString("division") ?: LocationHelper.inferDivisionForDistrict(district)
            val rawStatus = doc.getString("status") ?: "PENDING"
            val parsedStatus = when (rawStatus.uppercase()) {
                "OPEN", "PENDING" -> {
                    val acceptedUid = doc.getString("acceptedDonorUid")
                    if (!acceptedUid.isNullOrBlank()) RequestStatus.ACCEPTED else RequestStatus.PENDING
                }
                else -> runCatching { RequestStatus.valueOf(rawStatus) }.getOrDefault(RequestStatus.PENDING)
            }
            BloodRequest(
                id = doc.id,
                requesterUid = doc.getString("requesterUid") ?: doc.getString("requesterId") ?: "",
                requesterName = doc.getString("requesterName") ?: "",
                patientName = doc.getString("patientName") ?: "",
                patientAge = (doc.getLong("patientAge") ?: 0L).toInt(),
                bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE"),
                unitsRequired = (doc.getLong("unitsRequired") ?: doc.getLong("bagsNeeded") ?: 1L).toInt(),
                unitsFulfilled = (doc.getLong("unitsFulfilled") ?: doc.getLong("bagsPledged") ?: 0L).toInt(),
                hospitalName = doc.getString("hospitalName") ?: "",
                hospitalAddress = doc.getString("hospitalAddress") ?: "",
                division = division,
                district = district,
                upazila = doc.getString("upazila") ?: "",
                area = doc.getString("area") ?: "",
                city = doc.getString("city") ?: district,
                urgency = runCatching {
                    RequestUrgency.valueOf(doc.getString("urgency") ?: "HIGH")
                }.getOrDefault(RequestUrgency.HIGH),
                status = parsedStatus,
                contactPhone = doc.getString("contactPhone") ?: "",
                medicalReason = doc.getString("medicalReason") ?: "",
                requiredDateTime = doc.getLong("requiredDateTime") ?: System.currentTimeMillis(),
                acceptedDonorUid = doc.getString("acceptedDonorUid"),
                acceptedDonorName = doc.getString("acceptedDonorName"),
                respondedDonorUids = (doc.get("respondedDonorUids") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: (doc.get("acceptedDonorIds") as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList(),
                isEmergencyBroadcast = doc.getBoolean("isEmergencyBroadcast") ?: false,
                broadcastDispatchedAt = doc.getLong("broadcastDispatchedAt"),
                broadcastNotifiedCount = (doc.getLong("broadcastNotifiedCount") ?: 0L).toInt(),
                adminNotes = doc.getString("adminNotes") ?: "",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                expiresAt = doc.getLong("expiresAt") ?: (System.currentTimeMillis() + 72 * 3600 * 1000L)
            )
        } catch (e: Exception) {
            Log.w("FirestoreService", "Skipping malformed blood request ${doc.id}: ${e.message}")
            null
        }
    }

    // =========================================================================
    // 2. Real Public Donor Discovery (/public_donors) with Server-Side Filters
    // =========================================================================

    fun observePublicDonors(
        filterBloodGroup: BloodGroup? = null,
        includeCompatibleForRecipient: Boolean = false,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        onlyAvailable: Boolean = false,
        pageSize: Long = 100L,
        onError: ((String) -> Unit)? = null
    ): Flow<List<PublicDonorView>> = callbackFlow {
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

        val path = "public_donors"
        var query: Query = db.collection(path)
            .whereEqualTo("isDonor", true)

        if (filterBloodGroup != null && !includeCompatibleForRecipient) {
            query = query.whereEqualTo("bloodGroup", filterBloodGroup.name)
        }
        if (!districtFilter.isNullOrBlank()) {
            query = query.whereEqualTo("district", districtFilter.trim())
        }
        query = query.limit(pageSize)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                trySend(emptyList())
                return@addSnapshotListener
            }

            val allowedGroups: Set<BloodGroup>? = when {
                filterBloodGroup == null -> null
                includeCompatibleForRecipient -> BloodCompatibilityHelper.getCompatibleDonorGroups(filterBloodGroup).toSet()
                else -> setOf(filterBloodGroup)
            }

            val donors = snapshot?.documents?.mapNotNull { doc ->
                parsePublicDonorDoc(doc, allowedGroups, divisionFilter, districtFilter, upazilaFilter, areaQuery, onlyAvailable)
            }?.sortedWith(
                compareByDescending<PublicDonorView> {
                    filterBloodGroup != null && it.bloodGroup == filterBloodGroup
                }.thenByDescending {
                    it.availability == DonorAvailability.AVAILABLE
                }.thenByDescending {
                    it.verificationStatus == VerificationStatus.VERIFIED
                }.thenByDescending {
                    it.donationCount
                }
            ) ?: emptyList()

            trySend(donors)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Fetches a paginated page (default 25 items) of public donors using server-side filters,
     * `area_lower` prefix search, and `startAfter(lastDocument)` cursor pagination.
     */
    suspend fun fetchPublicDonorsPage(
        filterBloodGroup: BloodGroup? = null,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        onlyAvailable: Boolean = false,
        lastSnapshot: DocumentSnapshot? = null,
        pageSize: Long = DEFAULT_PAGE_SIZE
    ): Resource<PaginatedResult<PublicDonorView>> {
        val path = "public_donors"
        return try {
            var query: Query = db.collection(path).whereEqualTo("isDonor", true)
            if (filterBloodGroup != null) {
                query = query.whereEqualTo("bloodGroup", filterBloodGroup.name)
            }
            if (!divisionFilter.isNullOrBlank()) {
                query = query.whereEqualTo("division", divisionFilter.trim())
            }
            if (!districtFilter.isNullOrBlank()) {
                query = query.whereEqualTo("district", districtFilter.trim())
            }
            if (!upazilaFilter.isNullOrBlank()) {
                query = query.whereEqualTo("upazila", upazilaFilter.trim())
            }
            if (onlyAvailable) {
                query = query.whereEqualTo("availability", DonorAvailability.AVAILABLE.name)
            }
            if (!areaQuery.isNullOrBlank()) {
                val q = areaQuery.trim().lowercase()
                query = query
                    .whereGreaterThanOrEqualTo("area_lower", q)
                    .whereLessThanOrEqualTo("area_lower", q + "\uf8ff")
            }
            if (lastSnapshot != null) {
                query = query.startAfter(lastSnapshot)
            }
            val snap = query.limit(pageSize).get().await()
            val docs = snap.documents
            val items = docs.mapNotNull {
                parsePublicDonorDoc(
                    doc = it,
                    allowedGroups = filterBloodGroup?.let { bg -> setOf(bg) },
                    divisionFilter = divisionFilter,
                    districtFilter = districtFilter,
                    upazilaFilter = upazilaFilter,
                    areaQuery = areaQuery,
                    onlyAvailable = onlyAvailable
                )
            }
            Resource.Success(
                PaginatedResult(
                    items = items,
                    lastSnapshot = docs.lastOrNull(),
                    hasMore = docs.size.toLong() >= pageSize
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    internal fun parsePublicDonorDoc(
        doc: DocumentSnapshot,
        allowedGroups: Set<BloodGroup>? = null,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        onlyAvailable: Boolean = false
    ): PublicDonorView? {
        return try {
            val bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE")
            if (allowedGroups != null && !allowedGroups.contains(bloodGroup)) {
                return null
            }

            val division = doc.getString("division") ?: "Dhaka"
            val district = doc.getString("district") ?: "Dhaka"
            val upazila = doc.getString("upazila") ?: ""
            val area = doc.getString("area") ?: ""
            val generalLocation = doc.getString("generalLocation").orEmpty().ifBlank {
                LocationHelper.formatGeneralLocation(division, district, upazila, area)
            }

            if (!LocationHelper.matchesLocationFilter(
                    itemDivision = division,
                    itemDistrict = district,
                    itemUpazila = upazila,
                    itemArea = "$area $generalLocation",
                    divisionFilter = divisionFilter,
                    districtFilter = districtFilter,
                    upazilaFilter = upazilaFilter,
                    areaQuery = areaQuery
                )
            ) {
                return null
            }

            val storedAvail = runCatching {
                DonorAvailability.valueOf(
                    doc.getString("availability") ?: doc.getString("donorAvailability") ?: "AVAILABLE"
                )
            }.getOrDefault(DonorAvailability.AVAILABLE)

            val lastDonationDate = doc.getLong("lastDonationDate")
            val daysSinceLast = if (lastDonationDate != null && lastDonationDate > 0L) {
                (System.currentTimeMillis() - lastDonationDate) / (1000L * 60 * 60 * 24)
            } else {
                999L
            }
            val effectiveAvail = if (daysSinceLast < 90L && storedAvail == DonorAvailability.AVAILABLE) {
                DonorAvailability.RESTING
            } else {
                storedAvail
            }

            if (onlyAvailable && effectiveAvail != DonorAvailability.AVAILABLE) {
                return null
            }

            PublicDonorView(
                uid = doc.getString("uid") ?: doc.id,
                name = doc.getString("name") ?: "Voluntary Donor",
                bloodGroup = bloodGroup,
                availability = effectiveAvail,
                division = division,
                district = district,
                upazila = upazila,
                area = area,
                generalLocation = generalLocation,
                isEmergencyDonor = doc.getBoolean("isEmergencyDonor") ?: false,
                contactPhone = doc.getString("contactPhone") ?: "",
                verificationStatus = runCatching {
                    VerificationStatus.valueOf(doc.getString("verificationStatus") ?: "UNVERIFIED")
                }.getOrDefault(VerificationStatus.UNVERIFIED),
                donationCount = (doc.getLong("donationCount") ?: 0L).toInt(),
                lastDonationDate = lastDonationDate
            )
        } catch (e: Exception) {
            Log.w("FirestoreService", "Skipping malformed public donor ${doc.id}: ${e.message}")
            null
        }
    }

    // =========================================================================
    // 3. 1-to-1 Conversations & Messages (/conversations)
    // =========================================================================

    fun observeConversations(
        userUid: String,
        onError: ((String) -> Unit)? = null
    ): Flow<List<Conversation>> = callbackFlow {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null || userUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "conversations"
        val listener = db.collection(path)
            .whereArrayContains("participantIds", userUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val participants = (doc.get("participantIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: (doc.get("participants") as? List<*>)?.mapNotNull { it as? String }
                        ?: emptyList()
                    val names = (doc.get("participantNames") as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            if (k is String && v is String) k to v else null
                        }?.toMap() ?: emptyMap()
                    val groups = (doc.get("participantBloodGroups") as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            if (k is String && v is String) k to v else null
                        }?.toMap() ?: emptyMap()

                    Conversation(
                        id = doc.id,
                        participants = participants,
                        participantNames = names,
                        participantBloodGroups = groups,
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: doc.getLong("lastMessageTime") ?: 0L,
                        requestId = doc.getString("requestId")
                    )
                }?.sortedByDescending { it.lastMessageTimestamp } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun observeMessages(
        conversationId: String,
        onError: ((String) -> Unit)? = null
    ): Flow<List<ChatMessage>> = callbackFlow {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null || conversationId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "conversations/$conversationId/messages"
        val listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    ChatMessage(
                        id = doc.id,
                        conversationId = conversationId,
                        senderUid = doc.getString("senderUid") ?: doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                } ?: emptyList()
                trySend(msgs)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateConversation(
        myUid: String,
        myName: String,
        myBloodGroup: String,
        otherUid: String,
        otherName: String,
        otherBloodGroup: String,
        requestId: String? = null
    ): Resource<String> {
        if (myUid.isBlank() || otherUid.isBlank() || myUid == otherUid) {
            return Resource.Error("নিজের সাথে চ্যাট শুরু করা যাবে না।")
        }
        val sortedIds = listOf(myUid, otherUid).sorted()
        val convId = "${sortedIds[0]}_${sortedIds[1]}"
        val docRef = db.collection("conversations").document(convId)

        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val now = System.currentTimeMillis()
                val payload = mapOf<String, Any?>(
                    "participants" to sortedIds,
                    "participantIds" to sortedIds,
                    "participantNames" to mapOf(myUid to myName, otherUid to otherName),
                    "participantBloodGroups" to mapOf(myUid to myBloodGroup, otherUid to otherBloodGroup),
                    "lastMessage" to "কথোপকথন শুরু হয়েছে",
                    "lastMessageTimestamp" to now,
                    "lastMessageTime" to now,
                    "requestId" to requestId
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Resource.Success(convId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderUid: String,
        senderName: String,
        text: String
    ): Resource<Unit> {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return Resource.Error("বার্তা খালি রাখা যাবে না।")
        }
        if (cleanText.length > 500) {
            return Resource.Error("বার্তা সর্বোচ্চ ৫০০ অক্ষরের হতে পারবে।")
        }

        val convRef = db.collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document()
        val now = System.currentTimeMillis()

        return try {
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "conversationId" to conversationId,
                    "senderUid" to senderUid,
                    "senderId" to senderUid,
                    "senderName" to senderName.trim().ifBlank { "ব্যবহারকারী" },
                    "text" to cleanText,
                    "timestamp" to now
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to cleanText,
                    "lastMessageTimestamp" to now,
                    "lastMessageTime" to now
                )
            )
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    // =========================================================================
    // 4. Donation History & Organizations
    // =========================================================================

    fun observeUserDonations(
        donorUid: String,
        onError: ((String) -> Unit)? = null
    ): Flow<List<DonationRecord>> = callbackFlow {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null || donorUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "donation_history"
        val listener = db.collection(path)
            .whereEqualTo("donorId", donorUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    DonationRecord(
                        id = doc.id,
                        donorUid = doc.getString("donorId") ?: doc.getString("donorUid") ?: "",
                        donorName = doc.getString("donorName") ?: "",
                        bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE"),
                        requestId = doc.getString("requestId"),
                        hospitalName = doc.getString("hospitalName") ?: "",
                        city = doc.getString("city") ?: "",
                        donationDate = doc.getLong("donationDate") ?: 0L,
                        verifiedByOrgId = doc.getString("verifiedByOrgId"),
                        verifiedByOrgName = doc.getString("verifiedByOrgName") ?: "অনুমোদিত হাসপাতাল / প্রতিষ্ঠান",
                        unitsDonated = (doc.getLong("bags") ?: doc.getLong("unitsDonated") ?: 1L).toInt(),
                        certificateCode = doc.getString("certificateCode") ?: ""
                    )
                }?.sortedByDescending { it.donationDate } ?: emptyList()
                trySend(records)
            }

        awaitClose { listener.remove() }
    }

    fun observeOrganizations(
        onError: ((String) -> Unit)? = null
    ): Flow<List<Organization>> = callbackFlow {
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

        val path = "organizations"
        val listener = db.collection(path)
            .whereEqualTo("isVerified", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orgs = snapshot?.documents?.mapNotNull { doc ->
                    Organization(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        registrationNumber = doc.getString("registrationNumber") ?: "",
                        description = doc.getString("description") ?: "",
                        headquartersCity = doc.getString("headquartersCity") ?: "",
                        division = doc.getString("division") ?: "Dhaka",
                        district = doc.getString("district") ?: "Dhaka",
                        contactEmail = doc.getString("contactEmail") ?: "",
                        contactPhone = doc.getString("contactPhone") ?: "",
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        adminUids = (doc.get("adminUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        memberCount = (doc.getLong("memberCount") ?: 0L).toInt(),
                        totalBagsCollected = (doc.getLong("totalBagsCollected") ?: 0L).toInt(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(orgs)
            }

        awaitClose { listener.remove() }
    }

    fun observeCampaigns(
        onError: ((String) -> Unit)? = null
    ): Flow<List<Campaign>> = callbackFlow {
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

        val path = "campaigns"
        val listener = db.collection(path)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val campaigns = snapshot?.documents?.mapNotNull { doc ->
                    Campaign(
                        id = doc.id,
                        organizationId = doc.getString("organizationId") ?: "",
                        organizationName = doc.getString("organizationName") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        city = doc.getString("city") ?: "",
                        startDate = doc.getLong("startDate") ?: 0L,
                        endDate = doc.getLong("endDate") ?: 0L,
                        targetBags = (doc.getLong("targetBags") ?: 50L).toInt(),
                        collectedBags = (doc.getLong("collectedBags") ?: 0L).toInt(),
                        contactPhone = doc.getString("contactPhone") ?: "",
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                } ?: emptyList()
                trySend(campaigns)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createCampaign(campaign: Campaign): Resource<String> {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        } ?: return Resource.Error("ক্যাম্পেইন প্রকাশ করতে অনুগ্রহ করে লগইন করুন।")

        val docRef = if (campaign.id.isNotBlank()) {
            db.collection("campaigns").document(campaign.id)
        } else {
            db.collection("campaigns").document()
        }
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "organizationId" to campaign.organizationId.ifBlank { currentUser.uid },
            "organizationName" to campaign.organizationName.trim().ifBlank { "স্বেচ্ছাসেবী রক্তদান কর্মসূচি" },
            "title" to campaign.title.trim(),
            "description" to campaign.description.trim(),
            "location" to campaign.location.trim(),
            "city" to campaign.city.trim().ifBlank { "Dhaka" },
            "startDate" to if (campaign.startDate > 0L) campaign.startDate else now,
            "endDate" to if (campaign.endDate > 0L) campaign.endDate else (now + 7 * 86400000L),
            "targetBags" to campaign.targetBags.coerceIn(1, 10000),
            "collectedBags" to campaign.collectedBags.coerceAtLeast(0),
            "contactPhone" to campaign.contactPhone.trim(),
            "isActive" to campaign.isActive
        )

        return try {
            docRef.set(payload).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    internal fun parseUserProfileDoc(doc: DocumentSnapshot): UserProfile? {
        return try {
            val uid = doc.getString("uid") ?: doc.id
            val locMap = doc.get("location") as? Map<*, *>
            val district = doc.getString("district")
                ?: (locMap?.get("district") as? String)
                ?: doc.getString("city")
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
            UserProfile(
                uid = uid,
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
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
                verificationStatus = runCatching {
                    VerificationStatus.valueOf(doc.getString("verificationStatus") ?: "UNVERIFIED")
                }.getOrDefault(VerificationStatus.UNVERIFIED),
                role = runCatching {
                    UserRole.valueOf(doc.getString("role") ?: "USER")
                }.getOrDefault(UserRole.USER),
                accountStatus = runCatching {
                    AccountStatus.valueOf(doc.getString("accountStatus") ?: "ACTIVE")
                }.getOrDefault(AccountStatus.ACTIVE),
                suspensionReason = doc.getString("suspensionReason"),
                organizationId = doc.getString("organizationId"),
                donationCount = (doc.getLong("donationCount") ?: 0L).toInt(),
                badges = (doc.get("badges") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                photoUrl = doc.getString("photoUrl") ?: "",
                fcmToken = doc.getString("fcmToken") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                updatedAt = doc.getLong("updatedAt") ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }
}
