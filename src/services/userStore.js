const crypto = require('crypto');
const { getPool, sql } = require('../config/database');


function mapRowToUser(row) {
  if (!row) return null;
  return {
    uid: String(row.id),           // DB int -> Kotlin String
    email: row.email,
    fullName: row.full_name,
    schoolName: row.school_name,
    studentId: row.student_id,
    preferredLanguage: row.language_pref,
    // The column may be NULL on older rows; treat "never set" as on.
    notificationsEnabled: row.notifications_enabled === null || row.notifications_enabled === undefined
      ? true
      : Boolean(row.notifications_enabled),
    role: row.role || 'high_school_learner',   // fallback if column is null
    createdAt: row.created_at,
  };
}

function generateSalt() {
  return crypto.randomBytes(16).toString('hex');
}

function generateStudentIdPlaceholder() {
  return 'TEMP-' + crypto.randomBytes(4).toString('hex').toUpperCase();
}

async function findByEmail(email) {
  const pool = await getPool();
  const result = await pool.request()
    .input('email', sql.NVarChar, email.toLowerCase())
    .query('SELECT TOP 1 * FROM users WHERE email = @email');
  return result.recordset[0] || null;
}

async function findById(uid) {
  const pool = await getPool();
  const idInt = parseInt(uid, 10);
  if (isNaN(idInt)) return null;

  const result = await pool.request()
    .input('id', sql.Int, idInt)
    .query('SELECT TOP 1 * FROM users WHERE id = @id');
  return result.recordset[0] || null;
}

async function save(user) {
  const pool = await getPool();

  const result = await pool.request()
    .input('email', sql.NVarChar, user.email.toLowerCase())
    .input('password_hash', sql.NVarChar, user.passwordHash)
    .input('full_name', sql.NVarChar, user.fullName)
    .input('school_name', sql.NVarChar, user.schoolName || null)
    .input('student_id', sql.NVarChar, user.studentId || generateStudentIdPlaceholder())
    .input('salt', sql.NVarChar, generateSalt())
    .input('language_pref', sql.NVarChar, user.preferredLanguage || 'en')
    .input('role', sql.NVarChar, user.role || 'high_school_learner')
    .query(`
      INSERT INTO users
        (email, password_hash, full_name, school_name, student_id, salt, language_pref, role)
      OUTPUT INSERTED.*
      VALUES
        (@email, @password_hash, @full_name, @school_name, @student_id, @salt, @language_pref, @role)
    `);

  return mapRowToUser(result.recordset[0]);
}


/**
 * Updates one or more settings for a user.
 * `updates` is an object with a subset of:
 *   { preferredLanguage, notificationsEnabled, fullName, schoolName }
 * Returns the updated row, or null if the user doesn't exist.
 */


async function updateSettings(uid, updates) {
  const pool = await getPool();
  const idInt = parseInt(uid, 10);
  if (isNaN(idInt)) return null;

  // Map client-side camelCase fields to DB snake_case columns
  const setClauses = [];
  const request = pool.request().input('id', sql.Int, idInt);

  if (updates.preferredLanguage !== undefined) {
    request.input('language_pref', sql.NVarChar, updates.preferredLanguage);
    setClauses.push('language_pref = @language_pref');
  }

  if (updates.fullName !== undefined) {
    request.input('full_name', sql.NVarChar, updates.fullName);
    setClauses.push('full_name = @full_name');
  }

  if (updates.schoolName !== undefined) {
    request.input('school_name', sql.NVarChar, updates.schoolName || null);
    setClauses.push('school_name = @school_name');
  }

  if (updates.notificationsEnabled !== undefined) {
    request.input('notifications_enabled', sql.Bit, updates.notificationsEnabled ? 1 : 0);
    setClauses.push('notifications_enabled = @notifications_enabled');
  }

  if (setClauses.length === 0) {
    // Nothing to update — just return current row
    const result = await pool.request()
      .input('id', sql.Int, idInt)
      .query('SELECT TOP 1 * FROM users WHERE id = @id');
    return result.recordset[0] || null;
  }

  const query = `
    UPDATE users
    SET ${setClauses.join(', ')}
    OUTPUT INSERTED.*
    WHERE id = @id
  `;

  const result = await request.query(query);
  return result.recordset[0] || null;
}

/** Stores a new bcrypt hash for the user. Returns true if the user exists. */
async function updatePassword(uid, passwordHash) {
  const pool = await getPool();
  const idInt = parseInt(uid, 10);
  if (isNaN(idInt)) return false;
  const result = await pool.request()
    .input('id', sql.Int, idInt)
    .input('password_hash', sql.NVarChar, passwordHash)
    .query('UPDATE users SET password_hash = @password_hash WHERE id = @id');
  return result.rowsAffected[0] > 0;
}

module.exports = { findByEmail, findById, save, updateSettings, updatePassword, mapRowToUser };