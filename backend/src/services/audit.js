'use strict';

async function audit(pool, ownerId, action, detail, ip) {
  try {
    await pool.query(
      'INSERT INTO audit_logs (owner_id, action, detail, ip) VALUES ($1, $2, $3, $4)',
      [ownerId || null, action, detail || null, ip || null]
    );
  } catch (e) {
    console.error('audit failed', e.message);
  }
}

module.exports = { audit };
