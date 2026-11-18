'use strict';
const { validateBackup } = require('../validators');
const { audit } = require('../services/audit');

function backupController({ pool }) {
  async function get(req, res, next) {
    try {
      const r = await pool.query('SELECT version, payload, updated_at FROM backups WHERE owner_id = $1', [req.ownerId]);
      if (r.rowCount === 0) return res.status(404).json({ error: 'no backup yet' });
      await audit(pool, req.ownerId, 'backup_download', `v${r.rows[0].version}`, req.ip);
      res.json({ version: r.rows[0].version, updatedAt: r.rows[0].updated_at, payload: r.rows[0].payload });
    } catch (e) { next(e); }
  }

  async function put(req, res, next) {
    try {
      const v = validateBackup(req.body);
      if (!v.ok) return res.status(400).json({ error: v.error });
      let version;
      if (v.value.baseVersion === 0) {
        const r = await pool.query(
          'INSERT INTO backups (owner_id, version, payload) VALUES ($1, 1, $2) ON CONFLICT (owner_id) DO NOTHING RETURNING version',
          [req.ownerId, v.value.payload]
        );
        if (r.rowCount === 0) return res.status(409).json({ error: 'a newer backup already exists; download it first' });
        version = r.rows[0].version;
      } else {
        const r = await pool.query(
          'UPDATE backups SET payload = $2, version = version + 1, updated_at = now() WHERE owner_id = $1 AND version = $3 RETURNING version',
          [req.ownerId, v.value.payload, v.value.baseVersion]
        );
        if (r.rowCount === 0) return res.status(409).json({ error: 'version conflict; download the latest backup first' });
        version = r.rows[0].version;
      }
      await audit(pool, req.ownerId, 'backup_upload', `v${version}`, req.ip);
      res.json({ version });
    } catch (e) { next(e); }
  }

  return { get, put };
}

module.exports = { backupController };
