const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const errorHandler = require('./middleware/errorHandler');

const app = express();

const institutionRoutes = require('./routes/institution.routes');
const bursaryRoutes = require('./routes/bursary.routes');
const applicationRoutes = require('./routes/application.routes');
const deviceRoutes = require('./routes/device.routes');
const notificationRoutes = require('./routes/notification.routes');
const firebase = require('./config/firebase');

// ─── Security & Parsing Middleware ─────────────────────────────

// helmet: adds HTTP security headers (X-Frame-Options, etc.)
app.use(helmet());

// cors: allows the Android app to call this server from another origin
app.use(cors());

// express.json: parses JSON request bodies into req.body
app.use(express.json({ limit: '1mb' }));

// morgan: logs each request to the console (method, URL, status, time)
app.use(morgan('dev'));

// ─── Rate Limiting ─────────────────────────────────────────────
// Prevent abuse: max 100 requests per 15 minutes per IP
app.use('/api', rateLimit({
  windowMs: 15 * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_MAX || '100', 10), // RATE_LIMIT_MAX lets the automated tests send many requests
  message: { error: 'Too many requests, please try again later.' },
}));

app.use('/api/institutions', institutionRoutes);
app.use('/api/bursaries', bursaryRoutes);
app.use('/api/applications', applicationRoutes);
app.use('/api/devices', deviceRoutes);
app.use('/api/notifications', notificationRoutes);

// ─── Health Check ──────────────────────────────────────────────
// A simple endpoint to confirm the server is running
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'varsivo-api',
    firebase: firebase.isConfigured() ? 'configured' : 'not configured',
    timestamp: new Date().toISOString(),
  });
});

// ─── Routes ────────────────────────────────────────────────────
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);

// ─── 404 Handler ───────────────────────────────────────────────
// Any request that reaches this point didn't match a route
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// ─── Global Error Handler ──────────────────────────────────────
// Must be the LAST middleware. Handles any error thrown by controllers.
app.use(errorHandler);

// ─── Start the Server ──────────────────────────────────────────
// PORT comes from .env, falls back to 3000
// "0.0.0.0" binds to all network interfaces — needed for deployments.
// The app is exported so the automated tests can start it on a random port;
// it only listens when started with `npm start` (node src/app.js).
if (require.main === module) {
  const PORT = process.env.PORT || 3000;
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Varsivo API running on port ${PORT}`);
  });

  // Optional daily "bursary closing soon" push alerts (also available on demand through
  // POST /api/notifications/deadlines/run). Turn on with ENABLE_DEADLINE_JOB=true.
  if (process.env.ENABLE_DEADLINE_JOB === 'true' && firebase.isConfigured()) {
    const { runDeadlineAlerts } = require('./services/notificationService');
    const ONE_DAY = 24 * 60 * 60 * 1000;
    setInterval(() => {
      runDeadlineAlerts().catch((err) => console.error('[deadline-job] failed:', err.message));
    }, ONE_DAY);
    console.log('[deadline-job] daily bursary deadline alerts enabled');
  }
}

module.exports = app;
