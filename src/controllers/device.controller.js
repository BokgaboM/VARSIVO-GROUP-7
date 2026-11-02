const firestoreStore = require('../services/firestoreStore');
const userStore = require('../services/userStore');

/**
 * POST /api/devices   body: { token, platform? }
 * The Android app calls this after login (and whenever FCM issues a new token) so the API knows
 * which phone to push to. Also mirrors the user's notification preference into Firestore.
 */
exports.registerDevice = async (req, res, next) => {
  try {
    const { token, platform = 'android' } = req.body || {};
    if (typeof token !== 'string' || token.length < 20 || token.length > 4096) {
      return res.status(400).json({ error: 'token must be a valid FCM registration token' });
    }
    await firestoreStore.saveDeviceToken(req.user.uid, token, String(platform).slice(0, 20));

    // Keep Firestore's copy of the preference in step with the SQL row.
    const row = await userStore.findById(req.user.uid);
    if (row) {
      const enabled = userStore.mapRowToUser(row).notificationsEnabled;
      await firestoreStore.setNotificationsEnabled(req.user.uid, enabled);
    }
    console.log(`[devices] registered a ${platform} device for user ${req.user.uid}`);
    res.status(201).json({ registered: true });
  } catch (err) {
    next(err);
  }
};

/** DELETE /api/devices   body: { token }   (called on logout so the phone stops getting alerts) */
exports.unregisterDevice = async (req, res, next) => {
  try {
    const { token } = req.body || {};
    if (typeof token !== 'string' || !token) {
      return res.status(400).json({ error: 'token is required' });
    }
    await firestoreStore.removeDeviceToken(req.user.uid, token);
    res.json({ unregistered: true });
  } catch (err) {
    next(err);
  }
};
