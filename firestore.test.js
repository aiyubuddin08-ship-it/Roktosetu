const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const { test, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || 'gen-lang-client-0314627405';
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085').split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync('firestore.rules', 'utf8'),
      host: emulatorHost,
      port: emulatorPort
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test('Users collection: owner can create valid UNVERIFIED USER profile, cannot self-escalate to SUPER_ADMIN or VERIFIED', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();

  const validProfile = {
    uid: 'alice',
    name: 'Alice Rahman',
    email: 'alice@example.com',
    bloodGroup: 'A_POSITIVE',
    isDonor: true,
    donorAvailability: 'AVAILABLE',
    isEmergencyDonor: false,
    lastDonationDate: null,
    donationCount: 0,
    contactPhone: '01711223344',
    showPhonePublicly: false,
    location: {
      division: 'Dhaka',
      district: 'Dhaka',
      upazila: 'Dhanmondi',
      city: 'Dhaka',
      area: 'Dhanmondi'
    },
    verificationStatus: 'UNVERIFIED',
    role: 'USER',
    fcmToken: '',
    photoUrl: '',
    dateOfBirth: '01/01/1998',
    accountStatus: 'ACTIVE',
    emailVerified: true,
    profileCompleted: true,
    createdAt: Date.now(),
    updatedAt: Date.now()
  };

  await assertSucceeds(aliceDb.doc('users/alice').set(validProfile));

  // Bob cannot read Alice's private /users/alice profile (PII protection)
  await assertFails(bobDb.doc('users/alice').get());

  // Alice cannot self-escalate role to SUPER_ADMIN or verificationStatus to VERIFIED
  await assertFails(aliceDb.doc('users/alice').update({ role: 'SUPER_ADMIN' }));
  await assertFails(aliceDb.doc('users/alice').update({ verificationStatus: 'VERIFIED' }));

  // Alice CAN update her own availability and location
  await assertSucceeds(aliceDb.doc('users/alice').update({ donorAvailability: 'RESTING' }));
});

test('Public Donors collection: authenticated users can read, only owner can write sanitized donor view', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();

  const validPublicDonor = {
    uid: 'alice',
    name: 'Alice Rahman',
    bloodGroup: 'A_POSITIVE',
    availability: 'AVAILABLE',
    isDonor: true,
    isEmergencyDonor: false,
    division: 'Dhaka',
    district: 'Dhaka',
    upazila: 'Dhanmondi',
    city: 'Dhaka',
    area: 'Dhanmondi',
    generalLocation: 'Dhanmondi, Dhaka (Dhaka Div)',
    verificationStatus: 'UNVERIFIED',
    donationCount: 0,
    lastDonationDate: null,
    contactPhone: ''
  };

  await assertSucceeds(aliceDb.doc('public_donors/alice').set(validPublicDonor));
  await assertSucceeds(bobDb.doc('public_donors/alice').get());
  await assertFails(guestDb.doc('public_donors/alice').get());
  await assertFails(bobDb.doc('public_donors/alice').set(validPublicDonor));
});

test('Blood Requests: valid creation, donor acceptance transition, and requester fulfillment transition', async () => {
  const requesterDb = testEnv.authenticatedContext('requester1').firestore();
  const donorDb = testEnv.authenticatedContext('donor1').firestore();
  const intruderDb = testEnv.authenticatedContext('intruder').firestore();

  const validRequest = {
    id: 'req_101',
    requesterUid: 'requester1',
    requesterName: 'Karim Hossain',
    patientName: 'Rahim Hossain',
    patientAge: 42,
    bloodGroup: 'O_POSITIVE',
    unitsRequired: 2,
    hospitalName: 'Dhaka Medical College Hospital',
    hospitalAddress: 'Bakshibazar',
    division: 'Dhaka',
    district: 'Dhaka',
    upazila: 'Shahbagh',
    area: 'Bakshibazar',
    city: 'Dhaka',
    urgency: 'CRITICAL_EMERGENCY',
    status: 'PENDING',
    contactPhone: '01712345678',
    medicalReason: 'Emergency Surgery',
    acceptedDonorUid: null,
    acceptedDonorName: null,
    createdAt: Date.now(),
    expiresAt: Date.now() + 86400000,
    isEmergencyBroadcast: true
  };

  await assertSucceeds(requesterDb.doc('blood_requests/req_101').set(validRequest));

  // Requester cannot mark PENDING request directly as FULFILLED before a donor accepts
  await assertFails(requesterDb.doc('blood_requests/req_101').update({ status: 'FULFILLED' }));

  // Donor can accept PENDING request (transition to DONOR_RESPONDED)
  await assertSucceeds(donorDb.doc('blood_requests/req_101').update({
    status: 'DONOR_RESPONDED',
    acceptedDonorUid: 'donor1',
    acceptedDonorName: 'Donor One'
  }));

  // Intruder cannot hijack an already responded request
  await assertFails(intruderDb.doc('blood_requests/req_101').update({
    status: 'DONOR_RESPONDED',
    acceptedDonorUid: 'intruder',
    acceptedDonorName: 'Intruder'
  }));

  // Now requester CAN mark DONOR_RESPONDED request as FULFILLED
  await assertSucceeds(requesterDb.doc('blood_requests/req_101').update({
    status: 'FULFILLED'
  }));
});

test('Conversations: only participants can read/update and participant list is immutable', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const eveDb = testEnv.authenticatedContext('eve').firestore();

  const convData = {
    id: 'alice_bob',
    participants: ['alice', 'bob'],
    participantNames: { alice: 'Alice', bob: 'Bob' },
    participantBloodGroups: { alice: 'A+', bob: 'O+' },
    lastMessage: 'Conversation started',
    lastMessageTimestamp: Date.now(),
    requestId: null
  };

  await assertSucceeds(aliceDb.doc('conversations/alice_bob').set(convData));

  // Eve (non-participant) cannot read the conversation
  await assertFails(eveDb.doc('conversations/alice_bob').get());

  // Bob (participant) can read and update lastMessage, but CANNOT modify participants list
  await assertSucceeds(bobDb.doc('conversations/alice_bob').get());
  await assertFails(bobDb.doc('conversations/alice_bob').update({
    participants: ['alice', 'bob', 'eve']
  }));
});

test('Admins collection: client create, update, and delete are strictly forbidden; only owner or admin can get', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const claimAdminDb = testEnv.authenticatedContext('super_admin', { admin: true }).firestore();

  // Client cannot self-create an /admins/{uid} document
  await assertFails(aliceDb.doc('admins/alice').set({ uid: 'alice', role: 'SUPER_ADMIN' }));

  // Seed /admins/alice via server/admin context
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().doc('admins/alice').set({ uid: 'alice', role: 'SUPER_ADMIN' });
  });

  // Owner (alice) and custom-claim admin can get /admins/alice, but Bob cannot
  await assertSucceeds(aliceDb.doc('admins/alice').get());
  await assertSucceeds(claimAdminDb.doc('admins/alice').get());
  await assertFails(bobDb.doc('admins/alice').get());

  // Even owner or claim admin cannot update or delete /admins/alice from client SDK
  await assertFails(aliceDb.doc('admins/alice').update({ role: 'USER' }));
  await assertFails(claimAdminDb.doc('admins/alice').update({ role: 'USER' }));
  await assertFails(aliceDb.doc('admins/alice').delete());
});

test('Verification Requests: user can submit PENDING request (DONOR/ORGANIZATION) and admin can review with reviewedByUid/reviewedAt', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const adminDb = testEnv.authenticatedContext('admin_user', { admin: true }).firestore();

  const validVerif = {
    id: 'verif_alice_1',
    applicantUid: 'alice',
    name: 'Alice Rahman',
    type: 'DONOR',
    documents: ['NID Card: 1234567890'],
    status: 'PENDING',
    submittedAt: Date.now()
  };

  await assertSucceeds(aliceDb.doc('verification_requests/verif_alice_1').set(validVerif));

  // Bob cannot read or approve Alice's verification request
  await assertFails(bobDb.doc('verification_requests/verif_alice_1').get());
  await assertFails(bobDb.doc('verification_requests/verif_alice_1').update({ status: 'VERIFIED' }));

  // Admin can read and approve with reviewedByUid and reviewedAt
  await assertSucceeds(adminDb.doc('verification_requests/verif_alice_1').get());
  await assertSucceeds(adminDb.doc('verification_requests/verif_alice_1').update({
    status: 'APPROVED',
    reviewedByUid: 'admin_user',
    reviewedAt: Date.now()
  }));
});

test('System Config & Audit Logs: admins can toggle emergency_broadcast and append immutable audit_logs; users can only read system_config', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const adminDb = testEnv.authenticatedContext('admin_user', { admin: true }).firestore();

  // Regular user cannot write to system_config or audit_logs
  await assertFails(aliceDb.doc('system_config/emergency_broadcast').set({ armed: false }));
  await assertFails(aliceDb.doc('audit_logs/log_1').set({ action: 'FAKE_ACTION', timestamp: Date.now() }));

  // Admin can write system_config and append audit_logs
  await assertSucceeds(adminDb.doc('system_config/emergency_broadcast').set({
    armed: false,
    updatedByUid: 'admin_user',
    updatedAt: Date.now()
  }));
  await assertSucceeds(adminDb.doc('audit_logs/log_1').set({
    actorUid: 'admin_user',
    actorEmail: 'admin@roktosetu.org',
    action: 'EMERGENCY_BROADCAST_PAUSED',
    targetId: 'emergency_broadcast',
    targetType: 'SYSTEM_CONFIG',
    details: 'Emergency broadcast engine paused',
    timestamp: Date.now()
  }));

  // Regular signed-in user can read system_config/emergency_broadcast, but cannot read audit_logs
  await assertSucceeds(aliceDb.doc('system_config/emergency_broadcast').get());
  await assertFails(aliceDb.doc('audit_logs/log_1').get());

  // Audit logs are strictly immutable: even admin cannot update or delete an existing audit log
  await assertFails(adminDb.doc('audit_logs/log_1').update({ details: 'Tampered' }));
  await assertFails(adminDb.doc('audit_logs/log_1').delete());
});

test('Users & Campaigns: owner can transition verificationStatus to PENDING (not VERIFIED) and publish own campaign', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();

  const validProfile = {
    uid: 'alice',
    name: 'Alice Rahman',
    email: 'alice@example.com',
    bloodGroup: 'A_POSITIVE',
    isDonor: true,
    donorAvailability: 'AVAILABLE',
    isEmergencyDonor: false,
    lastDonationDate: null,
    donationCount: 0,
    contactPhone: '01711223344',
    showPhonePublicly: false,
    location: {
      division: 'Dhaka',
      district: 'Dhaka',
      upazila: 'Dhanmondi',
      city: 'Dhaka',
      area: 'Dhanmondi'
    },
    verificationStatus: 'UNVERIFIED',
    role: 'USER',
    fcmToken: '',
    photoUrl: '',
    dateOfBirth: '01/01/1998',
    accountStatus: 'ACTIVE',
    emailVerified: true,
    profileCompleted: true,
    createdAt: Date.now(),
    updatedAt: Date.now()
  };

  await assertSucceeds(aliceDb.doc('users/alice').set(validProfile));
  await assertSucceeds(aliceDb.doc('users/alice').update({ verificationStatus: 'PENDING' }));
  await assertFails(aliceDb.doc('users/alice').update({ verificationStatus: 'VERIFIED' }));

  const validCampaign = {
    id: 'camp_alice_1',
    organizationId: 'alice',
    organizationName: 'Alice Community Drive',
    title: 'Dhanmondi Voluntary Blood Camp',
    description: 'Annual community blood donation camp',
    location: 'Rabindra Sarobar, Dhanmondi',
    city: 'Dhaka',
    dateTimestamp: Date.now() + 86400000,
    targetBags: 100,
    collectedBags: 0,
    isActive: true
  };

  await assertSucceeds(aliceDb.doc('campaigns/camp_alice_1').set(validCampaign));
});

test('Reports collection: user can create valid report against others, cannot self-report or use invalid details/category, and can only read own reports while admin reads/updates all', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const adminDb = testEnv.authenticatedContext('admin_user', { admin: true }).firestore();

  const validReport = {
    id: 'rep_1',
    reporterUid: 'alice',
    reporterEmail: 'alice@example.com',
    targetId: 'bob',
    targetType: 'USER',
    category: 'HARASSMENT',
    details: 'এই ব্যবহারকারী চ্যাটে অসদাচরণ করেছেন এবং ভুল তথ্য দিয়েছেন।',
    status: 'OPEN',
    createdAt: Date.now()
  };

  // Valid report succeeds
  await assertSucceeds(aliceDb.doc('reports/rep_1').set(validReport));

  // Self-report is forbidden
  await assertFails(aliceDb.doc('reports/rep_self').set({
    ...validReport,
    id: 'rep_self',
    targetId: 'alice'
  }));

  // Short details (< 10 chars) is forbidden
  await assertFails(aliceDb.doc('reports/rep_short').set({
    ...validReport,
    id: 'rep_short',
    details: 'Too short'
  }));

  // Invalid category is forbidden
  await assertFails(aliceDb.doc('reports/rep_badcat').set({
    ...validReport,
    id: 'rep_badcat',
    category: 'UNKNOWN_CATEGORY'
  }));

  // Reporter can read own report, Bob cannot read Alice's report, Admin can read and resolve
  await assertSucceeds(aliceDb.doc('reports/rep_1').get());
  await assertFails(bobDb.doc('reports/rep_1').get());
  await assertSucceeds(adminDb.doc('reports/rep_1').get());
  await assertSucceeds(adminDb.doc('reports/rep_1').update({
    status: 'RESOLVED',
    resolvedByUid: 'admin_user',
    resolvedAt: Date.now()
  }));
});

test('Blood Banks, Ambulances, Blood Camps & RSVPs: signed-in users can read, only admins can write directories, and users can only manage their own RSVP', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const adminDb = testEnv.authenticatedContext('admin_user', { admin: true }).firestore();

  const bankData = {
    id: 'bb-1',
    name: 'ঢাকা মেডিকেল কলেজ ব্লাড ব্যাংক',
    division: 'Dhaka',
    district: 'Dhaka',
    city: 'Dhaka',
    address: 'বকশীবাজার, ঢাকা',
    contactPhone: '01711223344',
    isOpen24Hours: true,
    availableGroups: ['A_POSITIVE', 'O_POSITIVE'],
    outOfStockGroups: ['AB_NEGATIVE'],
    isVerified: true,
    isActive: true,
    updatedAt: Date.now()
  };

  const ambulanceData = {
    id: 'amb-1',
    serviceName: 'রেড ক্রিসেন্ট অ্যাম্বুলেন্স সার্ভিস',
    vehicleType: 'ICU',
    division: 'Dhaka',
    district: 'Dhaka',
    city: 'Dhaka',
    area: 'মগবাজার',
    driverPhone: '01811223344',
    isAvailableNow: true,
    fareType: 'স্বল্পমূল্য',
    isActive: true,
    updatedAt: Date.now()
  };

  const campData = {
    id: 'camp-1',
    title: 'ঢাকা বিশ্ববিদ্যালয় স্বেচ্ছায় রক্তদান ক্যাম্প',
    organizer: 'বাঁধন',
    division: 'Dhaka',
    district: 'Dhaka',
    venue: 'টিএসসি প্রাঙ্গণ, ঢাকা বিশ্ববিদ্যালয়',
    city: 'Dhaka',
    dateFormatted: '২৫ মে ২০২৬',
    timeSlot: 'সকাল ৯:০০ - বিকাল ৪:০০',
    targetDonors: 200,
    registeredCount: 45,
    contactPhone: '01911223344',
    isActive: true,
    updatedAt: Date.now()
  };

  // Non-admin cannot create blood bank, ambulance, or camp
  await assertFails(aliceDb.doc('blood_banks/bb-1').set(bankData));
  await assertFails(aliceDb.doc('ambulances/amb-1').set(ambulanceData));
  await assertFails(aliceDb.doc('blood_camps/camp-1').set(campData));

  // Admin can create all three
  await assertSucceeds(adminDb.doc('blood_banks/bb-1').set(bankData));
  await assertSucceeds(adminDb.doc('ambulances/amb-1').set(ambulanceData));
  await assertSucceeds(adminDb.doc('blood_camps/camp-1').set(campData));

  // Signed-in user can read all three
  await assertSucceeds(aliceDb.doc('blood_banks/bb-1').get());
  await assertSucceeds(aliceDb.doc('ambulances/amb-1').get());
  await assertSucceeds(aliceDb.doc('blood_camps/camp-1').get());

  // User cannot directly modify registeredCount on blood_camps
  await assertFails(aliceDb.doc('blood_camps/camp-1').update({ registeredCount: 999 }));

  // Alice can create and delete her own RSVP, but cannot create or delete Bob's RSVP
  await assertSucceeds(aliceDb.doc('blood_camps/camp-1/rsvps/alice').set({
    uid: 'alice',
    campId: 'camp-1',
    userName: 'Alice Rahman',
    createdAt: Date.now()
  }));
  await assertFails(aliceDb.doc('blood_camps/camp-1/rsvps/bob').set({
    uid: 'bob',
    campId: 'camp-1',
    userName: 'Bob',
    createdAt: Date.now()
  }));
  await assertFails(bobDb.doc('blood_camps/camp-1/rsvps/alice').delete());
  await assertSucceeds(aliceDb.doc('blood_camps/camp-1/rsvps/alice').delete());
});

test('10. Announcements and Security Alerts: Admin full access, users read published announcements only', async () => {
  const adminDb = testEnv.authenticatedContext('admin_user', { admin: true }).firestore();
  const aliceDb = testEnv.authenticatedContext('alice').firestore();

  const publishedAnn = {
    titleBn: 'জরুরি রক্তদান ক্যাম্প বিজ্ঞপ্তি',
    bodyBn: 'আগামীকাল ঢাকা মেডিকেল কলেজে স্বেচ্ছায় রক্তদান কর্মসূচি অনুষ্ঠিত হবে।',
    category: 'GENERAL',
    targetAudience: 'ALL_USERS',
    status: 'PUBLISHED',
    createdByUid: 'admin_user',
    createdAt: Date.now()
  };

  const draftAnn = {
    ...publishedAnn,
    status: 'DRAFT'
  };

  // Non-admin cannot create announcements
  await assertFails(aliceDb.doc('announcements/ann-1').set(publishedAnn));

  // Admin can create published and draft announcements
  await assertSucceeds(adminDb.doc('announcements/ann-pub').set(publishedAnn));
  await assertSucceeds(adminDb.doc('announcements/ann-draft').set(draftAnn));

  // Signed-in user can read PUBLISHED announcement, but NOT DRAFT announcement
  await assertSucceeds(aliceDb.doc('announcements/ann-pub').get());
  await assertFails(aliceDb.doc('announcements/ann-draft').get());
  await assertSucceeds(adminDb.doc('announcements/ann-draft').get());

  // Security alerts: admin-only read/write
  const alertDoc = {
    alertType: 'SECURITY_EVENT',
    severity: 'MEDIUM',
    detectedAt: Date.now(),
    reviewStatus: 'OPEN'
  };
  await assertFails(aliceDb.doc('security_alerts/alert-1').set(alertDoc));
  await assertSucceeds(adminDb.doc('security_alerts/alert-1').set(alertDoc));
  await assertFails(aliceDb.doc('security_alerts/alert-1').get());
  await assertSucceeds(adminDb.doc('security_alerts/alert-1').get());
});


