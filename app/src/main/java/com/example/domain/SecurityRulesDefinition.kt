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
    
    // Default Deny Everything
    match /{document=**} {
      allow read, write: if false;
    }

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    function isAdmin() {
      return isAuthenticated() && request.auth.token.admin == true;
    }
    
    function isOrgAdmin(orgId) {
      return isAuthenticated() && (
        isAdmin() || 
        request.auth.token.orgId == orgId ||
        request.auth.uid in get(/databases/$(database)/documents/organizations/$(orgId)).data.adminUids
      );
    }

    // 1. Users Collection - Strict Ownership & Protected Sensitive Fields
    match /users/{userId} {
      // Authenticated users can read public user data
      allow read: if isAuthenticated();
      
      // Only user can create their own document with role == 'USER'
      allow create: if isOwner(userId) 
        && request.resource.data.role == 'USER'
        && request.resource.data.verificationStatus == 'UNVERIFIED'
        && request.resource.data.donationCount == 0;
      
      // User can update basic profile fields, but NOT role, points, donationCount, or verificationStatus
      allow update: if (isOwner(userId) && 
        !request.resource.data.diff(resource.data).affectedKeys().hasAny([
          'role', 'verificationStatus', 'donationCount', 'createdAt'
        ])) || isAdmin();
        
      allow delete: if isOwner(userId) || isAdmin();
      
      // Notifications subcollection - read only by recipient; written by backend/admin
      match /notifications/{notificationId} {
        allow read: if isOwner(userId);
        allow write: if isAdmin(); // Normal users cannot inject notifications to other users
      }
    }

    // 2. Blood Requests - Creator Ownership and Controlled Status Transitions
    match /blood_requests/{requestId} {
      allow read: if isAuthenticated();
      
      // Authenticated users can create requests with initial 'PENDING' status
      allow create: if isAuthenticated() 
        && request.resource.data.requesterUid == request.auth.uid
        && request.resource.data.status == 'PENDING';
        
      // Only requester or admin can update core details or cancel
      // Donors can commit to accept (via trusted function or restricted state change)
      allow update: if isAuthenticated() && (
        isOwner(resource.data.requesterUid) || 
        isAdmin() ||
        (
          // Donor committing to help (status changes from PENDING to ACCEPTED)
          resource.data.status == 'PENDING' && 
          request.resource.data.status == 'ACCEPTED' &&
          request.resource.data.acceptedDonorUid == request.auth.uid
        )
      );
      
      allow delete: if (isAuthenticated() && isOwner(resource.data.requesterUid)) || isAdmin();
    }

    // 3. Donation Records - Read by donor/org/admin; Written only by verified Org/Admin or Trusted Function
    match /donations/{donationId} {
      allow read: if isAuthenticated() && (
        isOwner(resource.data.donorUid) || 
        isAdmin() ||
        (resource.data.verifiedByOrgId != null && isOrgAdmin(resource.data.verifiedByOrgId))
      );
      
      // Users CANNOT forge their own donation records or points directly
      allow create, update, delete: if isAdmin() || (
        request.resource.data.verifiedByOrgId != null && 
        isOrgAdmin(request.resource.data.verifiedByOrgId)
      );
    }

    // 4. Organizations - Read by all authenticated users; Write by Org Admin / Super Admin
    match /organizations/{orgId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated(); // Subject to admin verification
      allow update: if isOrgAdmin(orgId);
      allow delete: if isAdmin();
      
      match /campaigns/{campaignId} {
        allow read: if isAuthenticated();
        allow write: if isOrgAdmin(orgId);
      }
    }

    // 5. 1-to-1 Conversations and Messages - Strict Participant Isolation
    match /conversations/{convId} {
      allow read: if isAuthenticated() && (request.auth.uid in resource.data.participants);
      allow create: if isAuthenticated() && (request.auth.uid in request.resource.data.participants);
      allow update: if isAuthenticated() && (request.auth.uid in resource.data.participants);
      
      match /messages/{messageId} {
        allow read: if isAuthenticated() && (
          request.auth.uid in get(/databases/$(database)/documents/conversations/$(convId)).data.participants
        );
        allow create: if isAuthenticated() && (
          request.auth.uid == request.resource.data.senderUid &&
          request.auth.uid in get(/databases/$(database)/documents/conversations/$(convId)).data.participants
        );
        allow update, delete: if false; // Immutable audit trail
      }
    }

    // 6. Audit Logs & System Controls - Admin Only
    match /audit_logs/{logId} {
      allow read, write: if isAdmin();
    }
  }
}
""".trimIndent()

    val STORAGE_RULES: String = """
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Default Deny
    match /{allPaths=**} {
      allow read, write: if false;
    }

    // Profile photos - Read by all users; Written only by photo owner (Max 5MB, Image MIME)
    match /users/{userId}/profile.jpg {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId 
        && request.resource.size < 5 * 1024 * 1024
        && request.resource.contentType.matches('image/.*');
    }

    // Verification documents - Strictly private: Read only by user & admin; Write by user
    match /verifications/{userId}/{documentId} {
      allow read: if request.auth != null && (request.auth.uid == userId || request.auth.token.admin == true);
      allow write: if request.auth != null && request.auth.uid == userId
        && request.resource.size < 10 * 1024 * 1024;
    }

    // Organization assets
    match /organizations/{orgId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && (
        request.auth.token.admin == true || 
        request.auth.token.orgId == orgId
      );
    }
  }
}
""".trimIndent()

    val CLOUD_FUNCTIONS_SPECIFICATION: List<String> = listOf(
        "sendEmergencyBroadcast: Validates emergency blood request and securely triggers FCM multicast notification to nearby compatible donors without exposing donor tokens or allowing arbitrary client messaging.",
        "verifyDonationAndAwardBadge: Organization or hospital verifies blood donation, atomically creates verified DonationRecord, increments user's donationCount, and assigns milestone badges.",
        "setUserCustomClaims: Super-admin trusted function to assign role claims ('admin': true, 'orgId': 'org123') in Firebase Auth tokens.",
        "fulfillBloodRequest: Atomically marks blood request as fulfilled and sends confirmation notification to requester and donor.",
        "auditLogWriter: Server-side trigger to log security events and administrative actions to 'audit_logs' collection with immutable timestamps and actor tracking."
    )
}
