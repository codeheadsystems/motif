package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Note;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class NoteDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

  private static Jdbi jdbi;
  private NoteDao noteDao;
  private EventDao eventDao;
  private SubjectDao subjectDao;
  private OwnerDao ownerDao;

  private Event event;

  @BeforeAll
  static void setupJdbi() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());

    Flyway.configure().dataSource(ds).load().migrate();

    jdbi = Jdbi.create(ds);
    jdbi.installPlugin(new SqlObjectPlugin());
  }

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM notes");
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    eventDao = jdbi.onDemand(EventDao.class);
    noteDao = jdbi.onDemand(NoteDao.class);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    storeSubject(SUBJECT);
    event = Event.builder().owner(OWNER).subject(SUBJECT).value("test-event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    storeEvent(event);
  }

  private void storeSubject(Subject subject) {
    subjectDao.upsert(
        subject.identifier().uuid(),
        subject.ownerIdentifier().uuid(),
        subject.category().value(),
        subject.value());
  }

  private void storeEvent(Event event) {
    eventDao.upsert(
        event.identifier().uuid(),
        event.ownerIdentifier().uuid(),
        event.subject().identifier().uuid(),
        event.value(),
        event.timestamp().toOffsetDateTime());
  }

  private void storeNote(Note note) {
    noteDao.upsert(
        note.identifier().uuid(),
        note.ownerIdentifier().uuid(),
        note.subjectIdentifier().uuid(),
        note.eventIdentifier() != null ? note.eventIdentifier().uuid() : null,
        note.value(),
        note.timestamp().toOffsetDateTime());
  }

  // --- upsert and find ---

  @Test
  void upsertAndFindByOwnerAndIdentifier() {
    Note note = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("a note")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();

    storeNote(note);
    Optional<Note> result = noteDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), note.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().ownerIdentifier().uuid()).isEqualTo(OWNER.identifier().uuid());
    assertThat(result.get().subjectIdentifier().uuid()).isEqualTo(SUBJECT.identifier().uuid());
    assertThat(result.get().eventIdentifier().uuid()).isEqualTo(event.identifier().uuid());
    assertThat(result.get().value()).isEqualTo("a note");
    assertThat(result.get().identifier().uuid()).isEqualTo(note.identifier().uuid());
    assertThat(result.get().timestamp()).isEqualTo(note.timestamp());
  }

  @Test
  void upsertAndFindNoteWithoutEvent() {
    Note note = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("no event note")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();

    storeNote(note);
    Optional<Note> result = noteDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), note.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().eventIdentifier()).isNull();
    assertThat(result.get().value()).isEqualTo("no event note");
  }

  @Test
  void findByOwnerAndIdentifierReturnsEmptyWhenNotFound() {
    assertThat(noteDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), new Identifier().uuid())).isEmpty();
  }

  @Test
  void upsertUpdatesExistingNote() {
    Note original = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("original")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    storeNote(original);

    Note updated = Note.from(original)
        .value("updated")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    storeNote(updated);

    Optional<Note> result = noteDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), original.identifier().uuid());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated");
    assertThat(result.get().timestamp().timestamp())
        .isEqualTo(Instant.parse("2026-03-28T12:00:00Z"));
  }

  // --- delete ---

  @Test
  void deleteReturnsOneWhenNoteExists() {
    Note note = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("to delete").build();
    storeNote(note);

    int deleted = noteDao.deleteByOwnerAndIdentifier(
        OWNER.identifier().uuid(), note.identifier().uuid());
    assertThat(deleted).isEqualTo(1);
    assertThat(noteDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), note.identifier().uuid())).isEmpty();
  }

  @Test
  void deleteReturnsZeroWhenNoteDoesNotExist() {
    assertThat(noteDao.deleteByOwnerAndIdentifier(
        OWNER.identifier().uuid(), new Identifier().uuid())).isEqualTo(0);
  }

  // --- findByOwnerAndSubject ---

  @Test
  void findByOwnerAndSubjectReturnsMatchingNotes() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    storeSubject(other);

    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("note 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("note 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    Note n3 = Note.builder().owner(OWNER).subjectIdentifier(other.identifier()).value("note 3")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:30:00Z")))
        .build();

    storeNote(n1);
    storeNote(n2);
    storeNote(n3);

    List<Note> results = noteDao.findByOwnerAndSubject(
        OWNER.identifier().uuid(), SUBJECT.identifier().uuid(), Integer.MAX_VALUE, 0);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Note::value)
        .containsExactly("note 1", "note 2");
  }

  @Test
  void findByOwnerAndSubjectReturnsEmptyWhenNoMatches() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "nonexistent");
    storeSubject(other);

    assertThat(noteDao.findByOwnerAndSubject(
        OWNER.identifier().uuid(), other.identifier().uuid(), Integer.MAX_VALUE, 0)).isEmpty();
  }

  // --- findByOwnerSubjectAndTimeRange ---

  @Test
  void findByOwnerSubjectAndTimeRangeFiltersBoth() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    storeSubject(other);

    Note match = Note.builder().owner(OWNER).subject(SUBJECT).value("match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Note wrongSubject = Note.builder().owner(OWNER).subjectIdentifier(other.identifier())
        .value("wrong subject")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Note wrongTime = Note.builder().owner(OWNER).subject(SUBJECT).value("wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z")))
        .build();

    storeNote(match);
    storeNote(wrongSubject);
    storeNote(wrongTime);

    List<Note> results = noteDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(),
        SUBJECT.identifier().uuid(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")).toOffsetDateTime(),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")).toOffsetDateTime(),
        Integer.MAX_VALUE, 0);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  @Test
  void findByOwnerSubjectAndTimeRangeReturnsOrderedByTimestamp() {
    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("second")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T14:00:00Z")))
        .build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("first")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    storeNote(n1);
    storeNote(n2);

    List<Note> results = noteDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(),
        SUBJECT.identifier().uuid(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")).toOffsetDateTime(),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")).toOffsetDateTime(),
        Integer.MAX_VALUE, 0);

    assertThat(results).extracting(Note::value)
        .containsExactly("first", "second");
  }

  // --- findByOwnerAndEvent ---

  @Test
  void findByOwnerAndEventReturnsMatchingNotes() {
    Event otherEvent = Event.builder().owner(OWNER).subject(SUBJECT).value("other-event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    storeEvent(otherEvent);

    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("note for event")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("another note for event")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Note n3 = Note.builder().owner(OWNER).subject(SUBJECT).value("note for other event")
        .event(otherEvent)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:30:00Z")))
        .build();

    storeNote(n1);
    storeNote(n2);
    storeNote(n3);

    List<Note> results = noteDao.findByOwnerAndEvent(
        OWNER.identifier().uuid(), event.identifier().uuid(), Integer.MAX_VALUE, 0);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Note::value)
        .containsExactly("note for event", "another note for event");
  }

  @Test
  void findByOwnerAndEventReturnsEmptyWhenNoMatches() {
    assertThat(noteDao.findByOwnerAndEvent(
        OWNER.identifier().uuid(), new Identifier().uuid(), Integer.MAX_VALUE, 0)).isEmpty();
  }

  // --- findByOwnerEventAndTimeRange ---

  @Test
  void findByOwnerEventAndTimeRangeFiltersBoth() {
    Note match = Note.builder().owner(OWNER).subject(SUBJECT).value("match")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Note wrongTime = Note.builder().owner(OWNER).subject(SUBJECT).value("wrong time")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z")))
        .build();

    storeNote(match);
    storeNote(wrongTime);

    List<Note> results = noteDao.findByOwnerEventAndTimeRange(
        OWNER.identifier().uuid(),
        event.identifier().uuid(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")).toOffsetDateTime(),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")).toOffsetDateTime(),
        Integer.MAX_VALUE, 0);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  @Test
  void findByOwnerEventAndTimeRangeReturnsOrderedByTimestamp() {
    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("second")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T14:00:00Z")))
        .build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("first")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    storeNote(n1);
    storeNote(n2);

    List<Note> results = noteDao.findByOwnerEventAndTimeRange(
        OWNER.identifier().uuid(),
        event.identifier().uuid(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")).toOffsetDateTime(),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")).toOffsetDateTime(),
        Integer.MAX_VALUE, 0);

    assertThat(results).extracting(Note::value)
        .containsExactly("first", "second");
  }

  // --- tags are not stored in notes table ---

  @Test
  void storedNoteHasEmptyTags() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("with tags").build();
    storeNote(note);

    Note retrieved = noteDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), note.identifier().uuid()).orElseThrow();

    assertThat(retrieved.tags()).isEmpty();
  }

  // --- owner isolation ---

  @Test
  void notesAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.upsert(other.identifier().uuid(), other.value(), false);
    Subject otherSubject = new Subject(other.identifier(), CATEGORY, "test-subject");
    storeSubject(otherSubject);

    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("owner note").build();
    Note n2 = Note.builder().owner(other).subjectIdentifier(otherSubject.identifier())
        .value("other note").build();

    storeNote(n1);
    storeNote(n2);

    assertThat(noteDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), n1.identifier().uuid())).isPresent();
    assertThat(noteDao.findByOwnerAndIdentifier(
        other.identifier().uuid(), n1.identifier().uuid())).isEmpty();
    assertThat(noteDao.findByOwnerAndIdentifier(
        other.identifier().uuid(), n2.identifier().uuid())).isPresent();
  }
}
