-- Promote Category from a string-on-Subject to a first-class entity with color and icon.
-- Single atomic migration so the schema is never observed in a half-migrated state.

-- 1. Categories table.
CREATE TABLE categories (
  uuid       UUID         NOT NULL PRIMARY KEY,
  owner_uuid UUID         NOT NULL REFERENCES owners (uuid) ON DELETE CASCADE,
  name       VARCHAR(128) NOT NULL,
  color      VARCHAR(7)   NOT NULL,
  icon       VARCHAR(64)  NOT NULL,
  UNIQUE (owner_uuid, name)
);

CREATE INDEX idx_categories_owner ON categories (owner_uuid);

-- 2. Backfill: one category row per distinct (owner_uuid, subject.category) pair.
--    Defaults: gray + tag icon. Users edit afterwards.
INSERT INTO categories (uuid, owner_uuid, name, color, icon)
SELECT gen_random_uuid(), distinct_pairs.owner_uuid, distinct_pairs.category, '#9CA3AF', 'tag'
FROM (SELECT DISTINCT owner_uuid, category FROM subjects) AS distinct_pairs;

-- 3. Add the FK column on subjects, nullable for now so the UPDATE can populate it.
ALTER TABLE subjects ADD COLUMN category_uuid UUID;

UPDATE subjects s
SET category_uuid = c.uuid
FROM categories c
WHERE c.owner_uuid = s.owner_uuid AND c.name = s.category;

-- 4. Lock it down: NOT NULL + FK with RESTRICT so categories cannot be deleted while
--    subjects still reference them. (The application enforces this with a 409.)
ALTER TABLE subjects ALTER COLUMN category_uuid SET NOT NULL;
ALTER TABLE subjects ADD CONSTRAINT fk_subjects_category
  FOREIGN KEY (category_uuid) REFERENCES categories (uuid) ON DELETE RESTRICT;

-- 5. Drop the old (owner_uuid, category, value) unique constraint and the old column.
ALTER TABLE subjects DROP CONSTRAINT subjects_owner_uuid_category_value_key;
ALTER TABLE subjects DROP COLUMN category;

-- 6. New uniqueness keyed by FK.
ALTER TABLE subjects ADD CONSTRAINT subjects_owner_uuid_category_uuid_value_key
  UNIQUE (owner_uuid, category_uuid, value);

-- 7. Swap the index for the new key shape.
-- (Postgres may have already dropped idx_subjects_owner_category as a side effect of dropping
-- the category column it indexed; use IF EXISTS to be safe across versions.)
DROP INDEX IF EXISTS idx_subjects_owner_category;
CREATE INDEX idx_subjects_owner_category_uuid ON subjects (owner_uuid, category_uuid);
