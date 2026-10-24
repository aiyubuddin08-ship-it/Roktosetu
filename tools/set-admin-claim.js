#!/usr/bin/env node
/**
 * Server-side Administration Bootstrap Utility for RoktoSetu (রক্তসেতু)
 *
 * Sets the `admin: true` custom claim on a Firebase Auth user by UID and
 * writes a matching `/admins/{uid}` document in the custom Firestore database.
 *
 * Security:
 * - Contains NO hardcoded credentials, emails, or passwords.
 * - Reads service account credentials from `--service-account <path>` or
 *   `GOOGLE_APPLICATION_CREDENTIALS` environment variable.
 *
 * Usage:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccountKey.json \
 *   FIRESTORE_DATABASE_ID=<custom-db-id> \
 *   node tools/set-admin-claim.js --uid <USER_UID> [--revoke]
 */

import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);

function parseArgs(argv) {
  const args = {
    uid: null,
    serviceAccountPath: process.env.GOOGLE_APPLICATION_CREDENTIALS || null,
    databaseId: process.env.FIRESTORE_DATABASE_ID || null,
    revoke: false,
  };

  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--uid' && argv[i + 1]) {
      args.uid = argv[++i];
    } else if (arg === '--service-account' && argv[i + 1]) {
      args.serviceAccountPath = argv[++i];
    } else if (arg === '--db' && argv[i + 1]) {
      args.databaseId = argv[++i];
    } else if (arg === '--revoke') {
      args.revoke = true;
    }
  }
  return args;
}

function resolveDatabaseIdFromXml() {
  try {
    const xmlPath = path.resolve(process.cwd(), 'app/src/main/res/values/firebase_applet_config.xml');
    if (!fs.existsSync(xmlPath)) return '(default)';
    const xml = fs.readFileSync(xmlPath, 'utf8');
    const match = xml.match(/<string\s+name="firestore_database_id"[^>]*>([^<]+)<\/string>/);
    return match ? match[1].trim() : '(default)';
  } catch {
    return '(default)';
  }
}

async function main() {
  const { uid, serviceAccountPath, databaseId, revoke } = parseArgs(process.argv);

  if (!uid || typeof uid !== 'string' || uid.trim().length === 0) {
    console.error('Error: --uid <USER_UID> is required.');
    console.error('Usage: node tools/set-admin-claim.js --uid <USER_UID> [--service-account <path>] [--db <dbId>] [--revoke]');
    process.exit(1);
  }

  if (!serviceAccountPath || !fs.existsSync(serviceAccountPath)) {
    console.error('Error: Service account JSON not found. Set GOOGLE_APPLICATION_CREDENTIALS or pass --service-account <path>.');
    process.exit(1);
  }

  const admin = require('firebase-admin');
  const { getFirestore } = require('firebase-admin/firestore');

  const serviceAccount = JSON.parse(fs.readFileSync(path.resolve(serviceAccountPath), 'utf8'));
  const app = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });

  const targetDbId = databaseId || resolveDatabaseIdFromXml();
  const db = targetDbId && targetDbId !== '(default)'
    ? getFirestore(app, targetDbId)
    : getFirestore(app);

  const cleanUid = uid.trim();
  const userRecord = await admin.auth().getUser(cleanUid);
  const existingClaims = userRecord.customClaims || {};
  const now = Date.now();

  if (revoke) {
    const updatedClaims = { ...existingClaims };
    delete updatedClaims.admin;
    await admin.auth().setCustomUserClaims(cleanUid, updatedClaims);
    await db.collection('admins').doc(cleanUid).delete();
    console.log(`Revoked admin privileges for UID=${cleanUid} in database=${targetDbId}.`);
  } else {
    await admin.auth().setCustomUserClaims(cleanUid, {
      ...existingClaims,
      admin: true,
    });
    await db.collection('admins').doc(cleanUid).set(
      {
        uid: cleanUid,
        email: userRecord.email || '',
        role: 'SUPER_ADMIN',
        updatedAt: now,
      },
      { merge: true }
    );
    await db.collection('users').doc(cleanUid).set(
      {
        role: 'SUPER_ADMIN',
        updatedAt: now,
      },
      { merge: true }
    );
    console.log(`Granted admin:true custom claim and created /admins/${cleanUid} in database=${targetDbId}.`);
  }
}

main().catch((err) => {
  console.error('Failed to update admin claim:', err.message || err);
  process.exit(1);
});
