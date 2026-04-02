package com.codeheadsystems.motif.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
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
class EventDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

  private static Jdbi jdbi;
  private EventDao eventDao;
  private SubjectDao subjectDao;
  private OwnerDao ownerDao;

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
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    eventDao = jdbi.onDemand(EventDao.class);
    storeOwner(OWNER);
    storeSubject(SUBJECT);
  }

  private void storeOwner(Owner owner) {
    ownerDao.upsert(owner.identifier().uuid(), owner.value());
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

  // --- upsert and find ---

  @Test
  void upsertAndFindByOwnerAndIdentifier() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("something happened")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    storeEvent(event);
    Optional<Event> result = eventDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), event.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().ownerIdentifier().uuid()).isEqualTo(OWNER.identifier().uuid());
    assertThat(result.get().subject().identifier().uuid()).isEqualTo(SUBJECT.identifier().uuid());
    assertThat(result.get().value()).isEqualTo("something happened");
    assertThat(result.get().identifier().uuid()).isEqualTo(event.identifier().uuid());
    assertThat(result.get().timestamp()).isEqualTo(event.timestamp());
  }

  @Test
  void findByOwnerAndIdentifierReturnsEmptyWhenNotFound() {
    assertThat(eventDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), new Identifier().uuid())).isEmpty();
  }

  @Test
  void upsertUpdatesExistingEvent() {
    Event original = Event.builder().owner(OWNER).subject(SUBJECT).value("original value")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    storeEvent(original);

    Event updatedEvent = new Event(OWNER.identifier(), SUBJECT, "updated value",
        original.identifier(),
        new Timestamp(Instant.parse("2026-03-28T12:00:00Z")),
        null);
    storeEvent(updatedEvent);

    Optional<Event> result = eventDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), original.identifier().uuid());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated value");
    assertThat(result.get().timestamp().timestamp())
        .isEqualTo(Instant.parse("2026-03-28T12:00:00Z"));
  }

  // --- delete ---

  @Test
  void deleteReturnsOneWhenEventExists() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("to delete").build();
    storeEvent(event);

    int deleted = eventDao.deleteByOwnerAndIdentifier(
        OWNER.identifier().uuid(), event.identifier().uuid());
    assertThat(deleted).isEqualTo(1);
    assertThat(eventDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), event.identifier().uuid())).isEmpty();
  }

  @Test
  void deleteReturnsZeroWhenEventDoesNotExist() {
    assertThat(eventDao.deleteByOwnerAndIdentifier(
        OWNER.identifier().uuid(), new Identifier().uuid())).isEqualTo(0);
  }

  // --- findByOwnerAndSubject ---

  @Test
  void findByOwnerAndSubjectReturnsMatchingEvents() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    storeSubject(other);

    Event e1 = Event.builder().owner(OWNER).subject(SUBJECT).value("event 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Event e2 = Event.builder().owner(OWNER).subject(SUBJECT).value("event 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    Event e3 = Event.builder().owner(OWNER).subject(other).value("event 3")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:30:00Z")))
        .build();

    storeEvent(e1);
    storeEvent(e2);
    storeEvent(e3);

    List<Event> results = eventDao.findByOwnerAndSubject(
        OWNER.identifier().uuid(), SUBJECT.identifier().uuid());

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Event::value)
        .containsExactly("event 1", "event 2");
  }

  @Test
  void findByOwnerAndSubjectReturnsEmptyWhenNoMatches() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "nonexistent");
    storeSubject(other);

    assertThat(eventDao.findByOwnerAndSubject(
        OWNER.identifier().uuid(), other.identifier().uuid())).isEmpty();
  }

  // --- findByOwnerAndTimeRange ---

  @Test
  void findByOwnerAndTimeRangeReturnsEventsInRange() {
    Event before = Event.builder().owner(OWNER).subject(SUBJECT).value("before")
        .timestamp(new Timestamp(Instant.parse("2026-03-27T09:00:00Z")))
        .build();
    Event inRange1 = Event.builder().owner(OWNER).subject(SUBJECT).value("in range 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Event inRange2 = Event.builder().owner(OWNER).subject(SUBJECT).value("in range 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T15:00:00Z")))
        .build();
    Event after = Event.builder().owner(OWNER).subject(SUBJECT).value("after")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T09:00:00Z")))
        .build();

    storeEvent(before);
    storeEvent(inRange1);
    storeEvent(inRange2);
    storeEvent(after);

    List<Event> results = eventDao.findByOwnerAndTimeRange(
        OWNER.identifier().uuid(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")).toOffsetDateTime(),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")).toOffsetDateTime());

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Event::value)
        .containsExactly("in range 1", "in range 2");
  }

  @Test
  void findByOwnerAndTimeRangeIsInclusive() {
    Instant exact = Instant.parse("2026-03-28T12:00:00Z");
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("exact")
        .timestamp(new Timestamp(exact))
        .build();
    storeEvent(event);

    Timestamp ts = new Timestamp(exact);
    List<Event> results = eventDao.findByOwnerAndTimeRange(
        OWNER.identifier().uuid(), ts.toOffsetDateTime(), ts.toOffsetDateTime());

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("exact");
  }

  // --- findByOwnerSubjectAndTimeRange ---

  @Test
  void findByOwnerSubjectAndTimeRangeFiltersBoth() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    storeSubject(other);

    Event match = Event.builder().owner(OWNER).subject(SUBJECT).value("match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Event wrongSubject = Event.builder().owner(OWNER).subject(other).value("wrong subject")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Event wrongTime = Event.builder().owner(OWNER).subject(SUBJECT).value("wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z")))
        .build();

    storeEvent(match);
    storeEvent(wrongSubject);
    storeEvent(wrongTime);

    List<Event> results = eventDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(),
        SUBJECT.identifier().uuid(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")).toOffsetDateTime(),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")).toOffsetDateTime());

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  @Test
  void findByOwnerSubjectAndTimeRangeReturnsOrderedByTimestamp() {
    Event e1 = Event.builder().owner(OWNER).subject(SUBJECT).value("second")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T14:00:00Z")))
        .build();
    Event e2 = Event.builder().owner(OWNER).subject(SUBJECT).value("first")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    storeEvent(e1);
    storeEvent(e2);

    List<Event> results = eventDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(),
        SUBJECT.identifier().uuid(),
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")).toOffsetDateTime(),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")).toOffsetDateTime());

    assertThat(results).extracting(Event::value)
        .containsExactly("first", "second");
  }

  // --- tags are ignored ---

  @Test
  void storedEventHasEmptyTags() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("with tags").build();
    storeEvent(event);

    Event retrieved = eventDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), event.identifier().uuid()).orElseThrow();

    assertThat(retrieved.tags()).isEmpty();
  }

  // --- owner isolation ---

  @Test
  void eventsAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    storeOwner(other);
    Subject otherSubject = new Subject(other.identifier(), CATEGORY, "test-subject");
    storeSubject(otherSubject);

    Event e1 = Event.builder().owner(OWNER).subject(SUBJECT).value("owner event").build();
    Event e2 = Event.builder().owner(other).subject(otherSubject).value("other event").build();

    storeEvent(e1);
    storeEvent(e2);

    assertThat(eventDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), e1.identifier().uuid())).isPresent();
    assertThat(eventDao.findByOwnerAndIdentifier(
        other.identifier().uuid(), e1.identifier().uuid())).isEmpty();
    assertThat(eventDao.findByOwnerAndIdentifier(
        other.identifier().uuid(), e2.identifier().uuid())).isPresent();
  }
}
