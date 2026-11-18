'use strict';
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { validateRegister, validateLogin } = require('../validators');
const { audit } = require('../services/audit');

// user na mile tab bhi hash compare karte hain, taaki response time se pata na chale
const DUMMY_HASH = bcrypt.hashSync('dummy-password-for-timing', 12);

function authController({ pool, config }) {
  const sign = (id) => jwt.sign({ sub: String(id) }, config.jwtSecret, { algorithm: 'HS256', expiresIn: config.tokenTtl });

  async function register(req, res, next) {
    try {
      if (!config.allowRegistration) return res.status(403).json({ error: 'registration disabled' });
      const v = validateRegister(req.body);
      if (!v.ok) return res.status(400).json({ error: v.error });
      if (config.singleOwner) {
        const c = await pool.query('SELECT COUNT(*)::int AS n FROM owners');
        if (c.rows[0].n > 0) return res.status(403).json({ error: 'this server already has an owner account' });
      }
      const hash = await bcrypt.hash(v.value.password, 12);
      const r = await pool.query(
        'INSERT INTO owners (shop_name, mobile, password_hash) VALUES ($1, $2, $3) ON CONFLICT (mobile) DO NOTHING RETURNING id',
        [v.value.shopName, v.value.mobile, hash]
      );
      if (r.rowCount === 0) return res.status(409).json({ error: 'mobile already registered' });
      const id = r.rows[0].id;
      await audit(pool, id, 'register', null, req.ip);
      res.status(201).json({ token: sign(id) });
    } catch (e) { next(e); }
  }

  async function login(req, res, next) {
    try {
      const v = validateLogin(req.body);
      if (!v.ok) return res.status(400).json({ error: v.error });
      const r = await pool.query('SELECT id, password_hash FROM owners WHERE mobile = $1', [v.value.mobile]);
      const row = r.rows[0];
      const ok = await bcrypt.compare(v.value.password, row ? row.password_hash : DUMMY_HASH);
      if (!row || !ok) {
        await audit(pool, row && row.id, 'login_failed', v.value.mobile.slice(-4), req.ip);
        return res.status(401).json({ error: 'invalid mobile or password' });
      }
      await audit(pool, row.id, 'login', null, req.ip);
      res.json({ token: sign(row.id) });
    } catch (e) { next(e); }
  }

  return { register, login };
}

module.exports = { authController };
