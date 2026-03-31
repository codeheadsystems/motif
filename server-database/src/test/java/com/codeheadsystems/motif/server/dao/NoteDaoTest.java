package com.codeheadsystems.motif.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Note;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Timestamp;
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
    ownerDao.store(OWNER);
    subjectDao.store(SUBJECT);
    event = Event.builder().owner(OWNER).subject(SUBJECT).value("test-event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    eventDao.store(event);
  }

  // --- store and get ---

  @Test
  void storeAndRetrieveNote() {
    Note note = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("a note")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();

    noteDao.store(note);
    Optional<Note> result = noteDao.get(OWNER, note.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().ownerIdentifier().uuid()).isEqualTo(OWNER.identifier().uuid());
    assertThat(result.get().subjectIdentifier().uuid()).isEqualTo(SUBJECT.identifier().uuid());
    assertThat(result.get().eventIdentifier().uuid()).isEqualTo(event.identifier().uuid());
    assertThat(result.get().value()).isEqualTo("a note");
    assertThat(result.get().identifier().uuid()).isEqualTo(note.identifier().uuid());
    assertThat(result.get().timestamp()).isEqualTo(note.timestamp());
  }

  @Test
  void storeAndRetrieveNoteWithoutEvent() {
    Note note = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("no event note")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();

    noteDao.store(note);
    Optional<Note> result = noteDao.get(OWNER, note.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().eventIdentifier()).isNull();
    assertThat(result.get().value()).isEqualTo("no event note");
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(noteDao.get(OWNER, new Identifier())).isEmpty();
  }

  @Test
  void storeUpdatesExistingNote() {
    Note original = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("original")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    noteDao.store(original);

    Note updated = Note.from(original)
        .value("updated")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    noteDao.store(updated);

    Optional<Note> result = noteDao.get(OWNER, original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated");
    assertThat(result.get().timestamp().timestamp())
        .isEqualTo(Instant.parse("2026-03-28T12:00:00Z"));
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenNoteExists() {
    Note note = Note.builder()
        .owner(OWNER).subject(SUBJECT).value("to delete").build();
    noteDao.store(note);

    assertThat(noteDao.delete(OWNER, note.identifier())).isTrue();
    assertThat(noteDao.get(OWNER, note.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenNoteDoesNotExist() {
    assertThat(noteDao.delete(OWNER, new Identifier())).isFalse();
  }

  // --- findBySubject ---

  @Test
  void findBySubjectReturnsMatchingNotes() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    subjectDao.store(other);

    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("note 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("note 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    Note n3 = Note.builder().owner(OWNER).subjectIdentifier(other.identifier()).value("note 3")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:30:00Z")))
        .build();

    noteDao.store(n1);
    noteDao.store(n2);
    noteDao.store(n3);

    List<Note> results = noteDao.findBySubject(OWNER, SUBJECT);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Note::value)
        .containsExactly("note 1", "note 2");
  }

  @Test
  void findBySubjectReturnsEmptyWhenNoMatches() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "nonexistent");
    subjectDao.store(other);

    assertThat(noteDao.findBySubject(OWNER, other)).isEmpty();
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeFiltersBoth() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    subjectDao.store(other);

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

    noteDao.store(match);
    noteDao.store(wrongSubject);
    noteDao.store(wrongTime);

    List<Note> results = noteDao.findBySubjectAndTimeRange(OWNER, SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  @Test
  void findBySubjectAndTimeRangeReturnsOrderedByTimestamp() {
    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("second")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T14:00:00Z")))
        .build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("first")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    noteDao.store(n1);
    noteDao.store(n2);

    List<Note> results = noteDao.findBySubjectAndTimeRange(OWNER, SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).extracting(Note::value)
        .containsExactly("first", "second");
  }

  // --- findByEvent ---

  @Test
  void findByEventReturnsMatchingNotes() {
    Event otherEvent = Event.builder().owner(OWNER).subject(SUBJECT).value("other-event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    eventDao.store(otherEvent);

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

    noteDao.store(n1);
    noteDao.store(n2);
    noteDao.store(n3);

    List<Note> results = noteDao.findByEvent(OWNER, event.identifier());

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Note::value)
        .containsExactly("note for event", "another note for event");
  }

  @Test
  void findByEventReturnsEmptyWhenNoMatches() {
    assertThat(noteDao.findByEvent(OWNER, new Identifier())).isEmpty();
  }

  // --- findByEventAndTimeRange ---

  @Test
  void findByEventAndTimeRangeFiltersBoth() {
    Note match = Note.builder().owner(OWNER).subject(SUBJECT).value("match")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Note wrongTime = Note.builder().owner(OWNER).subject(SUBJECT).value("wrong time")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z")))
        .build();

    noteDao.store(match);
    noteDao.store(wrongTime);

    List<Note> results = noteDao.findByEventAndTimeRange(OWNER, event.identifier(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  @Test
  void findByEventAndTimeRangeReturnsOrderedByTimestamp() {
    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("second")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T14:00:00Z")))
        .build();
    Note n2 = Note.builder().owner(OWNER).subject(SUBJECT).value("first")
        .event(event)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    noteDao.store(n1);
    noteDao.store(n2);

    List<Note> results = noteDao.findByEventAndTimeRange(OWNER, event.identifier(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).extracting(Note::value)
        .containsExactly("first", "second");
  }

  // --- tags are not stored in notes table ---

  @Test
  void storedNoteHasEmptyTags() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("with tags").build();
    noteDao.store(note);

    Note retrieved = noteDao.get(OWNER, note.identifier()).orElseThrow();

    assertThat(retrieved.tags()).isEmpty();
  }

  // --- owner isolation ---

  @Test
  void notesAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.store(other);
    Subject otherSubject = new Subject(other.identifier(), CATEGORY, "test-subject");
    subjectDao.store(otherSubject);

    Note n1 = Note.builder().owner(OWNER).subject(SUBJECT).value("owner note").build();
    Note n2 = Note.builder().owner(other).subjectIdentifier(otherSubject.identifier())
        .value("other note").build();

    noteDao.store(n1);
    noteDao.store(n2);

    assertThat(noteDao.get(OWNER, n1.identifier())).isPresent();
    assertThat(noteDao.get(other, n1.identifier())).isEmpty();
    assertThat(noteDao.get(other, n2.identifier())).isPresent();
  }
}
