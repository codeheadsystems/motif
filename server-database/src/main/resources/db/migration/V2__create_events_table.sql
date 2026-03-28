CREATE TABLE events (
  identifier_class  VARCHAR(128) NOT NULL,
  identifier_uuid   UUID         NOT NULL,
  subject_category  VARCHAR(128) NOT NULL,
  subject_value     VARCHAR(128) NOT NULL,
  value             VARCHAR(256) NOT NULL,
  timestamp         TIMESTAMPTZ  NOT NULL,
  PRIMARY KEY (identifier_class, identifier_uuid)
);

CREATE INDEX idx_events_subject ON events (subject_category, subject_value);
CREATE INDEX idx_events_timestamp ON events (timestamp);
CREATE INDEX idx_events_subject_timestamp ON events (subject_category, subject_value, timestamp);
