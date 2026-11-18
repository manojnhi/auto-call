CREATE TABLE IF NOT EXISTS owners (
  id            BIGSERIAL PRIMARY KEY,
  shop_name     TEXT        NOT NULL,
  mobile        TEXT        NOT NULL UNIQUE,
  password_hash TEXT        NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Ek owner ka ek hi snapshot; version se takraav (conflict) pakda jaata hai.
CREATE TABLE IF NOT EXISTS backups (
  owner_id   BIGINT PRIMARY KEY REFERENCES owners(id) ON DELETE CASCADE,
  version    INTEGER     NOT NULL,
  payload    TEXT        NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id         BIGSERIAL PRIMARY KEY,
  owner_id   BIGINT,
  action     TEXT        NOT NULL,
  detail     TEXT,
  ip         TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_owner ON audit_logs(owner_id, created_at DESC);
