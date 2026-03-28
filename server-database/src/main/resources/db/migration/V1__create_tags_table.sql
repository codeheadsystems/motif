CREATE TABLE tags (
  identifier_uuid UUID        NOT NULL,
  tag_value       VARCHAR(32) NOT NULL,
  PRIMARY KEY (identifier_uuid, tag_value)
);
