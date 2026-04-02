package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.dao.OwnerDao;
import com.codeheadsystems.motif.server.dao.SubjectDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Tag;
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
class EventManagerIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

  private static Jdbi jdbi;
  private EventManager eventManager;
  private OwnerDao ownerDao;
  private SubjectDao subjectDao;

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
      handle.execute("DELETE FROM tags");
      handle.execute("DELETE FROM notes");
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    EventDao eventDao = jdbi.onDemand(EventDao.class);
    TagsDao tagsDao = jdbi.onDemand(TagsDao.class);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value());
    subjectDao.upsert(
        SUBJECT.identifier().uuid(),
        SUBJECT.ownerIdentifier().uuid(),
        SUBJECT.category().value(),
        SUBJECT.value());
    eventManager = new EventManager(eventDao, tagsDao);
  }

  // --- store and get ---

  @Test
  void storeAndGetEvent() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("something happened")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z")))
        .build();

    eventManager.store(event);

    Optional<Event> result = eventManager.get(OWNER, event.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("something happened");
    assertThat(result.get().subject().identifier().uuid()).isEqualTo(SUBJECT.identifier().uuid());
  }

  @Test
  void storeEventWithTags() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("tagged event")
        .tags(List.of(new Tag("A"), new Tag("B")))
        .build();

    eventManager.store(event);

    Optional<Event> result = eventManager.get(OWNER, event.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("tagged event");
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(eventManager.get(OWNER, new Identifier())).isEmpty();
  }

  // --- update ---

  @Test
  void updateExistingEvent() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("original").build();
    eventManager.store(event);

    Event updated = Event.from(event).value("updated").build();
    assertThat(eventManager.update(updated)).isTrue();

    Optional<Event> result = eventManager.get(OWNER, event.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated");
  }

  @Test
  void updateReturnsFalseWhenEventDoesNotExist() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("nonexistent").build();

    assertThat(eventManager.update(event)).isFalse();
  }

  // --- delete ---

  @Test
  void deleteExistingEvent() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("to delete").build();
    eventManager.store(event);

    assertThat(eventManager.delete(OWNER, event.identifier())).isTrue();
    assertThat(eventManager.get(OWNER, event.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenEventDoesNotExist() {
    assertThat(eventManager.delete(OWNER, new Identifier())).isFalse();
  }

  // --- findBySubject ---

  @Test
  void findBySubjectReturnsMatchingEvents() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    subjectDao.upsert(other.identifier().uuid(), other.ownerIdentifier().uuid(),
        other.category().value(), other.value());

    Event e1 = Event.builder().owner(OWNER).subject(SUBJECT).value("event 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z"))).build();
    Event e2 = Event.builder().owner(OWNER).subject(SUBJECT).value("event 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z"))).build();
    Event e3 = Event.builder().owner(OWNER).subject(other).value("event 3")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:30:00Z"))).build();

    eventManager.store(e1);
    eventManager.store(e2);
    eventManager.store(e3);

    List<Event> results = eventManager.findBySubject(OWNER, SUBJECT);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Event::value).containsExactly("event 1", "event 2");
  }

  // --- findByTimeRange ---

  @Test
  void findByTimeRangeReturnsEventsInRange() {
    Event before = Event.builder().owner(OWNER).subject(SUBJECT).value("before")
        .timestamp(new Timestamp(Instant.parse("2026-03-27T09:00:00Z"))).build();
    Event inRange = Event.builder().owner(OWNER).subject(SUBJECT).value("in range")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z"))).build();
    Event after = Event.builder().owner(OWNER).subject(SUBJECT).value("after")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T09:00:00Z"))).build();

    eventManager.store(before);
    eventManager.store(inRange);
    eventManager.store(after);

    List<Event> results = eventManager.findByTimeRange(OWNER,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("in range");
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeFiltersBoth() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY, "other-subject");
    subjectDao.upsert(other.identifier().uuid(), other.ownerIdentifier().uuid(),
        other.category().value(), other.value());

    Event match = Event.builder().owner(OWNER).subject(SUBJECT).value("match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z"))).build();
    Event wrongSubject = Event.builder().owner(OWNER).subject(other).value("wrong subject")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z"))).build();
    Event wrongTime = Event.builder().owner(OWNER).subject(SUBJECT).value("wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z"))).build();

    eventManager.store(match);
    eventManager.store(wrongSubject);
    eventManager.store(wrongTime);

    List<Event> results = eventManager.findBySubjectAndTimeRange(OWNER, SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("match");
  }

  // --- owner isolation ---

  @Test
  void eventsAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.upsert(other.identifier().uuid(), other.value());
    Subject otherSubject = new Subject(other.identifier(), CATEGORY, "test-subject");
    subjectDao.upsert(otherSubject.identifier().uuid(), otherSubject.ownerIdentifier().uuid(),
        otherSubject.category().value(), otherSubject.value());

    Event e1 = Event.builder().owner(OWNER).subject(SUBJECT).value("owner event").build();
    Event e2 = Event.builder().owner(other).subject(otherSubject).value("other event").build();

    eventManager.store(e1);
    eventManager.store(e2);

    assertThat(eventManager.get(OWNER, e1.identifier())).isPresent();
    assertThat(eventManager.get(other, e1.identifier())).isEmpty();
    assertThat(eventManager.get(other, e2.identifier())).isPresent();
  }
}
