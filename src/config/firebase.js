/**
 * Firebase Admin SDK setup. One Firebase project gives the API two things:
 *   - Cloud Firestore  -> the NoSQL database (device tokens, notification inbox, academic profile)
 *   - Cloud Messaging  -> push notifications (FCM) sent to the Android app
 *
 * The service-account key is NEVER committed. Provide it with ONE of these environment variables:
 *   FIREBASE_SERVICE_ACCOUNT_PATH  path to the downloaded JSON key file (local development)
 *   FIREBASE_SERVICE_ACCOUNT_JSON  the JSON itself, or the JSON base64-encoded (Azure App Service etc.)
 *
 * Reference: Google (2025) *Add the Firebase Admin SDK to your server*
 * https://firebase.google.com/docs/admin/setup
 */
const fs = require('fs');
const path = require('path');

let app = null;
let doubles = null; // test doubles injected by the unit tests

function loadServiceAccount() {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (raw && raw.trim()) {
    const text = raw.trim().startsWith('{') ? raw : Buffer.from(raw, 'base64').toString('utf8');
    return JSON.parse(text);
  }
  const file = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (file && file.trim()) {
    return JSON.parse(fs.readFileSync(path.resolve(file), 'utf8'));
  }
  return null;
}

/** True when a service account has been supplied (or a test double is installed). */
function isConfigured() {
  if (doubles) return true;
  return Boolean(
    (process.env.FIREBASE_SERVICE_ACCOUNT_JSON && process.env.FIREBASE_SERVICE_ACCOUNT_JSON.trim()) ||
      (process.env.FIREBASE_SERVICE_ACCOUNT_PATH && process.env.FIREBASE_SERVICE_ACCOUNT_PATH.trim())
  );
}

function getApp() {
  if (app) return app;
  const serviceAccount = loadServiceAccount();
  if (!serviceAccount) {
    const err = new Error('Firebase is not configured (set FIREBASE_SERVICE_ACCOUNT_PATH or FIREBASE_SERVICE_ACCOUNT_JSON)');
    err.status = 503;
    throw err;
  }
  const { initializeApp, cert } = require('firebase-admin/app');
  app = initializeApp({ credential: cert(serviceAccount) });
  console.log('[firebase] Admin SDK initialised for project', serviceAccount.project_id);
  return app;
}

/** Firestore instance (NoSQL database). */
function getFirestore() {
  if (doubles) return doubles.firestore;
  return require('firebase-admin/firestore').getFirestore(getApp());
}

/** Firebase Cloud Messaging instance (push notifications). */
function getMessaging() {
  if (doubles) return doubles.messaging;
  return require('firebase-admin/messaging').getMessaging(getApp());
}

/** Unit tests install in-memory fakes here; pass null to remove them. */
function setTestDoubles(value) {
  doubles = value;
}

module.exports = { isConfigured, getFirestore, getMessaging, setTestDoubles };
