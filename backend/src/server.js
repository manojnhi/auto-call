'use strict';
const { load } = require('./config');
const { createPool } = require('./config/db');
const { createApp } = require('./app');

const config = load();
const pool = createPool(config.databaseUrl);
createApp({ pool, config }).listen(config.port, () => console.log(`GramCall backend on :${config.port}`));
