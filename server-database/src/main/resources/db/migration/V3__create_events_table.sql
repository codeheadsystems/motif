CREATE TABLE events (
  identifier_uuid UUID         NOT NULL PRIMARY KEY,
  subject_uuid    UUID         NOT NULL REFERENCES subjects (identifier_uuid),
  value           VARCHAR(256) NOT NULL,
  timestamp       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_events_subject ON events (subject_uuid);
CREATE INDEX idx_events_timestamp ON events (timestamp);
CREATE INDEX idx_events_subject_timestamp ON events (subject_uuid, timestamp);
