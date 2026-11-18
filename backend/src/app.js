'use strict';
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { buildRouter } = require('./routes');

function createApp(deps) {
  const app = express();
  app.disable('x-powered-by');
  app.set('trust proxy', 1);
  app.use(helmet());
  app.use(rateLimit({ windowMs: 15 * 60 * 1000, limit: 300, standardHeaders: true, legacyHeaders: false }));
  app.get('/health', (req, res) => res.json({ ok: true }));
  app.use('/api', buildRouter(deps));
  app.use((req, res) => res.status(404).json({ error: 'not found' }));
  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    if (err && err.type === 'entity.too.large') return res.status(413).json({ error: 'payload too large' });
    if (err && err.type === 'entity.parse.failed') return res.status(400).json({ error: 'invalid JSON' });
    console.error(err);
    res.status(500).json({ error: 'internal error' });
  });
  return app;
}

module.exports = { createApp };
