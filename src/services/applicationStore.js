const { getPool, sql } = require('../config/database');

async function getAllForUser(userId) {
  const pool = await getPool();
  const result = await pool.request()
    .input('user_id', sql.Int, userId)
    .query(`
      SELECT a.*, i.name AS institution_name
      FROM applications a
      JOIN institutions i ON a.institution_id = i.id
      WHERE a.user_id = @user_id
    `);
  return result.recordset;
}

async function create(userId, institutionId, status) {
  const pool = await getPool();
  const result = await pool.request()
    .input('user_id', sql.Int, userId)
    .input('institution_id', sql.Int, institutionId)
    .input('status', sql.NVarChar, status)
    .query(`
      INSERT INTO applications (user_id, institution_id, status)
      OUTPUT INSERTED.*
      VALUES (@user_id, @institution_id, @status)
    `);
  return result.recordset[0];
}

/** Changes the status of one of the user's own applications. Returns the row, or null if not found. */
async function updateStatus(userId, id, status) {
  const pool = await getPool();
  const result = await pool.request()
    .input('id', sql.Int, id)
    .input('user_id', sql.Int, userId)
    .input('status', sql.NVarChar, status)
    .query(`
      UPDATE applications
      SET status = @status
      OUTPUT INSERTED.*
      WHERE id = @id AND user_id = @user_id
    `);
  return result.recordset[0] || null;
}

/** Deletes one of the user's own applications. Returns true if a row was removed. */
async function remove(userId, id) {
  const pool = await getPool();
  const result = await pool.request()
    .input('id', sql.Int, id)
    .input('user_id', sql.Int, userId)
    .query('DELETE FROM applications WHERE id = @id AND user_id = @user_id');
  return result.rowsAffected[0] > 0;
}

module.exports = { getAllForUser, create, updateStatus, remove };