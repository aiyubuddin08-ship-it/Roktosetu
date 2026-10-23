package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.R
import com.example.domain.BloodCompatibilityHelper
import com.example.domain.LocationHelper
import com.example.model.AuditLogEntry
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.Campaign
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.Organization
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.SystemMetrics
import com.example.model.VerificationStatus
import com.example.model.VerificationTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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
 * - Never injects hardcoded or simulated sample donors into production searches.
 * - Uses atomic Firestore transactions for blood request acceptance and donation verification.
 * - Logs all Firestore errors through `handleFirestoreError`.
 */
class FirestoreService(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // =========================================================================
    // 1. Emergency Blood Requests (/blood_requests)
    // =========================================================================

    fun observeBloodRequests(
        filterBloodGroup: BloodGroup? = null,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
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
        val query = db.collection(path)
            .whereIn(
                "status",
                listOf(
                    RequestStatus.PENDING.name,
                    RequestStatus.DONOR_RESPONDED.name,
                    RequestStatus.ACCEPTED.name,
                    RequestStatus.FULFILLED.name
                )
            )
            .limit(100)

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

    suspend fun createBloodRequest(request: BloodRequest): Resource<String> {
        val currentUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return Resource.Error("You must be signed in to post a blood request.")

        if (request.patientName.trim().length < 2) {
            return Resource.Error("Patient name must be at least 2 characters.")
        }
        if (request.hospitalName.trim().length < 2) {
            return Resource.Error("Hospital or clinic name is required.")
        }
        if (request.contactPhone.trim().length < 6) {
            return Resource.Error("A valid emergency contact phone number is required.")
        }
        if (request.unitsRequired !in 1..20) {
            return Resource.Error("Required blood units must be between 1 and 20 bags.")
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
     * Atomically commits a donor to a PENDING blood request using a Firestore Transaction.
     * Prevents race conditions, duplicate acceptances, self-acceptance, and incompatible blood types.
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
                        "Blood request no longer exists.",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val currentStatus = snapshot.getString("status") ?: "PENDING"
                val requesterUid = snapshot.getString("requesterUid") ?: ""
                val requestedBloodStr = snapshot.getString("bloodGroup") ?: "O_POSITIVE"
                val requestedBlood = BloodGroup.fromDisplay(requestedBloodStr)

                if (requesterUid == donorUid) {
                    throw IllegalStateException("You cannot accept your own blood request.")
                }

                if (currentStatus != RequestStatus.PENDING.name) {
                    val alreadyAssigned = snapshot.getString("acceptedDonorUid")
                    if (alreadyAssigned == donorUid) {
                        return@runTransaction
                    }
                    throw IllegalStateException("This request has already been responded to or closed (Status: $currentStatus).")
                }

                if (donorBloodGroup != null && !BloodCompatibilityHelper.isCompatible(donorBloodGroup, requestedBlood)) {
                    throw IllegalStateException(
                        "Your blood group (${donorBloodGroup.display}) is not medically compatible with the patient's group (${requestedBlood.display})."
                    )
                }

                val existingResponders = (snapshot.get("respondedDonorUids") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
                val updatedResponders = if (existingResponders.contains(donorUid)) {
                    existingResponders
                } else {
                    (existingResponders + donorUid).take(20)
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "status" to RequestStatus.ACCEPTED.name,
                        "acceptedDonorUid" to donorUid,
                        "acceptedDonorName" to donorName.trim().ifBlank { "Voluntary Donor" },
                        "respondedDonorUids" to updatedResponders,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Resource.Error(e.localizedMessage ?: "Could not accept blood request", e)
        }
    }

    /**
     * Updates the status of a blood request after validating the transition.
     */
    suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Resource<Unit> {
        val docRef = db.collection("blood_requests").document(requestId)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw FirebaseFirestoreException(
                        "Blood request not found.",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val currentStatusStr = snapshot.getString("status") ?: "PENDING"
                val currentStatus = runCatching { RequestStatus.valueOf(currentStatusStr) }
                    .getOrDefault(RequestStatus.PENDING)

                if (currentStatus in listOf(RequestStatus.FULFILLED, RequestStatus.CANCELLED, RequestStatus.EXPIRED)) {
                    throw IllegalStateException("Cannot modify a request that is already ${currentStatus.display}.")
                }

                if (newStatus == RequestStatus.FULFILLED) {
                    val acceptedDonor = snapshot.getString("acceptedDonorUid")
                    if (currentStatus !in listOf(RequestStatus.ACCEPTED, RequestStatus.DONOR_RESPONDED) ||
                        acceptedDonor.isNullOrBlank()
                    ) {
                        throw IllegalStateException("A request can only be marked Fulfilled after a donor has responded.")
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
            Resource.Error(e.localizedMessage ?: "Failed to update request status", e)
        }
    }

    private fun parseBloodRequest(doc: DocumentSnapshot): BloodRequest? {
        return try {
            val district = doc.getString("district") ?: doc.getString("city") ?: "Dhaka"
            val division = doc.getString("division") ?: LocationHelper.inferDivisionForDistrict(district)
            BloodRequest(
                id = doc.id,
                requesterUid = doc.getString("requesterUid") ?: "",
                requesterName = doc.getString("requesterName") ?: "",
                patientName = doc.getString("patientName") ?: "",
                patientAge = (doc.getLong("patientAge") ?: 0L).toInt(),
                bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE"),
                unitsRequired = (doc.getLong("unitsRequired") ?: 1L).toInt(),
                unitsFulfilled = (doc.getLong("unitsFulfilled") ?: 0L).toInt(),
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
                status = runCatching {
                    RequestStatus.valueOf(doc.getString("status") ?: "PENDING")
                }.getOrDefault(RequestStatus.PENDING),
                contactPhone = doc.getString("contactPhone") ?: "",
                medicalReason = doc.getString("medicalReason") ?: "",
                requiredDateTime = doc.getLong("requiredDateTime") ?: System.currentTimeMillis(),
                acceptedDonorUid = doc.getString("acceptedDonorUid"),
                acceptedDonorName = doc.getString("acceptedDonorName"),
                respondedDonorUids = (doc.get("respondedDonorUids") as? List<*>)
                    ?.mapNotNull { it as? String } ?: emptyList(),
                isEmergencyBroadcast = doc.getBoolean("isEmergencyBroadcast") ?: false,
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
    // 2. Real Public Donor Discovery (/public_donors) - Map-Free
    // =========================================================================

    fun observePublicDonors(
        filterBloodGroup: BloodGroup? = null,
        includeCompatibleForRecipient: Boolean = false,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        onlyAvailable: Boolean = false,
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
        // Query aligns strictly with firestore.rules: resource.data.isDonor == true
        val query: Query = db.collection(path)
            .whereEqualTo("isDonor", true)
            .limit(200)

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
                try {
                    val bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE")
                    if (allowedGroups != null && !allowedGroups.contains(bloodGroup)) {
                        return@mapNotNull null
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
                        return@mapNotNull null
                    }

                    val storedAvail = runCatching {
                        DonorAvailability.valueOf(doc.getString("availability") ?: "AVAILABLE")
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
                        return@mapNotNull null
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
            .whereArrayContains("participants", userUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val participants = (doc.get("participants") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()
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
                        lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
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
                        senderUid = doc.getString("senderUid") ?: "",
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
            return Resource.Error("Cannot start a conversation with yourself.")
        }
        val sortedIds = listOf(myUid, otherUid).sorted()
        val convId = "${sortedIds[0]}_${sortedIds[1]}"
        val docRef = db.collection("conversations").document(convId)

        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf<String, Any?>(
                    "participants" to sortedIds,
                    "participantNames" to mapOf(myUid to myName, otherUid to otherName),
                    "participantBloodGroups" to mapOf(myUid to myBloodGroup, otherUid to otherBloodGroup),
                    "lastMessage" to "Conversation started",
                    "lastMessageTimestamp" to System.currentTimeMillis(),
                    "requestId" to requestId
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Resource.Success(convId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Resource.Error(e.localizedMessage ?: "Failed to start conversation", e)
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
            return Resource.Error("Message cannot be empty.")
        }
        if (cleanText.length > 1000) {
            return Resource.Error("Message is too long (maximum 1000 characters).")
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
                    "senderName" to senderName.trim().ifBlank { "User" },
                    "text" to cleanText,
                    "timestamp" to now
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to cleanText,
                    "lastMessageTimestamp" to now
                )
            )
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Resource.Error(e.localizedMessage ?: "Failed to send message", e)
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

        val path = "donations"
        val listener = db.collection(path)
            .whereEqualTo("donorUid", donorUid)
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
                        donorUid = doc.getString("donorUid") ?: "",
                        donorName = doc.getString("donorName") ?: "",
                        bloodGroup = BloodGroup.fromDisplay(doc.getString("bloodGroup") ?: "O_POSITIVE"),
                        requestId = doc.getString("requestId"),
                        hospitalName = doc.getString("hospitalName") ?: "",
                        city = doc.getString("city") ?: "",
                        donationDate = doc.getLong("donationDate") ?: 0L,
                        verifiedByOrgId = doc.getString("verifiedByOrgId"),
                        verifiedByOrgName = doc.getString("verifiedByOrgName") ?: "Verified Hospital / Organization",
                        unitsDonated = (doc.getLong("unitsDonated") ?: 1L).toInt(),
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
        } ?: return Resource.Error("You must be signed in to publish a blood donation camp.")

        val docRef = if (campaign.id.isNotBlank()) {
            db.collection("campaigns").document(campaign.id)
        } else {
            db.collection("campaigns").document()
        }
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "organizationId" to campaign.organizationId.ifBlank { currentUser.uid },
            "organizationName" to campaign.organizationName.trim().ifBlank { "Community Blood Drive" },
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

    // =========================================================================
    // 5. Verification Queue & Admin Operations (RBAC-Protected)
    // =========================================================================

    suspend fun checkIsCurrentUserAdmin(uid: String): Boolean {
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

    suspend fun submitVerificationRequest(
        applicantUid: String,
        name: String,
        type: String,
        documents: List<String>
    ): Resource<String> {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        } ?: return Resource.Error("You must be signed in to submit a verification request.")

        val effectiveUid = applicantUid.ifBlank { currentUser.uid }
        if (effectiveUid != currentUser.uid) {
            return Resource.Error("You can only submit a verification request for your own account.")
        }

        val normalizedType = when (type.trim().uppercase()) {
            "ORGANIZATION" -> "ORGANIZATION"
            else -> "DONOR"
        }
        val docId = "ver_${effectiveUid}_${normalizedType.lowercase()}"
        val now = System.currentTimeMillis()

        val docRef = db.collection("verification_requests").document(docId)
        val payload = mapOf(
            "applicantUid" to effectiveUid,
            "name" to name.trim().ifBlank { currentUser.displayName ?: "Verified Applicant" },
            "type" to normalizedType,
            "documents" to documents.take(10),
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

    fun observePendingVerificationTasks(
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
        val listener = db.collection(path)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError?.invoke(FirebaseSecurityHelper.mapExceptionToMessage(error))
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val appUid = doc.getString("applicantUid") ?: doc.getString("targetId") ?: ""
                    val rawStatus = doc.getString("status") ?: "PENDING"
                    val parsedStatus = when (rawStatus.uppercase()) {
                        "APPROVED", "VERIFIED" -> VerificationStatus.VERIFIED
                        "REJECTED" -> VerificationStatus.REJECTED
                        else -> VerificationStatus.PENDING_REVIEW
                    }
                    VerificationTask(
                        id = doc.id,
                        targetId = appUid,
                        applicantUid = appUid,
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type") ?: "DONOR",
                        documents = (doc.get("documents") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        status = parsedStatus,
                        submittedAt = doc.getLong("submittedAt") ?: 0L
                    )
                }?.sortedByDescending { it.submittedAt } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun observeAuditLogs(
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
            .limit(50)
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
                        details = detailsMap,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                trySend(logs)
            }

        awaitClose { listener.remove() }
    }

    suspend fun reviewVerificationTaskAsAdmin(
        task: VerificationTask,
        approved: Boolean,
        adminUid: String,
        adminEmail: String
    ): Resource<Unit> {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        } ?: return Resource.Error("Administrator authentication is required.")

        val decision = if (approved) "APPROVED" else "REJECTED"
        val now = System.currentTimeMillis()
        val taskRef = db.collection("verification_requests").document(task.id)

        return try {
            db.runTransaction { tx ->
                val snap = tx.get(taskRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Verification task not found.")
                }
                tx.update(
                    taskRef,
                    mapOf(
                        "status" to decision,
                        "reviewedByUid" to adminUid.ifBlank { currentUser.uid },
                        "reviewedAt" to now
                    )
                )

                if (task.type == "ORGANIZATION") {
                    val orgRef = db.collection("organizations").document(task.applicantUid)
                    val orgSnap = tx.get(orgRef)
                    if (orgSnap.exists()) {
                        tx.update(orgRef, mapOf("isVerified" to approved))
                    }
                } else {
                    val verStatus = if (approved) VerificationStatus.VERIFIED.name else VerificationStatus.REJECTED.name
                    val userRef = db.collection("users").document(task.applicantUid)
                    val pubRef = db.collection("public_donors").document(task.applicantUid)
                    val userSnap = tx.get(userRef)
                    val pubSnap = tx.get(pubRef)
                    if (userSnap.exists()) {
                        tx.update(userRef, mapOf("verificationStatus" to verStatus, "updatedAt" to now))
                    }
                    if (pubSnap.exists()) {
                        tx.update(pubRef, mapOf("verificationStatus" to verStatus, "updatedAt" to now))
                    }
                }

                val auditRef = db.collection("audit_logs").document()
                tx.set(
                    auditRef,
                    mapOf(
                        "actorUid" to adminUid.ifBlank { currentUser.uid },
                        "actorEmail" to adminEmail.ifBlank { currentUser.email ?: "admin" },
                        "action" to "VERIFICATION_$decision",
                        "targetId" to task.applicantUid,
                        "targetType" to task.type,
                        "details" to "$decision verification for ${task.name}",
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

    /**
     * Uploads a verification document (PDF, JPEG, PNG, or WEBP <= 10 MB) to Firebase Storage
     * at `/verifications/{userId}/{sanitizedDocumentId}` in strict compliance with `storage.rules`.
     */
    suspend fun uploadVerificationDocument(
        context: Context,
        userId: String,
        uri: Uri
    ): Resource<String> {
        if (userId.isBlank()) {
            return Resource.Error("অনুগ্রহ করে প্রথমে লগইন করুন। (Please sign in first.)")
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
            return Resource.Error(
                "শুধুমাত্র PDF, JPG, PNG বা WEBP ফাইল আপলোড করা যাবে। (Only PDF, JPEG, PNG, or WEBP files are allowed.)"
            )
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
            return Resource.Error(
                "ফাইলের আকার সর্বোচ্চ ১০ মেগাবাইট হতে পারবে। (File size must not exceed 10 MB.)"
            )
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

    /**
     * Computes live system metrics from Firestore collections for the Admin Dashboard.
     */
    suspend fun fetchSystemMetrics(): SystemMetrics {
        return try {
            val donorsSnap = db.collection("public_donors")
                .whereEqualTo("isDonor", true)
                .limit(500)
                .get()
                .await()
            val totalDonors = donorsSnap.size()
            val activeDonors = donorsSnap.documents.count {
                (it.getString("availability") ?: DonorAvailability.AVAILABLE.name) == DonorAvailability.AVAILABLE.name
            }
            val emergencyDonorsOnline = donorsSnap.documents.count {
                (it.getBoolean("isEmergencyDonor") ?: false) &&
                    (it.getString("availability") ?: DonorAvailability.AVAILABLE.name) != DonorAvailability.UNAVAILABLE.name
            }
            val totalDonationsCompleted = donorsSnap.documents.sumOf {
                (it.getLong("donationCount") ?: 0L).toInt()
            }

            val requestsSnap = db.collection("blood_requests")
                .limit(500)
                .get()
                .await()
            val activeStatuses = setOf(
                RequestStatus.PENDING.name,
                RequestStatus.DONOR_RESPONDED.name,
                RequestStatus.ACCEPTED.name
            )
            val pendingRequests = requestsSnap.documents.count {
                (it.getString("status") ?: RequestStatus.PENDING.name) in activeStatuses
            }
            val fulfilledRequests = requestsSnap.documents.count {
                it.getString("status") == RequestStatus.FULFILLED.name
            }

            val verifiedOrgs = try {
                db.collection("organizations")
                    .whereEqualTo("isVerified", true)
                    .limit(200)
                    .get()
                    .await()
                    .size()
            } catch (_: Exception) {
                0
            }

            SystemMetrics(
                totalRegisteredUsers = totalDonors,
                activeDonors = activeDonors,
                emergencyDonorsOnline = emergencyDonorsOnline,
                pendingRequests = pendingRequests,
                fulfilledRequests = fulfilledRequests,
                verifiedOrganizations = verifiedOrgs,
                totalDonationsCompleted = totalDonationsCompleted,
                serverStatus = "Operational",
                appCheckStatus = "Initialized"
            )
        } catch (e: Exception) {
            Log.w("FirestoreService", "Unable to fetch full live metrics: ${e.message}")
            SystemMetrics(
                serverStatus = "Degraded / Offline",
                appCheckStatus = "Initialized"
            )
        }
    }

    suspend fun fetchEmergencyBroadcastArmed(): Boolean {
        return try {
            val snap = db.collection("system_config")
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

    suspend fun setEmergencyBroadcastArmed(
        armed: Boolean,
        adminUid: String,
        adminEmail: String
    ): Resource<Boolean> {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        } ?: return Resource.Error("Administrator authentication is required.")

        val configRef = db.collection("system_config").document("emergency_broadcast")
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
                    "updatedAt" to now
                )
            )
            batch.set(
                auditRef,
                mapOf(
                    "actorUid" to actorUid,
                    "actorEmail" to actorEmail,
                    "action" to if (armed) "EMERGENCY_BROADCAST_ARMED" else "EMERGENCY_BROADCAST_PAUSED",
                    "targetId" to "emergency_broadcast",
                    "targetType" to "SYSTEM_CONFIG",
                    "details" to if (armed) "Emergency broadcast engine armed" else "Emergency broadcast engine paused",
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
}
