const bcrypt = require('bcryptjs');
const userStore = require('../services/userStore');
const firestoreStore = require('../services/firestoreStore');
const firebase = require('../config/firebase');

const ALLOWED_LANGUAGES = ['EN', 'isiZulu', 'Afrikaans'];
const NO_PROFILE = { grade: null, yearOfStudy: null, subjects: [], apsScore: null, province: null, fieldOfStudy: null };

/** The NoSQL profile is optional: if Firestore is down or not set up, /me still works. */
async function loadAcademicProfile(uid) {
  if (!firebase.isConfigured()) return NO_PROFILE;
  try {
    return await firestoreStore.getAcademicProfile(uid);
  } catch (err) {
    console.error('[users] could not read academic profile from Firestore:', err.message);
    return NO_PROFILE;
  }
}

/** Copies the notification preference to Firestore (used when pushing). Never throws. */
async function mirrorNotificationPreference(uid, enabled) {
  if (!firebase.isConfigured()) return;
  try {
    await firestoreStore.setNotificationsEnabled(uid, enabled);
  } catch (err) {
    console.error('[users] could not mirror notification preference to Firestore:', err.message);
  }
}

/**
 * GET /api/users/me
 * Requires auth. Account details come from Azure SQL, the academic profile from Firestore (NoSQL).
 */
exports.getMe = async (req, res, next) => {
  try {
    const row = await userStore.findById(req.user.uid);
    if (!row) {
      return res.status(404).json({ error: 'User not found' });
    }

    // mapRowToUser already excludes password_hash and salt
    const user = userStore.mapRowToUser(row);
    res.json({ user, academicProfile: await loadAcademicProfile(req.user.uid) });
  } catch (err) {
    next(err);
  }
};

/**
 * PATCH /api/users/:id/settings
 * Requires auth. Only the user themselves can update their own settings.
 *
 * Body (any subset):
 *   { fullName?, schoolName?, preferredLanguage?, notificationsEnabled? }
 */
exports.updateSettings = async (req, res, next) => {
  try {
    const { id } = req.params;

    // Users can only update their own settings
    if (id !== req.user.uid) {
      return res.status(403).json({ error: 'You can only update your own settings' });
    }

    const { preferredLanguage, notificationsEnabled, fullName, schoolName } = req.body || {};

    if (preferredLanguage !== undefined && !ALLOWED_LANGUAGES.includes(preferredLanguage)) {
      return res.status(400).json({
        error: `preferredLanguage must be one of: ${ALLOWED_LANGUAGES.join(', ')}`,
      });
    }
    if (notificationsEnabled !== undefined && typeof notificationsEnabled !== 'boolean') {
      return res.status(400).json({ error: 'notificationsEnabled must be true or false' });
    }
    if (fullName !== undefined && (typeof fullName !== 'string' || fullName.trim().length < 2 || fullName.length > 100)) {
      return res.status(400).json({ error: 'fullName must be 2-100 characters' });
    }
    if (schoolName !== undefined && (typeof schoolName !== 'string' || schoolName.length > 100)) {
      return res.status(400).json({ error: 'schoolName must be at most 100 characters' });
    }

    const updated = await userStore.updateSettings(id, {
      preferredLanguage,
      notificationsEnabled,
      fullName: fullName === undefined ? undefined : fullName.trim(),
      schoolName: schoolName === undefined ? undefined : schoolName.trim(),
    });

    if (!updated) {
      return res.status(404).json({ error: 'User not found' });
    }

    if (notificationsEnabled !== undefined) await mirrorNotificationPreference(id, notificationsEnabled);

    const user = userStore.mapRowToUser(updated);
    res.json({ message: 'Settings updated', user });
  } catch (err) {
    next(err);
  }
};

/** Checks the academic profile fields; returns { value } or { errors }. */
function validateProfile(body) {
  const errors = [];
  const value = {};
  const has = (key) => body[key] !== undefined;
  const intOrNull = (key, min, max) => {
    if (body[key] === null) { value[key] = null; return; }
    const n = Number(body[key]);
    if (!Number.isInteger(n) || n < min || n > max) errors.push(`${key} must be a whole number from ${min} to ${max}, or null`);
    else value[key] = n;
  };
  const textOrNull = (key, max) => {
    if (body[key] === null) { value[key] = null; return; }
    if (typeof body[key] !== 'string' || body[key].length > max) errors.push(`${key} must be text of at most ${max} characters, or null`);
    else value[key] = body[key].trim();
  };

  if (has('grade')) intOrNull('grade', 8, 12);
  if (has('yearOfStudy')) intOrNull('yearOfStudy', 1, 10);
  if (has('apsScore')) intOrNull('apsScore', 0, 42);
  if (has('province')) textOrNull('province', 40);
  if (has('fieldOfStudy')) textOrNull('fieldOfStudy', 60);

  if (has('subjects')) {
    const subjects = body.subjects;
    if (!Array.isArray(subjects) || subjects.length > 12) {
      errors.push('subjects must be a list of at most 12 items');
    } else {
      const clean = [];
      for (const s of subjects) {
        const mark = Number(s && s.mark);
        if (!s || typeof s.name !== 'string' || !s.name.trim() || s.name.length > 50 || !Number.isInteger(mark) || mark < 0 || mark > 100) {
          errors.push('each subject needs a name (max 50 characters) and a whole-number mark from 0 to 100');
          break;
        }
        clean.push({ name: s.name.trim(), mark });
      }
      value.subjects = clean;
    }
  }
  return errors.length ? { errors } : { value };
}

/**
 * PUT /api/users/me/profile
 * Saves grade / year of study / subjects / APS / province / field of study in Firestore (NoSQL).
 */
exports.updateProfile = async (req, res, next) => {
  try {
    const { value, errors } = validateProfile(req.body || {});
    if (errors) return res.status(400).json({ error: 'Validation failed', details: errors });
    if (Object.keys(value).length === 0) return res.status(400).json({ error: 'Send at least one profile field' });

    const academicProfile = await firestoreStore.saveAcademicProfile(req.user.uid, value);
    res.json({ message: 'Profile saved', academicProfile });
  } catch (err) {
    next(err);
  }
};

/**
 * PATCH /api/users/me/password   body: { currentPassword, newPassword }
 * The new password is hashed with bcrypt (12 rounds) before it is stored - never saved in plain text.
 */
exports.changePassword = async (req, res, next) => {
  try {
    const { currentPassword, newPassword } = req.body || {};
    if (typeof currentPassword !== 'string' || !currentPassword) {
      return res.status(400).json({ error: 'currentPassword is required' });
    }
    if (typeof newPassword !== 'string' || newPassword.length < 8) {
      return res.status(400).json({ error: 'newPassword must be at least 8 characters' });
    }

    const row = await userStore.findById(req.user.uid);
    if (!row) return res.status(404).json({ error: 'User not found' });

    const matches = await bcrypt.compare(currentPassword, row.password_hash);
    if (!matches) return res.status(403).json({ error: 'Current password is incorrect' });

    const hash = await bcrypt.hash(newPassword, 12);
    await userStore.updatePassword(req.user.uid, hash);
    console.log(`[users] password changed for user ${req.user.uid}`);
    res.json({ message: 'Password updated' });
  } catch (err) {
    next(err);
  }
};
