# রক্তসেতু (RoktoSetu) — Blood Donation & Emergency Assistance Platform

**Application ID:** `com.aistudio.roktosetu.app`  
**Stack:** Kotlin 2.2.10 • Jetpack Compose (Material 3) • Firebase (Auth, Custom Firestore Database, Storage, Cloud Functions v2, FCM, App Check) • Room Offline Cache

---

## 1. Prerequisites & Secrets Configuration

- **JDK:** Java 17+
- **Gradle:** 9.3.1
- **Android SDK:** `compileSdk = 36`, `minSdk = 24`
- **Firebase Configuration:** Valid `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`
- **Secrets Panel / `.env` Configuration:**
  Copy `.env.example` or configure the following variables in the **AI Studio Secrets panel**:
  - `GOOGLE_WEB_CLIENT_ID`: Web OAuth 2.0 Client ID from Firebase Console (Authentication → Sign-in method → Google) for Credential Manager Google Sign-In.
  - `PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER`: Optional GCP project number override for Firebase App Check Play Integrity.

---

## 2. Architecture & Security Highlights (Phases 1–6)

1. **Zero-Trust Security Rules (`firestore.rules` & `storage.rules`):**
   - Custom Firestore Database (`ai-studio-android-lifelink-32b78329-0908-44db-bbd3-9204072ef691`) with strict schema, type, length, and ownership validation.
   - Zero client-side `SUPER_ADMIN` auto-elevation; admin privileges require server-side custom claims (`request.auth.token.admin == true`) or `/admins/{uid}`.
   - Private `/users/{uid}` profiles (PII such as phone, email, exact coordinates, and `fcmToken`) are strictly isolated from sanitized `/public_donors/{uid}` documents.
   - Immutable `/audit_logs/{logId}` collection (`allow update, delete: if false;`) and `/system_config/emergency_broadcast` configuration toggle.
   - Firebase Storage rules (`storage.rules`) restrict `/verification_docs/{userId}/{fileName}` to the authenticated owner (`<= 10 MB`, `image/*` or `application/pdf`).

2. **Authentication & Session Integrity:**
   - Email/Password and Google Sign-In via Jetpack Credential Manager (`FirebaseAuthService.kt`).
   - Mandatory email verification gate for password-based accounts before accessing dashboard routes.
   - Re-authentication prompt before account deletion and neutral password-reset confirmation messaging.

3. **Real-Time Blood Request State Machine, Offline Room Sync & Donor Card Export:**
   - Blood request lifecycle (`PENDING` → `ACCEPTED` → `FULFILLED` / `CANCELLED`) with `acceptedDonorUid` and `expiresAt`.
   - Offline-first Room cache (`RoktoSetuDatabase` v2) for sanitized public donors, blood requests, organizations, and chat messages.
   - Real PNG Donor Card export (`DonorCardExporter.kt`) rendered via `Bitmap` + `Canvas` to `Pictures/RoktoSetu` using Android `MediaStore`.

4. **Cloud Functions (`functions/index.js`) & FCM (`RoktoSetuMessagingService.kt`):**
   - `sendEmergencyBroadcast`: Triggered when a `CRITICAL` blood request is created; respects `/system_config/emergency_broadcast`, filters compatible donors (`BloodCompatibilityHelper` matrix + 90-day cooldown), and dispatches FCM multicast notifications.
   - `onVerificationStatusUpdated`: Synchronizes admin review decisions (`APPROVED`/`REJECTED`) across `/users/{uid}`, `/public_donors/{uid}`, and `/organizations/{orgId}`, and notifies the user via FCM.
   - `expireStaleBloodRequests`: Scheduled job that marks expired `PENDING` blood requests as `CANCELLED`.

---

## 3. Server-Side Admin Bootstrap (`tools/set-admin-claim.js`)

To grant or revoke `SUPER_ADMIN` privileges without any client-side backdoor:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccountKey.json \
node tools/set-admin-claim.js --uid <USER_FIREBASE_UID>
```

To revoke admin privileges:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccountKey.json \
node tools/set-admin-claim.js --uid <USER_FIREBASE_UID> --revoke
```

---

## 4. Firebase Deployment Commands

```bash
# Deploy Firestore Rules, Indexes, and Storage Rules
firebase deploy --only firestore:rules,firestore:indexes,storage

# Install & Deploy Cloud Functions
npm --prefix functions install
firebase deploy --only functions
```

---

## 5. Build & Automated Verification Commands

### Debug & Release Builds
```bash
gradle :app:assembleDebug
gradle :app:assembleRelease
```

### Firestore Security Rules Tests (Local Emulator)
```bash
firebase emulators:exec --only firestore "node --test firestore.test.js"
```

### Android Unit & Robolectric Tests
```bash
firebase emulators:exec --only firestore,auth "gradle :app:testDebugUnitTest"
```
