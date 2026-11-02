const jwt = require('jsonwebtoken');

/**
 * Verifies the JWT from the Authorization header.
 * Attaches { uid, email, role } to req.user on success.
 */
const authenticate = (req, res, next) => {
  const header = req.headers.authorization;

  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing or invalid Authorization header' });
  }

  const token = header.split(' ')[1];

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = { uid: decoded.uid, email: decoded.email, role: decoded.role };
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
};

module.exports = { authenticate };