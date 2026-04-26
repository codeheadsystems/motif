-- Workflows (Premium tier): an ordered sequence of named steps. Each step optionally
-- has an expected duration (typical time between this step and the next, in seconds)
-- and a free-form notes field. A workflow is a recipe; tracking actual execution
-- (started/completed) is intentionally out of scope for v1 and lives in a later phase.
CREATE TABLE workflows (
  uuid        UUID         NOT NULL PRIMARY KEY,
  owner_uuid  UUID         NOT NULL REFERENCES owners (uuid) ON DELETE CASCADE,
  name        VARCHAR(128) NOT NULL,
  description VARCHAR(2048),
  created_at  TIMESTAMPTZ  NOT NULL,
  updated_at  TIMESTAMPTZ  NOT NULL,
  UNIQUE (owner_uuid, name)
);

CREATE INDEX idx_workflows_owner ON workflows (owner_uuid);

-- Steps belong to a single workflow; deleting a workflow cascades. Positions are 1..N
-- contiguous (the manager renumbers on save) and the unique constraint prevents
-- accidental duplicates from a bad save path.
CREATE TABLE workflow_steps (
  uuid                     UUID         NOT NULL PRIMARY KEY,
  workflow_uuid            UUID         NOT NULL REFERENCES workflows (uuid) ON DELETE CASCADE,
  position                 INTEGER      NOT NULL,
  name                     VARCHAR(128) NOT NULL,
  expected_duration_seconds BIGINT,
  notes                    VARCHAR(2048),
  UNIQUE (workflow_uuid, position),
  CONSTRAINT workflow_steps_position_check CHECK (position >= 1)
);

CREATE INDEX idx_workflow_steps_workflow ON workflow_steps (workflow_uuid, position);
