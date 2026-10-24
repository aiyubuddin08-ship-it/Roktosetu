package com.example.data.firebase

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Client wrapper for RoktoSetu Callable Cloud Functions (`asia-south1`).
 * Connects to deployed server-side functions (`sendEmergencyBroadcast`, `verifyDonation`,
 * `reviewVerificationRequest`) with clean error propagation so the repository can
 * gracefully fall back when needed.
 */
class FunctionsService(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1")
) {
    /**
     * Triggers an emergency blood request broadcast to matching donors in the same city/district.
     */
    suspend fun sendEmergencyBroadcast(
        requestId: String,
        city: String,
        bloodGroup: String,
        patientName: String,
        hospitalName: String
    ): Resource<Int> {
        return try {
            val payload = hashMapOf(
                "requestId" to requestId,
                "city" to city,
                "bloodGroup" to bloodGroup,
                "patientName" to patientName,
                "hospitalName" to hospitalName
            )
            val result = functions
                .getHttpsCallable("sendEmergencyBroadcast")
                .call(payload)
                .await()
            val data = result.getData() as? Map<*, *>
            val notifiedCount = (data?.get("notifiedCount") as? Number)?.toInt() ?: 0
            Resource.Success(notifiedCount)
        } catch (e: Exception) {
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    /**
     * Records a verified donation via the server-side callable function (`verifyDonation`).
     */
    suspend fun verifyDonationServerSide(
        donorId: String,
        requestId: String,
        patientName: String,
        hospitalName: String,
        city: String,
        bloodGroup: String,
        bags: Int = 1
    ): Resource<String> {
        return try {
            val payload = hashMapOf(
                "donorId" to donorId,
                "requestId" to requestId,
                "patientName" to patientName,
                "hospitalName" to hospitalName,
                "city" to city,
                "bloodGroup" to bloodGroup,
                "bags" to bags
            )
            val result = functions
                .getHttpsCallable("verifyDonation")
                .call(payload)
                .await()
            val data = result.getData() as? Map<*, *>
            val historyId = data?.get("historyId") as? String ?: ""
            Resource.Success(historyId)
        } catch (e: Exception) {
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }

    /**
     * Approves or rejects a donor/organization verification request via `reviewVerificationRequest`.
     */
    suspend fun reviewVerificationRequestServerSide(
        verificationRequestId: String,
        decision: String,
        reviewNotes: String
    ): Resource<String> {
        return try {
            val payload = hashMapOf(
                "verificationRequestId" to verificationRequestId,
                "decision" to decision,
                "reviewNotes" to reviewNotes
            )
            val result = functions
                .getHttpsCallable("reviewVerificationRequest")
                .call(payload)
                .await()
            val data = result.getData() as? Map<*, *>
            val status = data?.get("status") as? String ?: decision
            Resource.Success(status)
        } catch (e: Exception) {
            Resource.Error(FirebaseSecurityHelper.mapExceptionToMessage(e), e)
        }
    }
}
