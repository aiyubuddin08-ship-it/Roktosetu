package com.example.domain

/**
 * Formal specification of the production Security Architecture, Firestore Security Rules,
 * Firebase Storage Security Rules, and Trusted Cloud Functions requirements.
 */
object SecurityRulesDefinition {

    val FIRESTORE_RULES: String = """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;
    }

    function isSignedIn() { return request.auth != null; }
    function isOwner(userId) { return isSignedIn() && request.auth.uid == userId; }
    function isAdmin() {
      return isSignedIn() && (
        exists(/databases/$(database)/documents/admins/$(request.auth.uid)) ||
        ('admin' in request.auth.token && request.auth.token.admin == true)
      );
    }

    // 0. Trusted Admin Registry (/admins/{adminUid}) - Server-Managed Only
    match /admins/{adminUid} {
      allow get: if isOwner(adminUid) || isAdmin();
      allow list, create, update, delete: if false;
    }

    // 1. Private User Profiles (/users/{userId}) - Strictly isolated to owner & admin
    match /users/{userId} {
      allow get: if isOwner(userId) || isAdmin();
      allow list: if isSignedIn() && (resource.data.uid == request.auth.uid || isAdmin());
      allow create: if isOwner(userId) &&
        request.resource.data.role == 'USER' &&
        request.resource.data.verificationStatus == 'UNVERIFIED' &&
        request.resource.data.donationCount == 0 &&
        request.resource.data.isEmergencyDonor == false;
      allow update: if (isOwner(userId) &&
        !request.resource.data.diff(resource.data).affectedKeys().hasAny([
          'uid', 'role', 'verificationStatus', 'donationCount', 'badges', 'organizationId', 'createdAt'
        ])) || isAdmin();
      allow delete: if isOwner(userId) || isAdmin();
    }

    // 2. Sanitized Public Donors (/public_donors/{donorUid}) - No private email/DOB/hidden phone
    match /public_donors/{donorUid} {
      allow get, list: if isSignedIn() && (resource.data.isDonor == true || isAdmin());
      allow create: if isOwner(donorUid) &&
        request.resource.data.verificationStatus == 'UNVERIFIED' &&
        request.resource.data.donationCount == 0;
      allow update: if (isOwner(donorUid) &&
        !request.resource.data.diff(resource.data).affectedKeys().hasAny([
          'uid', 'verificationStatus', 'donationCount'
        ])) || isAdmin();
      allow delete: if isOwner(donorUid) || isAdmin();
    }

    // 3. Emergency Blood Requests (/blood_requests/{requestId}) - Validated Status Transitions
    match /blood_requests/{requestId} {
      allow get, list: if isSignedIn();
      allow create: if isSignedIn() &&
        request.resource.data.requesterUid == request.auth.uid &&
        request.resource.data.status == 'PENDING';
      allow update: if isSignedIn() && (
        isAdmin() ||
        (!(resource.data.status in ['FULFILLED', 'CANCELLED', 'EXPIRED']) && (
          // Donor accepts PENDING request
          (request.auth.uid != resource.data.requesterUid &&
           resource.data.status == 'PENDING' &&
           request.resource.data.status in ['DONOR_RESPONDED', 'ACCEPTED'] &&
           request.resource.data.acceptedDonorUid == request.auth.uid &&
           request.resource.data.diff(resource.data).affectedKeys().hasOnly([
             'status', 'acceptedDonorUid', 'acceptedDonorName', 'respondedDonorUids', 'updatedAt'
           ])) ||
          // Requester marks FULFILLED after donor response or CANCELLED
          (isOwner(resource.data.requesterUid) &&
           request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'updatedAt']))
        ))
      );
    }

    // 4. Conversations & Messages - Immutable Participant List & Strict Isolation
    match /conversations/{convId} {
      allow get, list: if isSignedIn() && (request.auth.uid in resource.data.participants);
      allow create: if isSignedIn() && (request.auth.uid in request.resource.data.participants);
      allow update: if isSignedIn() &&
        (request.auth.uid in resource.data.participants) &&
        request.resource.data.participants == resource.data.participants;
    }
  }
}
""".trimIndent()

    val STORAGE_RULES: String = """
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if false;
    }
    function isAdmin() {
      return request.auth != null && (
        ('admin' in request.auth.token && request.auth.token.admin == true) ||
        firestore.exists(/databases/ai-studio-android-lifelink-32b78329-0908-44db-bbd3-9204072ef691/documents/admins/$(request.auth.uid))
      );
    }
    match /users/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null && request.auth.uid == userId
        && fileName.matches('^profile\\.(jpg|jpeg|png|webp)$')
        && request.resource.size <= 5 * 1024 * 1024
        && request.resource.contentType.matches('image/(jpeg|png|webp)');
    }
    match /verifications/{userId}/{documentId} {
      allow read: if request.auth != null && (request.auth.uid == userId || isAdmin());
      allow create, update: if request.auth != null && request.auth.uid == userId
        && request.resource.size <= 10 * 1024 * 1024;
    }
  }
}
""".trimIndent()

    val CLOUD_FUNCTIONS_SPECIFICATION: List<String> = listOf(
        "sendEmergencyBroadcast: Enforces App Check & Auth, verifies request ownership, and dispatches FCM notifications strictly to available compatible donors in the matching division/district (idempotent).",
        "acceptBloodRequest: Runs a Firestore transaction to verify blood group compatibility, prevent self-acceptance or race conditions, and record donor response.",
        "verifyDonation: Restricted to verified organizations & super-admins; uses deterministic IDs in a transaction to prevent duplicate donation records and update donor resting status.",
        "reviewVerificationRequest: Super-admin transactional workflow to approve/reject donor or organization verification tasks and record immutable audit logs.",
        "expireStaleBloodRequests: Scheduled background job that automatically transitions expired PENDING requests to EXPIRED."
    )
}
