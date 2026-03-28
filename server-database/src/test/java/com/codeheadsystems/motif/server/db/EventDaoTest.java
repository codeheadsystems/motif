package com.codeheadsystems.motif.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Event;
import com.codeheadsystems.motif.model.Identifier;
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

  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(CATEGORY, "test-subject");

  private static Jdbi jdbi;
  private EventDao dao;

  @BeforeAll
  static void setupJdbi() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());

    Flyway.configure()
        .dataSource(ds)
        .load()
        .migrate();

    jdbi = Jdbi.create(ds);
    jdbi.installPlugin(new SqlObjectPlugin());
  }

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> handle.execute("DELETE FROM events"));
    dao = jdbi.onDemand(EventDao.class);
  }

  // --- store and get ---

  @Test
  void storeAndRetrieveEvent() {
    Event event = Event.builder(SUBJECT, "something happened")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    dao.store(event);
    Optional<Event> result = dao.get(event.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().subject()).isEqualTo(SUBJECT);
    assertThat(result.get().value()).isEqualTo("something happened");
    assertThat(result.get().identifier().uuid()).isEqualTo(event.identifier().uuid());
    assertThat(result.get().timestamp()).isEqualTo(event.timestamp());
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Optional<Event> result = dao.get(new Identifier(Event.class));

    assertThat(result).isEmpty();
  }

  @Test
  void storeUpdatesExistingEvent() {
    Event original = Event.builder(SUBJECT, "original value")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    dao.store(original);

    Event updated = Event.from(original)
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    // Rebuild with same identifier but new value via the builder workaround
    Event updatedEvent = new Event(SUBJECT, "updated value",
        original.identifier(), updated.timestamp(), null);
    dao.store(updatedEvent);

    Optional<Event> result = dao.get(original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated value");
    assertThat(result.get().timestamp().timestamp())
        .isEqualTo(Instant.parse("2026-03-28T12:00:00Z"));
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenEventExists() {
    Event event = Event.builder(SUBJECT, "to delete").build();
    dao.store(event);

    boolean deleted = dao.delete(event.identifier());

    assertThat(deleted).isTrue();
    assertThat(dao.get(event.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenEventDoesNotExist() {
    boolean deleted = dao.delete(new Identifier(Event.class));

    assertThat(deleted).isFalse();
  }

  // --- findBySubject ---

  @Test
  void findBySubjectReturnsMatchingEvents() {
    Subject other = new Subject(CATEGORY, "other-subject");

    Event e1 = Event.builder(SUBJECT, "event 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Event e2 = Event.builder(SUBJECT, "event 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z")))
        .build();
    Event e3 = Event.builder(other, "event 3")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:30:00Z")))
        .build();

    dao.store(e1);
    dao.store(e2);
    dao.store(e3);

    List<Event> results = dao.findBySubject(SUBJECT);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Event::value)
        .containsExactly("event 1", "event 2");
  }

  @Test
  void findBySubjectReturnsEmptyWhenNoMatches() {
    Subject other = new Subject(CATEGORY, "nonexistent");

    assertThat(dao.findBySubject(other)).isEmpty();
  }

  // --- findByTimeRange ---

  @Test
  void findByTimeRangeReturnsEventsInRange() {
    Event before = Event.builder(SUBJECT, "before")
        .timestamp(new Timestamp(Instant.parse("2026-03-27T09:00:00Z")))
        .build();
    Event inRange1 = Event.builder(SUBJECT, "in range 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();
    Event inRange2 = Event.builder(SUBJECT, "in range 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T15:00:00Z")))
        .build();
    Event after = Event.builder(SUBJECT, "after")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T09:00:00Z")))
        .build();

    dao.store(before);
    dao.store(inRange1);
    dao.store(inRange2);
    dao.store(after);

    List<Event> results = dao.findByTimeRange(
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Event::value)
        .containsExactly("in range 1", "in range 2");
  }

  @Test
  void findByTimeRangeIsInclusive() {
    Instant exact = Instant.parse("2026-03-28T12:00:00Z");
    Event event = Event.builder(SUBJECT, "exact")
        .timestamp(new Timestamp(exact))
        .build();
    dao.store(event);

    List<Event> results = dao.findByTimeRange(
        new Timestamp(exact), new Timestamp(exact));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("exact");
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeFiltersBoth() {
    Subject other = new Subject(CATEGORY, "other-subject");

    Event match = Event.builder(SUBJECT, "match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Event wrongSubject = Event.builder(other, "wrong subject")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z")))
        .build();
    Event wrongTime = Event.builder(SUBJECT, "wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z")))
        .build();

    dao.store(match);
    dao.store(wrongSubject);
    dao.store(wrongTime);

    List<Event> results = dao.findBySubjectAndTimeRange(SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  @Test
  void findBySubjectAndTimeRangeReturnsOrderedByTimestamp() {
    Event e1 = Event.builder(SUBJECT, "second")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T14:00:00Z")))
        .build();
    Event e2 = Event.builder(SUBJECT, "first")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    dao.store(e1);
    dao.store(e2);

    List<Event> results = dao.findBySubjectAndTimeRange(SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).extracting(Event::value)
        .containsExactly("first", "second");
  }

  // --- tags are ignored ---

  @Test
  void storedEventHasEmptyTags() {
    Event event = Event.builder(SUBJECT, "with tags").build();
    dao.store(event);

    Event retrieved = dao.get(event.identifier()).orElseThrow();

    assertThat(retrieved.tags()).isEmpty();
  }
}
