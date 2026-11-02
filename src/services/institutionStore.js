const { getPool, sql } = require('../config/database');

async function getAll(aps) {
  const pool = await getPool();
  if (aps) {
    const result = await pool.request()
      .input('aps', sql.Int, parseInt(aps, 10))
      .query('SELECT * FROM institutions WHERE min_aps <= @aps');
    return result.recordset;
  }
  const result = await pool.request().query('SELECT * FROM institutions');
  return result.recordset;
}

module.exports = { getAll };