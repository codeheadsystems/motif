CREATE TABLE notes (
  uuid UUID         NOT NULL PRIMARY KEY,
  owner_uuid      UUID         NOT NULL REFERENCES owners (uuid),
  subject_uuid    UUID         NOT NULL REFERENCES subjects (uuid),
  event_uuid      UUID         REFERENCES events (uuid),
  value           VARCHAR(4096) NOT NULL,
  timestamp       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notes_owner ON notes (owner_uuid);
CREATE INDEX idx_notes_owner_subject ON notes (owner_uuid, subject_uuid);
CREATE INDEX idx_notes_owner_event ON notes (owner_uuid, event_uuid);
CREATE INDEX idx_notes_owner_timestamp ON notes (owner_uuid, timestamp);
CREATE INDEX idx_notes_owner_subject_timestamp ON notes (owner_uuid, subject_uuid, timestamp);
CREATE INDEX idx_notes_owner_event_timestamp ON notes (owner_uuid, event_uuid, timestamp);
