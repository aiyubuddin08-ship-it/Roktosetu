# রক্তসেতু (RoktoSetu) — Blood Donation & Emergency Assistance Platform

**Application ID:** `com.aistudio.roktosetu.app`  
**Stack:** Kotlin 2.2.10 • AGP 9.1.1 • KSP 2.3.5 • Jetpack Compose (Material 3) • Firebase (Auth, Custom Firestore Database, Storage, Cloud Functions v2 on Node 20, FCM, App Check) • Room Offline Cache

---

## 1. Prerequisites & Configuration

- **JDK:** Java 17+
- **Gradle:** 9.3.1
- **Android SDK:** `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`
- **Firebase Configuration:** Valid `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` (`ai-studio-android-lifelink-32b78329-0908-44db-bbd3-9204072ef691`)
- **Release Signing (`keystore.properties`):**
  For production release builds outside AI Studio, copy `keystore.properties.example` to `keystore.properties` (git-ignored) and set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.

---

## 2. Architecture & Security Highlights

1. **Zero-Trust Security Rules (`firestore.rules` & `storage.rules`):**
   - Custom Firestore Database (`ai-studio-android-lifelink-32b78329-0908-44db-bbd3-9204072ef691`) with strict default-deny, field-level `affectedKeys()`, schema, type, length, and ownership validation.
   - Server-verified `SUPER_ADMIN` authorization via Firebase Auth Custom Claim (`request.auth.token.admin == true`) or `/admins/{uid}` document. Super Admin role (`SUPER_ADMIN`) is strictly separated from donor verification status (`VerificationStatus.VERIFIED`).
   - Private `/users/{uid}` profiles (PII such as phone, email, DOB, and `fcmToken`) are isolated from sanitized `/public_donors/{uid}` documents.
   - Immutable `/audit_logs/{logId}` (`allow update, delete: if false;`), `/reports/{reportId}`, `/announcements/{announcementId}`, `/security_alerts/{alertId}`, and `/system_config/{configId}`.
   - Firebase Storage rules (`storage.rules`) restrict `/verification_docs/{userId}/{fileName}` to the authenticated owner (`<= 10 MB`, `image/*` or `application/pdf`).

2. **Super Admin Control Center (`AdminDashboardScreen.kt` & `AdminControlModules.kt`):**
   - Complete Bengali control center covering 9 operational modules: Overview & Live Telemetry, Users & Donors Management (including Account Suspension/Reactivation), Donor & Donation Verification, Blood Requests Management, Emergency Broadcasts (with 15-minute duplicate guard), Organization Verification, Notifications & Announcements (with safe Preview mode), Complaints & Abuse Reports, System Settings & Health/Export, and Immutable Audit Logs.

3. **Security & Privacy Center (`SecurityCenterScreen.kt`):**
   - 8 structured Bengali sections replacing raw code dumps with human-readable database protection cards, account & session security verification, privacy policy breakdown, app permissions, security alerts review, activity history, and Super-Admin-only technical diagnostics.

4. **Cloud Functions (`functions/index.js`, Node 20) & FCM (`RoktoSetuMessagingService.kt`):**
   - `sendEmergencyBroadcast`: Triggered when a critical blood request is created; checks `/system_config/emergency_broadcast`, filters compatible donors (`BloodCompatibilityHelper` matrix + 90-day cooldown), and dispatches FCM notifications.
   - `onVerificationStatusUpdated`: Synchronizes admin review decisions across `/users/{uid}`, `/public_donors/{uid}`, and `/organizations/{orgId}`.
   - `expireStaleBloodRequests`: Scheduled job that marks expired `PENDING` blood requests as `CANCELLED`.

---

## 3. Server-Side Admin Bootstrap (`tools/set-admin-claim.js`)

To grant or revoke `SUPER_ADMIN` privileges (without altering donor verification status):

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

# Install & Deploy Cloud Functions (Node 20)
npm --prefix functions install
firebase deploy --only functions
```

---

## 5. Build & Verification Commands

### Generate Local Gradle Wrapper (when building outside AI Studio)
```bash
gradle wrapper --gradle-version 9.3.1
```

### Debug & Release Builds
```bash
gradle :app:assembleDebug
gradle :app:assembleRelease
```

### Android Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```
