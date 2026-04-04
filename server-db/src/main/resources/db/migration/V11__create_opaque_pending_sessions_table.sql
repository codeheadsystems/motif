CREATE TABLE opaque_pending_sessions (
  session_token              VARCHAR(256) NOT NULL PRIMARY KEY,
  expected_client_mac        BYTEA        NOT NULL,
  session_key                BYTEA        NOT NULL,
  credential_identifier_b64  VARCHAR(512) NOT NULL,
  key_version                INT          NOT NULL DEFAULT 0,
  created_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pending_created ON opaque_pending_sessions (created_at);
