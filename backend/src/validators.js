'use strict';

const MOBILE = /^[6-9]\d{9}$/;
const MAX_PAYLOAD_CHARS = 20 * 1024 * 1024;
const BACKUP_FORMAT = 'GramCall-AI-backup';

function isObject(v) { return v !== null && typeof v === 'object' && !Array.isArray(v); }

function validateRegister(b) {
  if (!isObject(b)) return { ok: false, error: 'invalid body' };
  const shopName = typeof b.shopName === 'string' ? b.shopName.trim() : '';
  if (shopName.length < 1 || shopName.length > 100) return { ok: false, error: 'shopName required (max 100 chars)' };
  if (typeof b.mobile !== 'string' || !MOBILE.test(b.mobile)) return { ok: false, error: 'mobile must be a valid 10 digit number' };
  if (typeof b.password !== 'string' || b.password.length < 8 || b.password.length > 128) {
    return { ok: false, error: 'password must be 8 to 128 characters' };
  }
  return { ok: true, value: { shopName, mobile: b.mobile, password: b.password } };
}

function validateLogin(b) {
  if (!isObject(b)) return { ok: false, error: 'invalid body' };
  if (typeof b.mobile !== 'string' || !MOBILE.test(b.mobile)) return { ok: false, error: 'invalid mobile or password' };
  if (typeof b.password !== 'string' || b.password.length < 1 || b.password.length > 128) {
    return { ok: false, error: 'invalid mobile or password' };
  }
  return { ok: true, value: { mobile: b.mobile, password: b.password } };
}

function validateBackup(b) {
  if (!isObject(b)) return { ok: false, error: 'invalid body' };
  if (!Number.isInteger(b.baseVersion) || b.baseVersion < 0) return { ok: false, error: 'baseVersion must be an integer >= 0' };
  if (typeof b.payload !== 'string' || b.payload.length === 0) return { ok: false, error: 'payload must be a non-empty string' };
  if (b.payload.length > MAX_PAYLOAD_CHARS) return { ok: false, error: 'payload too large' };
  let parsed;
  try { parsed = JSON.parse(b.payload); } catch (e) { return { ok: false, error: 'payload is not valid JSON' }; }
  if (!isObject(parsed) || parsed.format !== BACKUP_FORMAT || !isObject(parsed.tables)) {
    return { ok: false, error: 'payload is not a GramCall backup' };
  }
  return { ok: true, value: { baseVersion: b.baseVersion, payload: b.payload } };
}

module.exports = { validateRegister, validateLogin, validateBackup, MOBILE, BACKUP_FORMAT };
