'use strict';

function load(env = process.env) {
  const jwtSecret = env.JWT_SECRET || '';
  if (jwtSecret.length < 32) {
    throw new Error('JWT_SECRET must be set and at least 32 characters long');
  }
  if (!env.DATABASE_URL) {
    throw new Error('DATABASE_URL must be set');
  }
  return {
    port: parseInt(env.PORT || '8080', 10),
    databaseUrl: env.DATABASE_URL,
    jwtSecret,
    tokenTtl: '30d',
    singleOwner: (env.SINGLE_OWNER || 'true') === 'true',
    allowRegistration: (env.ALLOW_REGISTRATION || 'true') === 'true',
  };
}

module.exports = { load };
