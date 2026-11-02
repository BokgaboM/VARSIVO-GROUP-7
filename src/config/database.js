const sql = require('mssql');
require('dotenv').config();


const config = {
  server: process.env.DB_SERVER,
  database: process.env.DB_DATABASE,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  port: parseInt(process.env.DB_PORT || '1433', 10),
  options: {
    encrypt: true,                 // Required for Azure SQL
    trustServerCertificate: false, // Recommended for production
    enableArithAbort: true,
  },
  pool: {
    max: 10,
    min: 0,
    idleTimeoutMillis: 30000,
  },
  connectionTimeout: 30000,
  requestTimeout: 30000,
};

let poolPromise = null;


async function getPool() {
  if (poolPromise) return poolPromise;

  poolPromise = new sql.ConnectionPool(config)
    .connect()
    .then((pool) => {
      console.log(' Connected to Azure SQL Database');
      return pool;
    })
    .catch((err) => {
      console.error(' Database connection failed:', err.message);
      poolPromise = null; // allow retry on next call
      throw err;
    });

  return poolPromise;
}


async function testConnection() {
  try {
    const pool = await getPool();
    const result = await pool.request().query('SELECT 1 AS ok');
    return result.recordset[0].ok === 1;
  } catch (err) {
    return false;
  }
}

module.exports = { sql, getPool, testConnection };