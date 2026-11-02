/**
 * NoSQL data access (Cloud Firestore). The SQL database keeps accounts, bursaries, institutions and
 * applications; Firestore keeps the flexible, document-shaped data:
 *
 *   users/{userId}                      { academicProfile: {...}, notificationsEnabled, updatedAt }
 *   users/{userId}/devices/{tokenHash}  { token, platform, createdAt, updatedAt }       <- FCM device registration
 *   users/{userId}/notifications/{id}   { title, body, type, data, read, createdAt }    <- notification inbox
 *   alerts/{key}                        { createdAt }  (stops the same deadline alert being sent twice a day)
 *
 * Sub-collections are used so every query is single-field and needs no composite index.
 * Timestamps are ISO-8601 strings so the app and the tests can read them without Firestore types.
 */
const crypto = require('crypto');
const firebase = require('../config/firebase');

const nowIso = () => new Date().toISOString();
const userDoc = (userId) => firebase.getFirestore().collection('users').doc(String(userId));
const tokenId = (token) => crypto.createHash('sha256').update(token).digest('hex').slice(0, 40);

// ---------- user document: academic profile + notification preference ----------

const EMPTY_PROFILE = {
  grade: null,
  yearOfStudy: null,
  subjects: [],
  apsScore: null,
  province: null,
  fieldOfStudy: null,
};

async function getAcademicProfile(userId) {
  const snap = await userDoc(userId).get();
  const stored = snap.exists ? snap.data().academicProfile : null;
  return { ...EMPTY_PROFILE, ...(stored || {}) };
}

async function saveAcademicProfile(userId, profile) {
  const existing = await getAcademicProfile(userId);
  const merged = { ...existing, ...profile };
  await userDoc(userId).set({ academicProfile: merged, updatedAt: nowIso() }, { merge: true });
  return merged;
}

async function setNotificationsEnabled(userId, enabled) {
  await userDoc(userId).set({ notificationsEnabled: Boolean(enabled), updatedAt: nowIso() }, { merge: true });
}

/** Defaults to true: a user who never changed the setting wants alerts. */
async function getNotificationsEnabled(userId) {
  const snap = await userDoc(userId).get();
  if (!snap.exists) return true;
  return snap.data().notificationsEnabled !== false;
}

// ---------- device registration (FCM tokens) ----------

async function saveDeviceToken(userId, token, platform = 'android') {
  const ref = userDoc(userId).collection('devices').doc(tokenId(token));
  const existing = await ref.get();
  await ref.set(
    {
      token,
      platform,
      createdAt: existing.exists ? existing.data().createdAt : nowIso(),
      updatedAt: nowIso(),
    },
    { merge: true }
  );
}

async function removeDeviceToken(userId, token) {
  await userDoc(userId).collection('devices').doc(tokenId(token)).delete();
}

/** All FCM tokens registered by this user (one per phone). */
async function getDeviceTokens(userId) {
  const snap = await userDoc(userId).collection('devices').get();
  return snap.docs.map((d) => d.data().token).filter(Boolean);
}

/** Every registered device of every user, as { userId, token }. Used for broadcasts. */
async function getAllDevices() {
  const snap = await firebase.getFirestore().collectionGroup('devices').get();
  return snap.docs
    .map((d) => ({ userId: d.ref.parent.parent.id, token: d.data().token }))
    .filter((d) => d.token);
}

// ---------- notification inbox ----------

async function saveNotification(userId, { title, body, type = 'general', data = {} }) {
  const ref = userDoc(userId).collection('notifications').doc();
  const doc = { title, body, type, data, read: false, createdAt: nowIso() };
  await ref.set(doc);
  return { id: ref.id, ...doc };
}

async function listNotifications(userId, limit = 50) {
  const snap = await userDoc(userId)
    .collection('notifications')
    .orderBy('createdAt', 'desc')
    .limit(limit)
    .get();
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
}

// ---------- de-duplication for scheduled alerts ----------

/** Returns true the first time a key is claimed, false if it was already used (create() fails). */
async function claimAlert(key) {
  try {
    await firebase.getFirestore().collection('alerts').doc(key).create({ createdAt: nowIso() });
    return true;
  } catch (err) {
    if (err.code === 6 || /already exists/i.test(err.message || '')) return false;
    throw err;
  }
}

module.exports = {
  EMPTY_PROFILE,
  getAcademicProfile,
  saveAcademicProfile,
  setNotificationsEnabled,
  getNotificationsEnabled,
  saveDeviceToken,
  removeDeviceToken,
  getDeviceTokens,
  getAllDevices,
  saveNotification,
  listNotifications,
  claimAlert,
};
