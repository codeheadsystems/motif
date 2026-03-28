CREATE TABLE tags (
  uuid UUID        NOT NULL,
  tag_value       VARCHAR(32) NOT NULL,
  PRIMARY KEY (uuid, tag_value)
);
