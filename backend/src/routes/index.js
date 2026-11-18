'use strict';
const express = require('express');
const rateLimit = require('express-rate-limit');
const { requireAuth } = require('../middleware/auth');
const { authController } = require('../controllers/authController');
const { backupController } = require('../controllers/backupController');

function buildRouter(deps) {
  const router = express.Router();
  const auth = authController(deps);
  const backup = backupController(deps);

  const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, limit: 10, standardHeaders: true, legacyHeaders: false,
    message: { error: 'too many attempts, try again later' },
  });

  router.post('/auth/register', authLimiter, express.json({ limit: '10kb' }), auth.register);
  router.post('/auth/login', authLimiter, express.json({ limit: '10kb' }), auth.login);

  const guard = requireAuth(deps.config.jwtSecret);
  router.get('/backup', guard, backup.get);
  router.put('/backup', guard, express.json({ limit: '25mb' }), backup.put);
  return router;
}

module.exports = { buildRouter };
