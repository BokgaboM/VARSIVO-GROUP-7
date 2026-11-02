const firestoreStore = require('../services/firestoreStore');
const notificationService = require('../services/notificationService');

/** GET /api/notifications?limit=20 - the signed-in user's notification inbox (newest first). */
exports.listMine = async (req, res, next) => {
  try {
    const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 50, 1), 100);
    res.json(await firestoreStore.listNotifications(req.user.uid, limit));
  } catch (err) {
    next(err);
  }
};

/** POST /api/notifications/test - sends a test push to the caller's own phones (used in the demo). */
exports.sendTest = async (req, res, next) => {
  try {
    const result = await notificationService.sendToUser(req.user.uid, {
      title: 'Varsivo test notification',
      body: 'Push notifications are working on this phone.',
      type: 'test',
    });
    res.json(result);
  } catch (err) {
    next(err);
  }
};

/** POST /api/notifications/broadcast  (admin key)  body: { title, body } */
exports.broadcast = async (req, res, next) => {
  try {
    const { title, body } = req.body || {};
    if (!title || !body) return res.status(400).json({ error: 'title and body are required' });
    res.json(await notificationService.broadcast({ title: String(title).slice(0, 100), body: String(body).slice(0, 500), type: 'announcement' }));
  } catch (err) {
    next(err);
  }
};

/** POST /api/notifications/deadlines/run  (admin key)  body: { days? } - sends "closing soon" alerts. */
exports.runDeadlines = async (req, res, next) => {
  try {
    const days = Math.min(Math.max(parseInt(req.body && req.body.days, 10) || 7, 0), 60);
    res.json(await notificationService.runDeadlineAlerts({ days }));
  } catch (err) {
    next(err);
  }
};
