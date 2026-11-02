const { getPool, sql } = require('../config/database');

async function getAll() {
  const pool = await getPool();
  const result = await pool.request().query('SELECT * FROM bursaries');
  return result.recordset;
}

module.exports = { getAll };