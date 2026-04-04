package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.dao.EventDao;
import com.codeheadsystems.motif.server.db.dao.NoteDao;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Note;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteManagerIntegrationTest extends DatabaseTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

  private NoteManager noteManager;
  private Event event;
  private SubjectDao subjectDao;
  private EventDao eventDao;
  private OwnerDao ownerDao;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM tags");
      handle.execute("DELETE FROM notes");
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    eventDao = jdbi.onDemand(EventDao.class);
    NoteDao noteDao = jdbi.onDemand(NoteDao.class);
    TagsDao tagsDao = jdbi.onDemand(TagsDao.class);
    TagsManager tagsManager = new TagsManager(tagsDao);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    subjectDao.upsert(SUBJECT.identifier().uuid(), SUBJECT.ownerIdentifier().uuid(),
        SUBJECT.category().value(), SUBJECT.value());
    event = Event.builder().owner(OWNER).subject(SUBJECT).value("test-event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z"))).build();
    eventDao.upsert(event.identifier().uuid(), event.ownerIdentifier().uuid(),
        event.subject().identifier().uuid(), event.value(),
        event.timestamp().toOffsetDateTime());
    noteManager = new NoteManager(jdbi, noteDao, tagsManager);
  }

  // --- store and get ---

  @Test
  void storeAndGetNote() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).event(event).value("a note")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z"))).build();

    noteManager.store(note);

    Optional<Note> result = noteManager.get(OWNER, note.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("a note");
    assertThat(result.get().eventIdentifier().uuid()).isEqualTo(event.identifier().uuid());
  }

  @Test
  void storeNoteWithoutEvent() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("no event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z"))).build();

    noteManager.store(note);

    Optional<Note> result = noteManager.get(OWNER, note.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().eventIdentifier()).isNull();
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(noteManager.get(OWNER, new Identifier())).isEmpty();
  }

  // --- update ---

  @Test
  void updateExistingNote() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("original").build();
    noteManager.store(note);

    Note updated = Note.from(note).value("updated").build();
    noteManager.update(updated);

    Optional<Note> result = noteManager.get(OWNER, note.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated");
  }

  @Test
  void updateThrowsWhenNoteDoesNotExist() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("nonexistent").build();

    assertThatThrownBy(() -> noteManager.update(note))
        .isInstanceOf(NotFoundException.class);
  }

  // --- delete ---

  @Test
  void deleteExistingNote() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("to delete").build();
    noteManager.store(note);

    assertThat(noteManager.delete(OWNER, note.identifier())).isTrue();
    assertThat(noteManager.get(OWNER, note.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenNoteDoesNotExist() {
    assertThat(noteManager.delete(OWNER, new Identifier())).isFalse();
  }

  // --- findBySubject ---

  @Test
  void findBySubjectReturnsMatchingNotes() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    subjectDao.upsert(other.identifier().uuid(), other.ownerIdentifier().uuid(),
        other.category().value(), other.value());

    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("note 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z"))).build();
    Note n2 = Note.builder().owner(OWNER).subjectIdentifier(other.identifier()).value("note 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z"))).build();

    noteManager.store(n1);
    noteManager.store(n2);

    Page<Note> results = noteManager.findBySubject(OWNER, SUBJECT, PageRequest.first());
    assertThat(results.items()).hasSize(1);
    assertThat(results.items().getFirst().value()).isEqualTo("note 1");
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeFiltersBoth() {
    Note match = Note.builder().owner(OWNER).subject(SUBJECT).value("match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z"))).build();
    Note wrongTime = Note.builder().owner(OWNER).subject(SUBJECT).value("wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z"))).build();

    noteManager.store(match);
    noteManager.store(wrongTime);

    Page<Note> results = noteManager.findBySubjectAndTimeRange(OWNER, SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")),
        PageRequest.first());

    assertThat(results.items()).hasSize(1);
    assertThat(results.items().getFirst().value()).isEqualTo("match");
  }

  // --- findByEvent ---

  @Test
  void findByEventReturnsMatchingNotes() {
    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).event(event).value("for event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z"))).build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("no event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z"))).build();

    noteManager.store(n1);
    noteManager.store(n2);

    Page<Note> results = noteManager.findByEvent(OWNER, event.identifier(), PageRequest.first());
    assertThat(results.items()).hasSize(1);
    assertThat(results.items().getFirst().value()).isEqualTo("for event");
  }

  // --- findByEventAndTimeRange ---

  @Test
  void findByEventAndTimeRangeFiltersBoth() {
    Note match = Note.builder().owner(OWNER).subject(SUBJECT).event(event).value("match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z"))).build();
    Note wrongTime = Note.builder().owner(OWNER).subject(SUBJECT).event(event).value("wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z"))).build();

    noteManager.store(match);
    noteManager.store(wrongTime);

    Page<Note> results = noteManager.findByEventAndTimeRange(OWNER, event.identifier(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")),
        PageRequest.first());

    assertThat(results.items()).hasSize(1);
    assertThat(results.items().getFirst().value()).isEqualTo("match");
  }

  // --- owner isolation ---

  @Test
  void notesAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.upsert(other.identifier().uuid(), other.value(), false);
    Subject otherSubject = new Subject(other.identifier(), CATEGORY, "test-subject");
    subjectDao.upsert(otherSubject.identifier().uuid(), otherSubject.ownerIdentifier().uuid(),
        otherSubject.category().value(), otherSubject.value());

    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("owner note").build();
    Note n2 = Note.builder().owner(other).subjectIdentifier(otherSubject.identifier())
        .value("other note").build();

    noteManager.store(n1);
    noteManager.store(n2);

    assertThat(noteManager.get(OWNER, n1.identifier())).isPresent();
    assertThat(noteManager.get(other, n1.identifier())).isEmpty();
    assertThat(noteManager.get(other, n2.identifier())).isPresent();
  }
}
