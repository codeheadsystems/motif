CREATE TABLE subjects (
  uuid UUID         NOT NULL PRIMARY KEY,
  owner_uuid      UUID         NOT NULL REFERENCES owners (uuid),
  category        VARCHAR(128) NOT NULL,
  value           VARCHAR(128) NOT NULL,
  UNIQUE (owner_uuid, category, value)
);

CREATE INDEX idx_subjects_owner ON subjects (owner_uuid);
CREATE INDEX idx_subjects_owner_category ON subjects (owner_uuid, category);
