/**
 * End-to-end API tests. The real Express app (routes, validation, JWT auth, controllers and
 * services) runs on a random port; only the three outside systems are replaced with in-memory
 * fakes: the SQL stores, Firestore and Firebase Cloud Messaging.
 *
 * Run with:  npm test
 */
process.env.JWT_SECRET = 'test-secret';
process.env.ADMIN_API_KEY = 'admin-test-key';
process.env.RATE_LIMIT_MAX = '100000';
delete process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
delete process.env.FIREBASE_SERVICE_ACCOUNT_PATH;

const test = require('node:test');
const assert = require('node:assert/strict');
const { FakeFirestore, FakeMessaging } = require('./fakes');

const firebase = require('../src/config/firebase');
const userStore = require('../src/services/userStore');
const applicationStore = require('../src/services/applicationStore');
const bursaryStore = require('../src/services/bursaryStore');
const app = require('../src/app');

// ---------- fake SQL stores ----------
let users;
let applications;
let nextUserId;
let nextAppId;

function resetSql() {
  users = [];
  applications = [];
  nextUserId = 1;
  nextAppId = 1;
}

userStore.findByEmail = async (email) => users.find((u) => u.email === email.toLowerCase()) || null;
userStore.findById = async (uid) => users.find((u) => String(u.id) === String(uid)) || null;
userStore.save = async (user) => {
  const row = {
    id: nextUserId++, email: user.email, password_hash: user.passwordHash, full_name: user.fullName,
    school_name: user.schoolName, student_id: user.studentId, language_pref: user.preferredLanguage,
    role: user.role, notifications_enabled: null, created_at: new Date(),
  };
  users.push(row);
  return userStore.mapRowToUser(row);
};
userStore.updateSettings = async (uid, u) => {
  const row = users.find((x) => String(x.id) === String(uid));
  if (!row) return null;
  if (u.preferredLanguage !== undefined) row.language_pref = u.preferredLanguage;
  if (u.notificationsEnabled !== undefined) row.notifications_enabled = u.notificationsEnabled;
  if (u.fullName !== undefined) row.full_name = u.fullName;
  if (u.schoolName !== undefined) row.school_name = u.schoolName;
  return row;
};
userStore.updatePassword = async (uid, hash) => {
  const row = users.find((x) => String(x.id) === String(uid));
  if (!row) return false;
  row.password_hash = hash;
  return true;
};
applicationStore.getAllForUser = async (uid) => applications.filter((a) => String(a.user_id) === String(uid));
applicationStore.create = async (uid, institutionId, status) => {
  const row = { id: nextAppId++, user_id: Number(uid), institution_id: institutionId, status };
  applications.push(row);
  return row;
};
applicationStore.updateStatus = async (uid, id, status) => {
  const row = applications.find((a) => a.id === id && String(a.user_id) === String(uid));
  if (!row) return null;
  row.status = status;
  return row;
};
applicationStore.remove = async (uid, id) => {
  const i = applications.findIndex((a) => a.id === id && String(a.user_id) === String(uid));
  if (i < 0) return false;
  applications.splice(i, 1);
  return true;
};

// ---------- test harness ----------
let server;
let base;
let db;
let messaging;

test.before(async () => {
  await new Promise((resolve) => { server = app.listen(0, '127.0.0.1', resolve); });
  base = `http://127.0.0.1:${server.address().port}`;
});

test.after(() => new Promise((resolve) => server.close(resolve)));

test.beforeEach(() => {
  resetSql();
  db = new FakeFirestore();
  messaging = new FakeMessaging();
  firebase.setTestDoubles({ firestore: db, messaging });
});

async function call(method, path, { body, token, headers = {} } = {}) {
  const res = await fetch(base + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let json = null;
  try { json = await res.json(); } catch { /* empty body */ }
  return { status: res.status, body: json };
}

async function registerUser(email = 'thabo@example.com', password = 'Password1') {
  const res = await call('POST', '/api/auth/register', { body: { email, password, fullName: 'Thabo Mokoena', schoolName: 'Rosebank High' } });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return { token: res.body.token, uid: res.body.user.uid, email, password };
}

const FCM_TOKEN = 'fcm-token-' + 'a'.repeat(40);
async function waitFor(check, ms = 1500) {
  const end = Date.now() + ms;
  while (Date.now() < end) { if (await check()) return true; await new Promise((r) => setTimeout(r, 10)); }
  return false;
}

// ================= accounts =================

test('health check reports the service and Firebase status', async () => {
  const res = await call('GET', '/health');
  assert.equal(res.status, 200);
  assert.equal(res.body.status, 'ok');
  assert.equal(res.body.firebase, 'configured'); // the test doubles count as configured
});

test('register stores a bcrypt hash, never the plain password', async () => {
  const { password } = await registerUser();
  assert.equal(users.length, 1);
  assert.notEqual(users[0].password_hash, password);
  assert.match(users[0].password_hash, /^\$2[aby]\$12\$/);
});

test('register rejects a duplicate email, a short password and an empty name', async () => {
  await registerUser();
  const dup = await call('POST', '/api/auth/register', { body: { email: 'thabo@example.com', password: 'Password1', fullName: 'X Y' } });
  assert.equal(dup.status, 409);
  const weak = await call('POST', '/api/auth/register', { body: { email: 'a@b.co', password: 'short', fullName: 'X Y' } });
  assert.equal(weak.status, 400);
  const noName = await call('POST', '/api/auth/register', { body: { email: 'c@d.co', password: 'Password1', fullName: '  ' } });
  assert.equal(noName.status, 400);
});

test('login works with the right password and fails with the wrong one', async () => {
  const { email, password } = await registerUser();
  const ok = await call('POST', '/api/auth/login', { body: { email, password } });
  assert.equal(ok.status, 200);
  assert.ok(ok.body.token);
  const bad = await call('POST', '/api/auth/login', { body: { email, password: 'WrongPass9' } });
  assert.equal(bad.status, 401);
  const unknown = await call('POST', '/api/auth/login', { body: { email: 'nobody@x.co', password } });
  assert.equal(unknown.status, 401);
});

test('protected routes reject a missing or bad token', async () => {
  assert.equal((await call('GET', '/api/users/me')).status, 401);
  assert.equal((await call('GET', '/api/users/me', { token: 'not-a-token' })).status, 401);
  assert.equal((await call('POST', '/api/devices', { body: { token: FCM_TOKEN } })).status, 401);
});

// ================= settings, profile (NoSQL), password =================

test('settings: update own profile, language and notifications; refuse others and bad input', async () => {
  const { token, uid } = await registerUser();
  const ok = await call('PATCH', `/api/users/${uid}/settings`, { token, body: { fullName: 'Thabo M', schoolName: 'New School', preferredLanguage: 'isiZulu', notificationsEnabled: false } });
  assert.equal(ok.status, 200);
  assert.equal(ok.body.user.fullName, 'Thabo M');
  assert.equal(ok.body.user.preferredLanguage, 'isiZulu');
  assert.equal(ok.body.user.notificationsEnabled, false);
  // the preference is mirrored into Firestore for the push service
  assert.equal(db.docs.get(`users/${uid}`).notificationsEnabled, false);

  assert.equal((await call('PATCH', '/api/users/999/settings', { token, body: { preferredLanguage: 'EN' } })).status, 403);
  assert.equal((await call('PATCH', `/api/users/${uid}/settings`, { token, body: { preferredLanguage: 'Klingon' } })).status, 400);
  assert.equal((await call('PATCH', `/api/users/${uid}/settings`, { token, body: { fullName: 'A' } })).status, 400);
  assert.equal((await call('PATCH', `/api/users/${uid}/settings`, { token, body: { notificationsEnabled: 'yes' } })).status, 400);
});

test('academic profile is saved to Firestore and returned by /me', async () => {
  const { token, uid } = await registerUser();
  const profile = { grade: 12, subjects: [{ name: 'Mathematics', mark: 78 }, { name: 'Physical Sciences', mark: 71 }], apsScore: 34, province: 'Gauteng', fieldOfStudy: 'Engineering' };
  const saved = await call('PUT', '/api/users/me/profile', { token, body: profile });
  assert.equal(saved.status, 200);

  assert.deepEqual(db.docs.get(`users/${uid}`).academicProfile.subjects, profile.subjects); // it really is in the NoSQL store
  const me = await call('GET', '/api/users/me', { token });
  assert.equal(me.status, 200);
  assert.equal(me.body.user.email, 'thabo@example.com');
  assert.equal(me.body.academicProfile.grade, 12);
  assert.equal(me.body.academicProfile.province, 'Gauteng');
  assert.equal(me.body.academicProfile.yearOfStudy, null);
});

test('academic profile rejects invalid values without crashing', async () => {
  const { token } = await registerUser();
  for (const bad of [
    { grade: 13 }, { grade: 'x' }, { apsScore: 50 }, { subjects: 'maths' },
    { subjects: [{ name: 'Maths', mark: 101 }] }, { subjects: [{ name: '', mark: 50 }] }, { province: 'x'.repeat(41) },
  ]) {
    const res = await call('PUT', '/api/users/me/profile', { token, body: bad });
    assert.equal(res.status, 400, JSON.stringify(bad));
  }
  assert.equal((await call('PUT', '/api/users/me/profile', { token, body: {} })).status, 400);
});

test('change password: checks the current one, enforces length, and the new one works', async () => {
  const { token, email, password } = await registerUser();
  assert.equal((await call('PATCH', '/api/users/me/password', { token, body: { currentPassword: 'nope', newPassword: 'NewPass22' } })).status, 403);
  assert.equal((await call('PATCH', '/api/users/me/password', { token, body: { currentPassword: password, newPassword: 'short' } })).status, 400);
  assert.equal((await call('PATCH', '/api/users/me/password', { token, body: { newPassword: 'NewPass22' } })).status, 400);

  const ok = await call('PATCH', '/api/users/me/password', { token, body: { currentPassword: password, newPassword: 'NewPass22' } });
  assert.equal(ok.status, 200);
  assert.equal((await call('POST', '/api/auth/login', { body: { email, password: 'NewPass22' } })).status, 200);
  assert.equal((await call('POST', '/api/auth/login', { body: { email, password } })).status, 401);
  assert.match(users[0].password_hash, /^\$2[aby]\$12\$/);
});

// ================= push notifications =================

test('device registration stores the FCM token in Firestore and validates input', async () => {
  const { token, uid } = await registerUser();
  assert.equal((await call('POST', '/api/devices', { token, body: {} })).status, 400);
  assert.equal((await call('POST', '/api/devices', { token, body: { token: 'short' } })).status, 400);

  const res = await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });
  assert.equal(res.status, 201);
  const stored = [...db.docs.entries()].filter(([path]) => path.startsWith(`users/${uid}/devices/`));
  assert.equal(stored.length, 1);
  assert.equal(stored[0][1].token, FCM_TOKEN);

  // registering the same phone again does not create a duplicate
  await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });
  assert.equal([...db.docs.keys()].filter((p) => p.startsWith(`users/${uid}/devices/`)).length, 1);

  const gone = await call('DELETE', '/api/devices', { token, body: { token: FCM_TOKEN } });
  assert.equal(gone.status, 200);
  assert.equal([...db.docs.keys()].filter((p) => p.startsWith(`users/${uid}/devices/`)).length, 0);
});

test('test notification reaches the registered phone and lands in the inbox', async () => {
  const { token } = await registerUser();
  await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });

  const res = await call('POST', '/api/notifications/test', { token });
  assert.equal(res.status, 200);
  assert.equal(res.body.sent, 1);

  assert.equal(messaging.sent.length, 1);
  const message = messaging.sent[0];
  assert.deepEqual(message.tokens, [FCM_TOKEN]);
  assert.equal(message.notification.title, 'Varsivo test notification');
  assert.equal(message.android.notification.channelId, 'varsivo_alerts');
  assert.equal(message.data.type, 'test');
  for (const value of Object.values(message.data)) assert.equal(typeof value, 'string'); // FCM needs string values

  const inbox = await call('GET', '/api/notifications', { token });
  assert.equal(inbox.status, 200);
  assert.equal(inbox.body.length, 1);
  assert.equal(inbox.body[0].title, 'Varsivo test notification');
  assert.equal(inbox.body[0].read, false);
});

test('a user without a registered phone still gets the inbox entry and no error', async () => {
  const { token } = await registerUser();
  const res = await call('POST', '/api/notifications/test', { token });
  assert.equal(res.status, 200);
  assert.equal(res.body.devices, 0);
  assert.equal(messaging.sent.length, 0);
  assert.equal((await call('GET', '/api/notifications', { token })).body.length, 1);
});

test('tokens that FCM reports as dead are removed automatically', async () => {
  const { token, uid } = await registerUser();
  const dead = 'dead-token-' + 'b'.repeat(40);
  await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });
  await call('POST', '/api/devices', { token, body: { token: dead } });
  messaging.failures.set(dead, 'messaging/registration-token-not-registered');

  const res = await call('POST', '/api/notifications/test', { token });
  assert.equal(res.body.sent, 1);
  assert.equal(res.body.failed, 1);
  const left = [...db.docs.entries()].filter(([p]) => p.startsWith(`users/${uid}/devices/`)).map(([, d]) => d.token);
  assert.deepEqual(left, [FCM_TOKEN]);
});

test('no push is sent when the user switched notifications off in Settings', async () => {
  const { token, uid } = await registerUser();
  await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });
  await call('PATCH', `/api/users/${uid}/settings`, { token, body: { notificationsEnabled: false } });

  const res = await call('POST', '/api/notifications/test', { token });
  assert.equal(res.body.skipped, 'notifications_disabled');
  assert.equal(messaging.sent.length, 0);
});

// ================= applications raise alerts =================

test('applications: validation, create, update status and delete - each change pushes an alert', async () => {
  const { token } = await registerUser();
  await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });

  assert.equal((await call('POST', '/api/applications', { token, body: {} })).status, 400);
  assert.equal((await call('POST', '/api/applications', { token, body: { institutionId: 'abc' } })).status, 400);
  assert.equal((await call('POST', '/api/applications', { token, body: { institutionId: 3, status: 'x'.repeat(31) } })).status, 400);

  const created = await call('POST', '/api/applications', { token, body: { institutionId: 3 } });
  assert.equal(created.status, 201);
  assert.equal(created.body.status, 'PENDING');
  assert.ok(await waitFor(() => messaging.sent.length === 1), 'create alert was not pushed');
  assert.equal(messaging.sent[0].notification.title, 'Application saved');

  const id = created.body.id;
  const patched = await call('PATCH', `/api/applications/${id}`, { token, body: { status: 'OFFER' } });
  assert.equal(patched.status, 200);
  assert.equal(patched.body.status, 'OFFER');
  assert.ok(await waitFor(() => messaging.sent.length === 2), 'status alert was not pushed');
  assert.match(messaging.sent[1].notification.body, /OFFER/);

  assert.equal((await call('PATCH', `/api/applications/${id}`, { token, body: {} })).status, 400);
  assert.equal((await call('PATCH', '/api/applications/9999', { token, body: { status: 'OFFER' } })).status, 404);
  assert.equal((await call('GET', '/api/applications', { token })).body.length, 1);

  assert.equal((await call('DELETE', `/api/applications/${id}`, { token })).status, 200);
  assert.equal((await call('DELETE', `/api/applications/${id}`, { token })).status, 404);
  assert.equal((await call('GET', '/api/applications', { token })).body.length, 0);
});

test("one student cannot change or delete another student's application", async () => {
  const a = await registerUser('a@example.com');
  const b = await registerUser('b@example.com');
  const created = await call('POST', '/api/applications', { token: a.token, body: { institutionId: 1 } });
  assert.equal((await call('PATCH', `/api/applications/${created.body.id}`, { token: b.token, body: { status: 'OFFER' } })).status, 404);
  assert.equal((await call('DELETE', `/api/applications/${created.body.id}`, { token: b.token })).status, 404);
});

test('a push failure never breaks saving an application', async () => {
  const { token } = await registerUser();
  await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });
  messaging.sendEachForMulticast = async () => { throw new Error('FCM is down'); };
  const created = await call('POST', '/api/applications', { token, body: { institutionId: 2 } });
  assert.equal(created.status, 201);
});

// ================= operator endpoints =================

test('admin endpoints need the admin key', async () => {
  assert.equal((await call('POST', '/api/notifications/broadcast', { body: { title: 'x', body: 'y' } })).status, 403);
  assert.equal((await call('POST', '/api/notifications/broadcast', { headers: { 'x-admin-key': 'wrong' }, body: { title: 'x', body: 'y' } })).status, 403);
  const res = await call('POST', '/api/notifications/broadcast', { headers: { 'x-admin-key': 'admin-test-key' }, body: {} });
  assert.equal(res.status, 400);
});

test('broadcast reaches every phone except users who switched notifications off', async () => {
  const a = await registerUser('a@example.com');
  const b = await registerUser('b@example.com');
  await call('POST', '/api/devices', { token: a.token, body: { token: 'token-a-' + 'a'.repeat(40) } });
  await call('POST', '/api/devices', { token: b.token, body: { token: 'token-b-' + 'b'.repeat(40) } });
  await call('PATCH', `/api/users/${b.uid}/settings`, { token: b.token, body: { notificationsEnabled: false } });

  const res = await call('POST', '/api/notifications/broadcast', { headers: { 'x-admin-key': 'admin-test-key' }, body: { title: 'Hello', body: 'World' } });
  assert.equal(res.status, 200);
  assert.equal(res.body.users, 1);
  assert.equal(res.body.sent, 1);
  assert.equal(messaging.sent.length, 1);
  assert.deepEqual(messaging.sent[0].tokens, ['token-a-' + 'a'.repeat(40)]);
});

test('deadline alerts: only bursaries closing soon, and never twice on the same day', async () => {
  const { token } = await registerUser();
  await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } });
  const soon = new Date(Date.now() + 3 * 24 * 3600 * 1000);
  const later = new Date(Date.now() + 40 * 24 * 3600 * 1000);
  const past = new Date(Date.now() - 5 * 24 * 3600 * 1000);
  bursaryStore.getAll = async () => [
    { id: 1, name: 'Sasol Bursary', closing_date: soon },
    { id: 2, name: 'Far Away Bursary', closing_date: later },
    { id: 3, name: 'Expired Bursary', closing_date: past },
    { id: 4, name: 'No Date Bursary' },
  ];
  const admin = { 'x-admin-key': 'admin-test-key' };

  const first = await call('POST', '/api/notifications/deadlines/run', { headers: admin, body: {} });
  assert.equal(first.status, 200);
  assert.equal(first.body.checked, 4);
  assert.equal(first.body.alerts.length, 1);
  assert.equal(first.body.alerts[0].bursary, 'Sasol Bursary');
  assert.match(messaging.sent[0].notification.body, /Sasol Bursary closes in [34] days/);

  const second = await call('POST', '/api/notifications/deadlines/run', { headers: admin, body: {} });
  assert.equal(second.body.alerts.length, 0);
  assert.equal(messaging.sent.length, 1);
});

// ================= Firestore not configured =================

test('without Firebase, /me still works and NoSQL endpoints answer 503 instead of crashing', async () => {
  const { token } = await registerUser();
  firebase.setTestDoubles(null); // behaves like a server with no service-account key
  const me = await call('GET', '/api/users/me', { token });
  assert.equal(me.status, 200);
  assert.equal(me.body.academicProfile.grade, null);
  assert.equal((await call('PUT', '/api/users/me/profile', { token, body: { grade: 11 } })).status, 503);
  assert.equal((await call('POST', '/api/devices', { token, body: { token: FCM_TOKEN } })).status, 503);
  // saving an application still works - alerts are simply skipped
  assert.equal((await call('POST', '/api/applications', { token, body: { institutionId: 1 } })).status, 201);
});
