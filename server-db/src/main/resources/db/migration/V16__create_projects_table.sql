-- Projects (Premium tier): an initiative that groups Subjects across Categories.
-- A Subject's category is its taxonomy ("Plants"); its project is its goal/duration
-- ("Build vegetable garden"). Subjects optionally belong to 0 or 1 Projects.
CREATE TABLE projects (
  uuid        UUID         NOT NULL PRIMARY KEY,
  owner_uuid  UUID         NOT NULL REFERENCES owners (uuid) ON DELETE CASCADE,
  name        VARCHAR(128) NOT NULL,
  description VARCHAR(2048),
  status      VARCHAR(16)  NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL,
  updated_at  TIMESTAMPTZ  NOT NULL,
  UNIQUE (owner_uuid, name),
  CONSTRAINT projects_status_check
    CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED'))
);

CREATE INDEX idx_projects_owner_status ON projects (owner_uuid, status);

-- Subjects gain an optional project reference. ON DELETE SET NULL: deleting a Project
-- leaves its Subjects intact (they fall back to their Category-only home).
ALTER TABLE subjects
    ADD COLUMN project_uuid UUID REFERENCES projects (uuid) ON DELETE SET NULL;

CREATE INDEX idx_subjects_owner_project ON subjects (owner_uuid, project_uuid);
