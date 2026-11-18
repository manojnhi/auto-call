'use strict';
const test = require('node:test');
const assert = require('node:assert');
const { validateRegister, validateLogin, validateBackup } = require('../src/validators');
const { load } = require('../src/config');

const goodBackup = JSON.stringify({ format: 'GramCall-AI-backup', schema: 3, tables: {} });

test('register: valid input', () => {
  const r = validateRegister({ shopName: ' Ram Kirana ', mobile: '9876543210', password: 'longenough' });
  assert.equal(r.ok, true);
  assert.equal(r.value.shopName, 'Ram Kirana');
});

test('register: rejects bad mobile / short password / missing name', () => {
  assert.equal(validateRegister({ shopName: 'x', mobile: '1234567890', password: 'longenough' }).ok, false);
  assert.equal(validateRegister({ shopName: 'x', mobile: '9876543210', password: 'short' }).ok, false);
  assert.equal(validateRegister({ shopName: '  ', mobile: '9876543210', password: 'longenough' }).ok, false);
  assert.equal(validateRegister(null).ok, false);
});

test('login: rejects non-string and malformed values', () => {
  assert.equal(validateLogin({ mobile: { $ne: 1 }, password: 'x' }).ok, false);
  assert.equal(validateLogin({ mobile: "9876543210' OR '1'='1", password: 'x' }).ok, false);
  assert.equal(validateLogin({ mobile: '9876543210', password: 'x' }).ok, true);
});

test('backup: accepts a GramCall snapshot', () => {
  assert.equal(validateBackup({ baseVersion: 0, payload: goodBackup }).ok, true);
});

test('backup: rejects bad version, non-JSON, wrong format, oversize', () => {
  assert.equal(validateBackup({ baseVersion: -1, payload: goodBackup }).ok, false);
  assert.equal(validateBackup({ baseVersion: 1.5, payload: goodBackup }).ok, false);
  assert.equal(validateBackup({ baseVersion: 0, payload: 'not json' }).ok, false);
  assert.equal(validateBackup({ baseVersion: 0, payload: JSON.stringify({ format: 'other', tables: {} }) }).ok, false);
  assert.equal(validateBackup({ baseVersion: 0, payload: 'x'.repeat(21 * 1024 * 1024) }).ok, false);
});

test('config: refuses weak or missing secrets', () => {
  assert.throws(() => load({ DATABASE_URL: 'postgres://x', JWT_SECRET: 'short' }));
  assert.throws(() => load({ JWT_SECRET: 'a'.repeat(32) }));
  assert.equal(load({ DATABASE_URL: 'postgres://x', JWT_SECRET: 'a'.repeat(32) }).singleOwner, true);
});
