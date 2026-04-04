CREATE TABLE opaque_sessions (
  jti                   VARCHAR(256) NOT NULL PRIMARY KEY,
  credential_identifier VARCHAR(512) NOT NULL,
  session_key           VARCHAR(512) NOT NULL,
  issued_at             TIMESTAMPTZ  NOT NULL,
  expires_at            TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_opaque_sessions_cred ON opaque_sessions (credential_identifier);
CREATE INDEX idx_opaque_sessions_exp ON opaque_sessions (expires_at);
