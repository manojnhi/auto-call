'use strict';
const fs = require('fs');
const path = require('path');
const { load } = require('../config');
const { createPool } = require('../config/db');

(async () => {
  const cfg = load();
  const pool = createPool(cfg.databaseUrl);
  await pool.query(fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8'));
  await pool.end();
  console.log('schema ready');
})().catch((e) => { console.error(e); process.exit(1); });
