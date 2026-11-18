'use strict';
const jwt = require('jsonwebtoken');

function requireAuth(secret) {
  return (req, res, next) => {
    const h = req.headers.authorization || '';
    const m = /^Bearer (.+)$/.exec(h);
    if (!m) return res.status(401).json({ error: 'missing token' });
    try {
      const payload = jwt.verify(m[1], secret, { algorithms: ['HS256'] });
      req.ownerId = payload.sub;
      next();
    } catch (e) {
      res.status(401).json({ error: 'invalid or expired token' });
    }
  };
}

module.exports = { requireAuth };
