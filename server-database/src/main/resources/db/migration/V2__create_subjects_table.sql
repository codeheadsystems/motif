CREATE TABLE subjects (
  identifier_uuid UUID         NOT NULL PRIMARY KEY,
  category        VARCHAR(128) NOT NULL,
  value           VARCHAR(128) NOT NULL,
  UNIQUE (category, value)
);

CREATE INDEX idx_subjects_category ON subjects (category);
