-- Add ON DELETE CASCADE to all foreign keys so the database handles
-- cascade deletes automatically when an owner or parent entity is removed.

-- subjects -> owners
ALTER TABLE subjects DROP CONSTRAINT subjects_owner_uuid_fkey;
ALTER TABLE subjects ADD CONSTRAINT subjects_owner_uuid_fkey
  FOREIGN KEY (owner_uuid) REFERENCES owners(uuid) ON DELETE CASCADE;

-- events -> owners
ALTER TABLE events DROP CONSTRAINT events_owner_uuid_fkey;
ALTER TABLE events ADD CONSTRAINT events_owner_uuid_fkey
  FOREIGN KEY (owner_uuid) REFERENCES owners(uuid) ON DELETE CASCADE;

-- events -> subjects
ALTER TABLE events DROP CONSTRAINT events_subject_uuid_fkey;
ALTER TABLE events ADD CONSTRAINT events_subject_uuid_fkey
  FOREIGN KEY (subject_uuid) REFERENCES subjects(uuid) ON DELETE CASCADE;

-- notes -> owners
ALTER TABLE notes DROP CONSTRAINT notes_owner_uuid_fkey;
ALTER TABLE notes ADD CONSTRAINT notes_owner_uuid_fkey
  FOREIGN KEY (owner_uuid) REFERENCES owners(uuid) ON DELETE CASCADE;

-- notes -> subjects
ALTER TABLE notes DROP CONSTRAINT notes_subject_uuid_fkey;
ALTER TABLE notes ADD CONSTRAINT notes_subject_uuid_fkey
  FOREIGN KEY (subject_uuid) REFERENCES subjects(uuid) ON DELETE CASCADE;

-- notes -> events
ALTER TABLE notes DROP CONSTRAINT notes_event_uuid_fkey;
ALTER TABLE notes ADD CONSTRAINT notes_event_uuid_fkey
  FOREIGN KEY (event_uuid) REFERENCES events(uuid) ON DELETE CASCADE;
