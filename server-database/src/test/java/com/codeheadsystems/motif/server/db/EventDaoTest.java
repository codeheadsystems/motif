package com.codeheadsystems.motif.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Event;
import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Owner;
import com.codeheadsystems.motif.model.Subject;
import com.codeheadsystems.motif.model.Timestamp;
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
  private static final Subject SUBJECT = new Subject(OWNER, CATEGORY, "test-subject");

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
    ownerDao.store(OWNER);
    subjectDao.store(SUBJECT);
  }

  // --- store and get ---

  @Test
  void storeAndRetrieveEvent() {
    Event event = Event.builder(OWNER, SUBJECT, "something happened")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    eventDao.store(event);
    Optional<Event> result = eventDao.get(OWNER, event.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().owner().identifier().uuid()).isEqualTo(OWNER.identifier().uuid());
    assertThat(result.get().subject().identifier().uuid()).isEqualTo(SUBJECT.identifier().uuid());
    assertThat(result.get().value()).isEqualTo("something happened");
    assertThat(result.get().identifier().uuid()).isEqualTo(event.identifier().uuid());
    assertThat(result.get().timestamp()).isEqualTo(event.timestamp());
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(eventDao.get(OWNER, new Identifier())).isEmpty();
  }

  @Test
  void storeUpdatesExistingEvent() {
    Event original = Event.builder(OWNER, SUBJECT, "original value")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    eventDao.store(original);

    Event updatedEvent = new Event(OWNER, SUBJECT, "updated value",
        original.identifier(),
        new Timestamp(Instant.parse("2026-03-28T12:00:00Z")),
        null);
    eventDao.store(updatedEvent);

    Optional<Event> result = eventDao.get(OWNER, original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated value");
    assertThat(result.get().timestamp().timestamp())
        .isEqualTo(Instant.parse("2026-03-28T12:00:00Z"));
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenEventExists() {
    Event event = Event.builder(OWNER, SUBJECT, "to delete").build();
    eventDao.store(event);

    assertThat(eventDao.delete(OWNER, event.identifier())).isTrue();
    assertThat(eventDao.get(OWNER, event.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenEventDoesNotExist() {
    assertThat(eventDao.delete(OWNER, new Identifier())).isFalse();
  }

  // --- findBySubject ---

  @Test
  void findBySubjectReturnsMatchingEvents() {
    Subject other = new Subject(OWNER, CATEGORY, "other-subject");
    subjectDao.store(other);

    Event e1 = Event.builder(OWNER, SUBJECT, "event 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Event e2 = Event.builder(OWNER, SUBJECT, "event 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    Event e3 = Event.builder(OWNER, other, "event 3")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:30:00Z")))
        .build();

    eventDao.store(e1);
    eventDao.store(e2);
    eventDao.store(e3);

    List<Event> results = eventDao.findBySubject(OWNER, SUBJECT);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Event::value)
        .containsExactly("event 1", "event 2");
  }

  @Test
  void findBySubjectReturnsEmptyWhenNoMatches() {
    Subject other = new Subject(OWNER, CATEGORY, "nonexistent");
    subjectDao.store(other);

    assertThat(eventDao.findBySubject(OWNER, other)).isEmpty();
  }

  // --- findByTimeRange ---

  @Test
  void findByTimeRangeReturnsEventsInRange() {
    Event before = Event.builder(OWNER, SUBJECT, "before")
        .timestamp(new Timestamp(Instant.parse("2026-03-27T09:00:00Z")))
        .build();
    Event inRange1 = Event.builder(OWNER, SUBJECT, "in range 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Event inRange2 = Event.builder(OWNER, SUBJECT, "in range 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T15:00:00Z")))
        .build();
    Event after = Event.builder(OWNER, SUBJECT, "after")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T09:00:00Z")))
        .build();

    eventDao.store(before);
    eventDao.store(inRange1);
    eventDao.store(inRange2);
    eventDao.store(after);

    List<Event> results = eventDao.findByTimeRange(OWNER,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Event::value)
        .containsExactly("in range 1", "in range 2");
  }

  @Test
  void findByTimeRangeIsInclusive() {
    Instant exact = Instant.parse("2026-03-28T12:00:00Z");
    Event event = Event.builder(OWNER, SUBJECT, "exact")
        .timestamp(new Timestamp(exact))
        .build();
    eventDao.store(event);

    List<Event> results = eventDao.findByTimeRange(OWNER,
        new Timestamp(exact), new Timestamp(exact));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("exact");
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeFiltersBoth() {
    Subject other = new Subject(OWNER, CATEGORY, "other-subject");
    subjectDao.store(other);

    Event match = Event.builder(OWNER, SUBJECT, "match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Event wrongSubject = Event.builder(OWNER, other, "wrong subject")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Event wrongTime = Event.builder(OWNER, SUBJECT, "wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z")))
        .build();

    eventDao.store(match);
    eventDao.store(wrongSubject);
    eventDao.store(wrongTime);

    List<Event> results = eventDao.findBySubjectAndTimeRange(OWNER, SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  @Test
  void findBySubjectAndTimeRangeReturnsOrderedByTimestamp() {
    Event e1 = Event.builder(OWNER, SUBJECT, "second")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T14:00:00Z")))
        .build();
    Event e2 = Event.builder(OWNER, SUBJECT, "first")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    eventDao.store(e1);
    eventDao.store(e2);

    List<Event> results = eventDao.findBySubjectAndTimeRange(OWNER, SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).extracting(Event::value)
        .containsExactly("first", "second");
  }

  // --- tags are ignored ---

  @Test
  void storedEventHasEmptyTags() {
    Event event = Event.builder(OWNER, SUBJECT, "with tags").build();
    eventDao.store(event);

    Event retrieved = eventDao.get(OWNER, event.identifier()).orElseThrow();

    assertThat(retrieved.tags()).isEmpty();
  }

  // --- owner isolation ---

  @Test
  void eventsAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.store(other);
    Subject otherSubject = new Subject(other, CATEGORY, "test-subject");
    subjectDao.store(otherSubject);

    Event e1 = Event.builder(OWNER, SUBJECT, "owner event").build();
    Event e2 = Event.builder(other, otherSubject, "other event").build();

    eventDao.store(e1);
    eventDao.store(e2);

    assertThat(eventDao.get(OWNER, e1.identifier())).isPresent();
    assertThat(eventDao.get(other, e1.identifier())).isEmpty();
    assertThat(eventDao.get(other, e2.identifier())).isPresent();
  }
}
