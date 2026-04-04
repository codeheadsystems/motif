CREATE TABLE opaque_credentials (
  credential_id     BYTEA NOT NULL PRIMARY KEY,
  client_public_key BYTEA NOT NULL,
  masking_key       BYTEA NOT NULL,
  envelope_nonce    BYTEA NOT NULL,
  auth_tag          BYTEA NOT NULL,
  key_version       INT   NOT NULL DEFAULT 0
);
