-- Patterns are the output of the v1 frequency-based pattern detector.
-- Each row represents one (subject, normalized event-value) periodicity that the
-- detector flagged for an owner. Re-detection is idempotent: the unique constraint
-- lets the manager UPSERT, and a re-run that no longer satisfies the thresholds
-- DELETEs the row from the prior run.
CREATE TABLE patterns (
  uuid                    UUID         NOT NULL PRIMARY KEY,
  owner_uuid              UUID         NOT NULL REFERENCES owners (uuid) ON DELETE CASCADE,
  subject_uuid            UUID         NOT NULL REFERENCES subjects (uuid) ON DELETE CASCADE,
  -- Normalized form of Event.value (strip + lowercase). The same logical action
  -- regardless of capitalization or surrounding whitespace.
  event_value             VARCHAR(256) NOT NULL,
  -- DAILY / WEEKLY / MONTHLY / OTHER, written as the enum name.
  period_classification   VARCHAR(16)  NOT NULL,
  interval_mean_seconds   BIGINT       NOT NULL,
  occurrence_count        INTEGER      NOT NULL,
  -- 0..1, derived from coefficient of variation of inter-arrival intervals.
  confidence              DOUBLE PRECISION NOT NULL,
  last_seen_at            TIMESTAMPTZ  NOT NULL,
  next_expected_at        TIMESTAMPTZ  NOT NULL,
  -- Composite ranking (occurrences * confidence * recency). Higher is better.
  score                   DOUBLE PRECISION NOT NULL,
  detected_at             TIMESTAMPTZ  NOT NULL,
  UNIQUE (owner_uuid, subject_uuid, event_value)
);

CREATE INDEX idx_patterns_owner_score ON patterns (owner_uuid, score DESC);
