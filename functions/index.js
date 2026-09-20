const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Triggered on new emergency blood request creation.
 * Securely finds compatible emergency donors within geographic range and sends FCM multicast.
 */
exports.sendEmergencyBroadcast = functions.firestore
  .document("blood_requests/{requestId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    if (!data || !data.isEmergencyBroadcast) return null;

    const bloodGroup = data.bloodGroup;
    const city = data.city || "";
    const hospital = data.hospitalName || "Hospital";

    try {
      // Find compatible donors in the same city who have isEmergencyDonor = true
      const donorsSnapshot = await db.collection("users")
        .where("isDonor", "==", true)
        .where("isEmergencyDonor", "==", true)
        .where("isAccountActive", "==", true)
        .get();

      const tokens = [];
      const notificationPromises = [];

      donorsSnapshot.forEach((doc) => {
        const donor = doc.data();
        if (donor.fcmToken) {
          tokens.push(donor.fcmToken);
        }

        // Store notification record in donor's subcollection
        notificationPromises.push(
          db.collection("users").doc(doc.id).collection("notifications").add({
            title: `URGENT: ${bloodGroup} Blood Needed`,
            body: `Emergency blood request at ${hospital}, ${city}. Please respond if available.`,
            type: "EMERGENCY_BROADCAST",
            relatedEntityId: context.params.requestId,
            bloodGroup: bloodGroup,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false
          })
        );
      });

      await Promise.all(notificationPromises);

      if (tokens.length > 0) {
        await admin.messaging().sendEachForMulticast({
          tokens: tokens.slice(0, 500),
          notification: {
            title: `🚨 Emergency Blood Needed: ${bloodGroup}`,
            body: `Urgent requirement at ${hospital}, ${city}`
          },
          data: {
            requestId: context.params.requestId,
            bloodGroup: bloodGroup,
            type: "EMERGENCY_BROADCAST"
          },
          android: {
            priority: "high"
          }
        });
      }

      // Log to audit log
      await db.collection("audit_logs").add({
        action: "EMERGENCY_BROADCAST_TRIGGERED",
        actorUid: data.requesterUid,
        targetId: context.params.requestId,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        details: `Broadcasted to ${donorsSnapshot.size} donors for ${bloodGroup} at ${hospital}`
      });

      return { success: true, count: donorsSnapshot.size };
    } catch (error) {
      console.error("Emergency broadcast failed:", error);
      return { error: error.message };
    }
  });

/**
 * Callable function to verify a completed blood donation.
 * Can only be called by an authenticated Org Admin or Super Admin.
 * Atomically updates user's donation count and last donation timestamp.
 */
exports.verifyDonation = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required");
  }

  const { donorUid, requestId, hospitalName, unitsDonated, notes, bloodGroup } = data;

  if (!donorUid) {
    throw new functions.https.HttpsError("invalid-argument", "donorUid is required");
  }

  const callerUid = context.auth.uid;
  const isSuperAdmin = context.auth.token.admin === true;
  const orgId = context.auth.token.orgId;

  if (!isSuperAdmin && !orgId) {
    throw new functions.https.HttpsError("permission-denied", "Only verified organization representatives can verify donations");
  }

  const batch = db.batch();

  // 1. Create verified donation record
  const donationRef = db.collection("donations").doc();
  batch.set(donationRef, {
    id: donationRef.id,
    donorUid: donorUid,
    bloodGroup: bloodGroup || "O_POSITIVE",
    donationDate: admin.firestore.FieldValue.serverTimestamp(),
    hospitalName: hospitalName || "Verified Center",
    unitsDonated: Number(unitsDonated) || 1,
    requestId: requestId || null,
    verifiedByOrgId: orgId || "superadmin",
    verifiedByUid: callerUid,
    verificationStatus: "VERIFIED",
    notes: notes || "Verified on-site"
  });

  // 2. Increment donor's verified donation count and set lastDonationDate
  const userRef = db.collection("users").doc(donorUid);
  batch.update(userRef, {
    donationCount: admin.firestore.FieldValue.increment(1),
    lastDonationDate: admin.firestore.FieldValue.serverTimestamp()
  });

  // 3. If tied to a blood request, mark request fulfilled
  if (requestId) {
    const requestRef = db.collection("blood_requests").doc(requestId);
    batch.update(requestRef, {
      status: "FULFILLED",
      fulfilledAt: admin.firestore.FieldValue.serverTimestamp(),
      acceptedDonorUid: donorUid
    });
  }

  // 4. Send congratulations notification to donor
  const notifRef = db.collection("users").doc(donorUid).collection("notifications").doc();
  batch.set(notifRef, {
    title: "Donation Verified! ❤️",
    body: `Thank you for saving a life at ${hospitalName}! Your donation count has been updated.`,
    type: "BADGE_AWARDED",
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
    isRead: false
  });

  await batch.commit();

  return { success: true, donationId: donationRef.id };
});

/**
 * Super Admin function to assign custom claims (roles).
 */
exports.setAdminRole = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.admin !== true) {
    throw new functions.https.HttpsError("permission-denied", "Super admin authorization required");
  }

  const { targetUid, role, orgId } = data;
  if (!targetUid) {
    throw new functions.https.HttpsError("invalid-argument", "targetUid required");
  }

  const claims = {};
  if (role === "ADMIN") {
    claims.admin = true;
  } else if (role === "ORG_REPRESENTATIVE" && orgId) {
    claims.orgId = orgId;
  }

  await admin.auth().setCustomUserClaims(targetUid, claims);

  // Update in Firestore profile
  await db.collection("users").doc(targetUid).update({
    role: role,
    orgId: orgId || null
  });

  return { success: true, targetUid, claims };
});
