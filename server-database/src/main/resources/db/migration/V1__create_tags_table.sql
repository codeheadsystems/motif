CREATE TABLE tags (
  identifier_class VARCHAR(128) NOT NULL,
  identifier_uuid  UUID         NOT NULL,
  tag_value        VARCHAR(32)  NOT NULL,
  PRIMARY KEY (identifier_class, identifier_uuid, tag_value)
);

CREATE INDEX idx_tags_identifier ON tags (identifier_class, identifier_uuid);
