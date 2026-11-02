const crypto = require('crypto');

/**
 * Protects "operator" endpoints (broadcast, deadline run) with a shared secret sent in the
 * `x-admin-key` header. If ADMIN_API_KEY is not set the endpoints are switched off.
 */
const requireAdminKey = (req, res, next) => {
  const expected = process.env.ADMIN_API_KEY;
  if (!expected) {
    return res.status(503).json({ error: 'Admin endpoints are disabled (ADMIN_API_KEY is not set)' });
  }
  const provided = String(req.headers['x-admin-key'] || '');
  const a = Buffer.from(provided);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
    return res.status(403).json({ error: 'Invalid admin key' });
  }
  next();
};

module.exports = { requireAdminKey };
