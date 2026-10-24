/**
 * RoktoSetu (রক্তসেতু) - Trusted Firebase Cloud Functions (Backend Security Layer)
 *
 * Enforces server-side authentication, App Check, RBAC verification, blood-group & location
 * matching for emergency notifications, atomic transactions for request acceptance and
 * donation verification, idempotency, and scheduled request expiration.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const { getFirestore } = require("firebase-admin/firestore");

admin.initializeApp();
const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-lifelink-32b78329-0908-44db-bbd3-9204072ef691";
const db = getFirestore(DATABASE_ID);

// Medical red blood cell compatibility matrix: recipient -> allowed donor blood groups
const COMPATIBLE_DONORS_BY_RECIPIENT = {
  A_POSITIVE: ["A_POSITIVE", "A_NEGATIVE", "O_POSITIVE", "O_NEGATIVE"],
  A_NEGATIVE: ["A_NEGATIVE", "O_NEGATIVE"],
  B_POSITIVE: ["B_POSITIVE", "B_NEGATIVE", "O_POSITIVE", "O_NEGATIVE"],
  B_NEGATIVE: ["B_NEGATIVE", "O_NEGATIVE"],
  AB_POSITIVE: [
    "A_POSITIVE", "A_NEGATIVE", "B_POSITIVE", "B_NEGATIVE",
    "O_POSITIVE", "O_NEGATIVE", "AB_POSITIVE", "AB_NEGATIVE"
  ],
  AB_NEGATIVE: ["AB_NEGATIVE", "A_NEGATIVE", "B_NEGATIVE", "O_NEGATIVE"],
  O_POSITIVE: ["O_POSITIVE", "O_NEGATIVE"],
  O_NEGATIVE: ["O_NEGATIVE"]
};

const VALID_BLOOD_GROUPS = new Set(Object.keys(COMPATIBLE_DONORS_BY_RECIPIENT));

function sanitizeString(val, maxLen = 150) {
  if (typeof val !== "string") return "";
  return val.trim().slice(0, maxLen);
}

async function isCallerSuperAdmin(auth) {
  if (!auth || !auth.uid) return false;
  if (auth.token && auth.token.admin === true) return true;
  const adminDoc = await db.collection("admins").doc(auth.uid).get();
  return adminDoc.exists;
}

async function isCallerAuthorizedVerifier(auth, orgId) {
  if (!auth || !auth.uid) return false;
  if (await isCallerSuperAdmin(auth)) return true;
  if (!orgId || typeof orgId !== "string") return false;
  const orgDoc = await db.collection("organizations").doc(orgId).get();
  if (!orgDoc.exists) return false;
  const orgData = orgDoc.data();
  return (
    orgData.isVerified === true &&
    Array.isArray(orgData.adminUids) &&
    orgData.adminUids.includes(auth.uid)
  );
}

/**
 * 1. Send Emergency Blood Request Broadcast (Targeted & Idempotent)
 * Matches only available donors with compatible blood groups in the target division/district.
 */
exports.sendEmergencyBroadcast = onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const data = request.data || {};
    const requestId = sanitizeString(data.requestId, 128);
    if (!requestId) {
      throw new HttpsError("invalid-argument", "A valid requestId is required.");
    }

    const reqRef = db.collection("blood_requests").doc(requestId);
    const reqSnap = await reqRef.get();
    if (!reqSnap.exists) {
      throw new HttpsError("not-found", "Blood request not found.");
    }

    const reqData = reqSnap.data();
    const requesterUid = reqData.requesterUid || reqData.requesterId || "";
    const isSuperAdmin = await isCallerSuperAdmin(request.auth);
    if (requesterUid !== request.auth.uid && !isSuperAdmin) {
      throw new HttpsError(
        "permission-denied",
        "Only the request creator or an administrator can trigger an emergency broadcast."
      );
    }

    if (!["PENDING", "OPEN"].includes(reqData.status)) {
      throw new HttpsError(
        "failed-precondition",
        "Broadcasts can only be sent for PENDING or OPEN blood requests."
      );
    }

    const sysConfigSnap = await db.collection("system_settings").doc("emergency_broadcast").get();
    if (sysConfigSnap.exists && sysConfigSnap.data().armed === false) {
      throw new HttpsError(
        "failed-precondition",
        "Emergency push broadcast engine is temporarily paused by system administration."
      );
    }

    // Idempotency guard: prevent duplicate broadcasts for the same request
    if (reqData.broadcastDispatchedAt) {
      return {
        success: true,
        alreadyDispatched: true,
        notifiedCount: reqData.broadcastNotifiedCount || 0
      };
    }

    const recipientBloodGroup = reqData.bloodGroup;
    if (!VALID_BLOOD_GROUPS.has(recipientBloodGroup)) {
      throw new HttpsError("invalid-argument", "Invalid blood group on request.");
    }

    const compatibleGroups = COMPATIBLE_DONORS_BY_RECIPIENT[recipientBloodGroup] || [recipientBloodGroup];
    const targetDistrict = sanitizeString(reqData.district || reqData.city, 60);
    const targetDivision = sanitizeString(reqData.division, 60);

    // Query available emergency donors with compatible blood groups in the target district/division
    let donorsQuery = db
      .collection("users")
      .where("isDonor", "==", true)
      .where("isEmergencyDonor", "==", true)
      .where("donorAvailability", "==", "AVAILABLE")
      .where("bloodGroup", "in", compatibleGroups);

    const donorsSnapshot = await donorsQuery.limit(200).get();

    const tokens = [];
    const matchedDonorUids = [];

    donorsSnapshot.forEach((doc) => {
      if (doc.id === requesterUid) return;
      const d = doc.data();
      const donorDistrict = sanitizeString(d.district || (d.location && d.location.district) || d.city, 60);
      const donorDivision = sanitizeString(d.division || (d.location && d.location.division), 60);

      const districtMatch =
        !targetDistrict ||
        donorDistrict.toLowerCase() === targetDistrict.toLowerCase();
      const divisionMatch =
        !targetDivision ||
        donorDivision.toLowerCase() === targetDivision.toLowerCase();

      if (districtMatch || divisionMatch) {
        matchedDonorUids.push(doc.id);
        if (d.fcmToken && typeof d.fcmToken === "string") {
          tokens.push(d.fcmToken);
        }
      }
    });

    const unitsNeeded = reqData.unitsRequired || reqData.bagsNeeded || 1;
    let successCount = 0;
    if (tokens.length > 0) {
      const message = {
        notification: {
          title: `Urgent ${recipientBloodGroup.replace("_", " ")} Blood Needed`,
          body: `Patient at ${sanitizeString(reqData.hospitalName, 80)} (${targetDistrict || "Bangladesh"}) needs ${unitsNeeded} bag(s).`
        },
        data: {
          requestId,
          bloodGroup: recipientBloodGroup,
          district: targetDistrict,
          type: "EMERGENCY_BLOOD_REQUEST"
        },
        tokens: tokens.slice(0, 500)
      };

      const response = await admin.messaging().sendEachForMulticast(message);
      successCount = response.successCount;
    }

    await reqRef.update({
      broadcastDispatchedAt: Date.now(),
      broadcastNotifiedCount: matchedDonorUids.length,
      updatedAt: Date.now()
    });

    await db.collection("audit_logs").add({
      actorUid: request.auth.uid,
      actorEmail: request.auth.token.email || "authenticated-user",
      action: "EMERGENCY_BROADCAST_SENT",
      targetId: requestId,
      targetType: "BLOOD_REQUEST",
      details: `Matched ${matchedDonorUids.length} compatible donors (${successCount} push notifications delivered) in ${targetDistrict || targetDivision}`,
      timestamp: Date.now()
    });

    return {
      success: true,
      matchedDonors: matchedDonorUids.length,
      notifiedCount: successCount
    };
  }
);

/**
 * 2. Atomic Blood Request Acceptance (Transaction-Protected)
 * Prevents race conditions when multiple donors attempt to accept the same request.
 */
exports.acceptBloodRequest = onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError("unauthenticated", "You must be signed in to respond to a blood request.");
    }

    const requestId = sanitizeString((request.data || {}).requestId, 128);
    if (!requestId) {
      throw new HttpsError("invalid-argument", "requestId is required.");
    }

    const donorUid = request.auth.uid;
    const reqRef = db.collection("blood_requests").doc(requestId);
    const donorRef = db.collection("users").doc(donorUid);

    const result = await db.runTransaction(async (tx) => {
      const [reqDoc, donorDoc] = await Promise.all([tx.get(reqRef), tx.get(donorRef)]);
      if (!reqDoc.exists) {
        throw new HttpsError("not-found", "Blood request does not exist.");
      }
      if (!donorDoc.exists) {
        throw new HttpsError("failed-precondition", "Please complete your donor profile before accepting requests.");
      }

      const reqData = reqDoc.data();
      const donorData = donorDoc.data();
      const requesterUid = reqData.requesterUid || reqData.requesterId || "";

      if (requesterUid === donorUid) {
        throw new HttpsError("failed-precondition", "You cannot accept your own blood request.");
      }

      if (!["PENDING", "OPEN", "DONOR_RESPONDED"].includes(reqData.status)) {
        if (reqData.acceptedDonorUid === donorUid) {
          return { alreadyAccepted: true, status: reqData.status };
        }
        throw new HttpsError(
          "aborted",
          `This blood request is no longer open/pending (current status: ${reqData.status}).`
        );
      }

      const allowedGroups = COMPATIBLE_DONORS_BY_RECIPIENT[reqData.bloodGroup] || [reqData.bloodGroup];
      if (!allowedGroups.includes(donorData.bloodGroup)) {
        throw new HttpsError(
          "failed-precondition",
          `Your blood group (${donorData.bloodGroup}) is not medically compatible with the requested group (${reqData.bloodGroup}).`
        );
      }

      const now = Date.now();
      const existingResponders = Array.isArray(reqData.respondedDonorUids)
        ? reqData.respondedDonorUids
        : [];
      const updatedResponders = existingResponders.includes(donorUid)
        ? existingResponders
        : [...existingResponders, donorUid].slice(0, 20);
      const unitsRequired = Number(reqData.unitsRequired || reqData.bagsNeeded || 1);
      const currentFulfilled = Number(reqData.unitsFulfilled || reqData.bagsPledged || 0);
      const newFulfilled = Math.min(currentFulfilled + 1, unitsRequired);
      const newStatus = newFulfilled >= unitsRequired ? "ACCEPTED" : "DONOR_RESPONDED";

      tx.update(reqRef, {
        status: newStatus,
        acceptedDonorUid: donorUid,
        acceptedDonorName: sanitizeString(donorData.name, 100) || "Voluntary Donor",
        respondedDonorUids: updatedResponders,
        unitsFulfilled: newFulfilled,
        updatedAt: now
      });

      if (requesterUid) {
        const notifRef = db
          .collection("users")
          .doc(requesterUid)
          .collection("notifications")
          .doc(`accept_${requestId}_${donorUid}`);

        tx.set(notifRef, {
          id: notifRef.id,
          recipientUid: requesterUid,
          title: "Donor Responded to Your Request",
          body: `${sanitizeString(donorData.name, 80)} (${donorData.bloodGroup}) has committed to donate for ${sanitizeString(reqData.patientName, 80)}.`,
          type: "REQUEST_ACCEPTED",
          referenceId: requestId,
          isRead: false,
          createdAt: now
        });
      }

      return { alreadyAccepted: false, status: newStatus };
    });

    return { success: true, ...result };
  }
);

/**
 * 3. Verify Blood Donation & Award Milestone Badges (Idempotent & Transactional)
 * Restricted strictly to verified organization administrators or system super-admins.
 */
exports.verifyDonation = onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const data = request.data || {};
    const donorUid = sanitizeString(data.donorUid, 128);
    const requestId = sanitizeString(data.requestId, 128);
    const hospitalName = sanitizeString(data.hospitalName, 150);
    const city = sanitizeString(data.city, 60) || "Dhaka";
    const orgId = sanitizeString(data.orgId, 128);
    const unitsDonated = Number.isInteger(data.unitsDonated)
      ? Math.min(Math.max(data.unitsDonated, 1), 5)
      : 1;

    if (!donorUid || !hospitalName) {
      throw new HttpsError("invalid-argument", "donorUid and hospitalName are required.");
    }

    const authorized = await isCallerAuthorizedVerifier(request.auth, orgId);
    if (!authorized) {
      throw new HttpsError(
        "permission-denied",
        "Only verified organization administrators or system admins can verify donations."
      );
    }

    // Deterministic ID prevents duplicate donation verification on retries
    const deterministicKey = requestId
      ? `don_${requestId}_${donorUid}`
      : `don_${donorUid}_${Math.floor(Date.now() / 86400000)}`;

    const donationRef = db.collection("donation_history").doc(deterministicKey);
    const userRef = db.collection("users").doc(donorUid);
    const publicDonorRef = db.collection("public_donors").doc(donorUid);

    const txResult = await db.runTransaction(async (tx) => {
      const [existingDonation, userDoc, publicDonorDoc] = await Promise.all([
        tx.get(donationRef),
        tx.get(userRef),
        tx.get(publicDonorRef)
      ]);

      if (existingDonation.exists) {
        return {
          alreadyVerified: true,
          donationId: existingDonation.id,
          certificateCode: existingDonation.data().certificateCode
        };
      }

      if (!userDoc.exists) {
        throw new HttpsError("not-found", "Donor profile not found.");
      }

      let reqDoc = null;
      let reqRef = null;
      if (requestId) {
        reqRef = db.collection("blood_requests").doc(requestId);
        reqDoc = await tx.get(reqRef);
      }

      let orgName = "RoktoSetu Clinical Verification";
      if (orgId) {
        const orgDoc = await tx.get(db.collection("organizations").doc(orgId));
        if (orgDoc.exists) {
          orgName = sanitizeString(orgDoc.data().name, 120) || orgName;
        }
      }

      const userData = userDoc.data();
      const now = Date.now();
      const newCount = (Number(userData.donationCount) || 0) + 1;

      const badges = new Set(Array.isArray(userData.badges) ? userData.badges : []);
      if (newCount >= 1) badges.add("FIRST_DROP");
      if (newCount >= 5) badges.add("BRONZE_SAVIOR");
      if (newCount >= 10) badges.add("SILVER_GUARDIAN");
      if (newCount >= 25) badges.add("GOLDEN_HERO");
      if (newCount >= 50) badges.add("PLATINUM_LEGEND");

      const certificateCode = `RS-${now.toString(36).toUpperCase()}-${donorUid.slice(0, 5).toUpperCase()}`;

      tx.set(donationRef, {
        id: donationRef.id,
        donorId: donorUid,
        donorUid,
        donorName: sanitizeString(userData.name, 100) || "Donor",
        bloodGroup: userData.bloodGroup || "O_POSITIVE",
        requestId: requestId || null,
        hospitalName,
        city,
        district: city,
        donationDate: now,
        verifiedByRequester: true,
        verifiedByOrgId: orgId || "SYSTEM_ADMIN",
        verifiedByOrgName: orgName,
        verificationStatus: "VERIFIED",
        bags: unitsDonated,
        unitsDonated,
        certificateId: certificateCode,
        certificateCode
      });

      tx.update(userRef, {
        donationCount: newCount,
        lastDonationDate: now,
        donorAvailability: "RESTING",
        verificationStatus: "VERIFIED",
        badges: Array.from(badges),
        updatedAt: now
      });

      if (publicDonorDoc.exists) {
        tx.update(publicDonorRef, {
          donationCount: newCount,
          lastDonationDate: now,
          availability: "RESTING",
          verificationStatus: "VERIFIED",
          updatedAt: now
        });
      }

      if (reqDoc && reqDoc.exists && reqRef) {
        tx.update(reqRef, {
          status: "FULFILLED",
          updatedAt: now
        });
      }

      const auditRef = db.collection("audit_logs").doc();
      tx.set(auditRef, {
        actorUid: request.auth.uid,
        actorEmail: request.auth.token.email || "admin",
        action: "VERIFY_DONATION",
        targetId: donorUid,
        targetType: "USER",
        details: `Verified ${unitsDonated} bag(s) at ${hospitalName}. Total donations: ${newCount}`,
        timestamp: now
      });

      return {
        alreadyVerified: false,
        donationId: donationRef.id,
        newCount,
        certificateCode
      };
    });

    return { success: true, ...txResult };
  }
);

/**
 * 4. Approve Verification Request (Super-Admin Only)
 * Atomically approves or rejects a donor or organization verification request.
 */
exports.reviewVerificationRequest = onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!(await isCallerSuperAdmin(request.auth))) {
      throw new HttpsError("permission-denied", "Only system super-admins can review verification requests.");
    }

    const data = request.data || {};
    const taskId = sanitizeString(data.taskId, 128);
    const decision = sanitizeString(data.decision, 20); // "APPROVED" or "REJECTED"

    if (!taskId || !["APPROVED", "REJECTED"].includes(decision)) {
      throw new HttpsError("invalid-argument", "Valid taskId and decision (APPROVED or REJECTED) are required.");
    }

    const taskRef = db.collection("verification_requests").doc(taskId);
    await db.runTransaction(async (tx) => {
      const taskSnap = await tx.get(taskRef);
      if (!taskSnap.exists) {
        throw new HttpsError("not-found", "Verification request not found.");
      }

      const taskData = taskSnap.data();
      if (taskData.status !== "PENDING") {
        return; // Idempotent no-op if already processed
      }

      const applicantUid = taskData.applicantUid || taskData.userUid || "";
      const now = Date.now();
      tx.update(taskRef, {
        status: decision,
        reviewedByUid: request.auth.uid,
        reviewedAt: now
      });

      if (taskData.type === "ORGANIZATION") {
        const orgRef = db.collection("organizations").doc(applicantUid);
        const orgSnap = await tx.get(orgRef);
        if (orgSnap.exists) {
          tx.update(orgRef, { isVerified: decision === "APPROVED" });
        }
      } else {
        const newStatus = decision === "APPROVED" ? "VERIFIED" : "REJECTED";
        const userRef = db.collection("users").doc(applicantUid);
        const pubRef = db.collection("public_donors").doc(applicantUid);
        const [uSnap, pSnap] = await Promise.all([tx.get(userRef), tx.get(pubRef)]);
        if (uSnap.exists) {
          tx.update(userRef, { verificationStatus: newStatus, updatedAt: now });
        }
        if (pSnap.exists) {
          tx.update(pubRef, { verificationStatus: newStatus, updatedAt: now });
        }
      }

      const auditRef = db.collection("audit_logs").doc();
      tx.set(auditRef, {
        actorUid: request.auth.uid,
        actorEmail: request.auth.token.email || "admin",
        action: `VERIFICATION_${decision}`,
        targetId: applicantUid,
        targetType: taskData.type,
        details: `${decision} verification task ${taskId} for ${sanitizeString(taskData.name, 80)}`,
        timestamp: now
      });
    });

    return { success: true, taskId, decision };
  }
);

/**
 * 5. Scheduled Expiration of Stale Blood Requests
 * Automatically marks PENDING or OPEN blood requests as EXPIRED once their expiresAt timestamp passes.
 */
exports.expireStaleBloodRequests = onSchedule("every 60 minutes", async () => {
  const now = Date.now();
  const staleRequestsSnap = await db
    .collection("blood_requests")
    .where("status", "in", ["PENDING", "OPEN"])
    .where("expiresAt", "<=", now)
    .limit(200)
    .get();

  if (staleRequestsSnap.empty) return;

  const batch = db.batch();
  staleRequestsSnap.docs.forEach((doc) => {
    batch.update(doc.ref, {
      status: "EXPIRED",
      updatedAt: now
    });
  });

  await batch.commit();
});
