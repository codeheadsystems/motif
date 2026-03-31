CREATE TABLE events (
  uuid UUID         NOT NULL PRIMARY KEY,
  owner_uuid      UUID         NOT NULL REFERENCES owners (uuid),
  subject_uuid    UUID         NOT NULL REFERENCES subjects (uuid),
  value           VARCHAR(256) NOT NULL,
  timestamp       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_events_owner ON events (owner_uuid);
CREATE INDEX idx_events_owner_subject ON events (owner_uuid, subject_uuid);
CREATE INDEX idx_events_owner_timestamp ON events (owner_uuid, timestamp);
CREATE INDEX idx_events_owner_subject_timestamp ON events (owner_uuid, subject_uuid, timestamp);
