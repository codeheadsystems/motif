CREATE TABLE owners (
  uuid UUID         NOT NULL PRIMARY KEY,
  value           VARCHAR(256) NOT NULL UNIQUE
);
