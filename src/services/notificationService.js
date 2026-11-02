/**
 * Push notifications. Every notification is (1) saved in the user's Firestore inbox and
 * (2) sent to each of the user's registered phones through Firebase Cloud Messaging (FCM).
 * Phones whose token FCM reports as no longer valid are removed automatically.
 *
 * Reference: Google (2025) *Send messages to multiple devices*
 * https://firebase.google.com/docs/cloud-messaging/send/admin-sdk
 */
const firebase = require('../config/firebase');
const store = require('./firestoreStore');
const bursaryStore = require('./bursaryStore');

const CHANNEL_ID = 'varsivo_alerts'; // must match the notification channel created in the Android app
const MAX_TOKENS_PER_REQUEST = 500; // FCM multicast limit
const INVALID_TOKEN_CODES = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
]);

/** FCM "data" values must all be strings. */
function toStringMap(data = {}) {
  const out = {};
  for (const [key, value] of Object.entries(data)) {
    if (value !== undefined && value !== null) out[key] = String(value);
  }
  return out;
}

/** Sends one message to many tokens; returns counts and the tokens FCM says are dead. */
async function pushToTokens(tokens, { title, body, data }) {
  const messaging = firebase.getMessaging();
  let sent = 0;
  let failed = 0;
  const invalidTokens = [];

  for (let i = 0; i < tokens.length; i += MAX_TOKENS_PER_REQUEST) {
    const chunk = tokens.slice(i, i + MAX_TOKENS_PER_REQUEST);
    const response = await messaging.sendEachForMulticast({
      tokens: chunk,
      notification: { title, body },
      data: toStringMap(data),
      android: { priority: 'high', notification: { channelId: CHANNEL_ID } },
    });
    response.responses.forEach((r, index) => {
      if (r.success) {
        sent += 1;
      } else {
        failed += 1;
        const code = r.error && r.error.code;
        console.warn('[push] FCM send failed:', code);
        if (INVALID_TOKEN_CODES.has(code)) invalidTokens.push(chunk[index]);
      }
    });
  }
  return { sent, failed, invalidTokens };
}

/** Saves to the inbox and pushes to all of one user's phones. */
async function sendToUser(userId, { title, body, type = 'general', data = {} }) {
  if (!(await store.getNotificationsEnabled(userId))) {
    console.log(`[push] user ${userId} has notifications turned off, skipping`);
    return { skipped: 'notifications_disabled', sent: 0, failed: 0, devices: 0 };
  }

  const inbox = await store.saveNotification(userId, { title, body, type, data });
  const tokens = await store.getDeviceTokens(userId);
  if (tokens.length === 0) {
    console.log(`[push] user ${userId} has no registered device; saved to inbox only`);
    return { inboxId: inbox.id, devices: 0, sent: 0, failed: 0 };
  }

  const result = await pushToTokens(tokens, { title, body, data: { ...data, type, notificationId: inbox.id } });
  for (const token of result.invalidTokens) await store.removeDeviceToken(userId, token);
  console.log(`[push] user ${userId}: sent ${result.sent}/${tokens.length}`);
  return { inboxId: inbox.id, devices: tokens.length, sent: result.sent, failed: result.failed };
}

/** Sends the same message to every registered phone (skips users who turned notifications off). */
async function broadcast({ title, body, type = 'general', data = {} }) {
  const devices = await store.getAllDevices();
  const byUser = new Map();
  for (const d of devices) {
    if (!byUser.has(d.userId)) byUser.set(d.userId, []);
    byUser.get(d.userId).push(d.token);
  }

  let users = 0;
  let sent = 0;
  let failed = 0;
  for (const [userId, tokens] of byUser) {
    if (!(await store.getNotificationsEnabled(userId))) continue;
    const inbox = await store.saveNotification(userId, { title, body, type, data });
    const result = await pushToTokens(tokens, { title, body, data: { ...data, type, notificationId: inbox.id } });
    for (const token of result.invalidTokens) await store.removeDeviceToken(userId, token);
    users += 1;
    sent += result.sent;
    failed += result.failed;
  }
  console.log(`[push] broadcast "${title}": ${users} user(s), ${sent} sent, ${failed} failed`);
  return { users, sent, failed };
}

// ---------- alerts the API raises by itself ----------

/** Never throws: a failed push must not break the request that triggered it. */
async function safely(label, work) {
  try {
    if (!firebase.isConfigured()) return null;
    return await work();
  } catch (err) {
    console.error(`[push] ${label} failed:`, err.message);
    return null;
  }
}

function notifyApplicationCreated(userId, application) {
  return safely('application-created alert', () =>
    sendToUser(userId, {
      title: 'Application saved',
      body: `Your application is now ${application.status || 'PENDING'}. We will keep you updated.`,
      type: 'application',
      data: { applicationId: application.id, status: application.status },
    })
  );
}

function notifyApplicationStatus(userId, application) {
  return safely('application-status alert', () =>
    sendToUser(userId, {
      title: 'Application update',
      body: `Your application status changed to ${application.status}.`,
      type: 'application',
      data: { applicationId: application.id, status: application.status },
    })
  );
}

// ---------- bursary deadline alerts ----------

const CLOSING_DATE_FIELDS = ['closing_date', 'closingDate', 'deadline', 'close_date', 'end_date'];
const DAY_MS = 24 * 60 * 60 * 1000;

function readClosingDate(row) {
  for (const field of CLOSING_DATE_FIELDS) {
    if (row[field]) {
      const date = new Date(row[field]);
      if (!Number.isNaN(date.getTime())) return date;
    }
  }
  return null;
}

/**
 * Looks at every bursary closing within `days` days and broadcasts one alert per bursary per day.
 * (The closing-date column name is read from CLOSING_DATE_FIELDS - add yours if it is different.)
 */
async function runDeadlineAlerts({ days = 7, now = new Date() } = {}) {
  const bursaries = await bursaryStore.getAll();
  const today = now.toISOString().slice(0, 10);
  const alerts = [];

  for (const row of bursaries) {
    const closing = readClosingDate(row);
    if (!closing) continue;
    const daysLeft = Math.ceil((closing.getTime() - now.getTime()) / DAY_MS);
    if (daysLeft < 0 || daysLeft > days) continue;

    const name = row.name || row.title || 'A bursary';
    const key = `bursary-${row.id ?? name}-${today}`.replace(/[\/\s]/g, '_');
    if (!(await store.claimAlert(key))) continue; // already sent today

    const when = daysLeft === 0 ? 'today' : daysLeft === 1 ? 'tomorrow' : `in ${daysLeft} days`;
    const result = await broadcast({
      title: 'Bursary closing soon',
      body: `${name} closes ${when}. Apply now!`,
      type: 'deadline',
      data: { bursaryId: row.id, daysLeft },
    });
    alerts.push({ bursary: name, daysLeft, ...result });
  }
  return { checked: bursaries.length, alerts };
}

module.exports = {
  sendToUser,
  broadcast,
  notifyApplicationCreated,
  notifyApplicationStatus,
  runDeadlineAlerts,
  toStringMap,
  CHANNEL_ID,
};
