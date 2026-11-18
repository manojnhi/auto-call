'use strict';
const { Pool } = require('pg');

function createPool(databaseUrl) {
  return new Pool({ connectionString: databaseUrl, max: 10 });
}

module.exports = { createPool };
