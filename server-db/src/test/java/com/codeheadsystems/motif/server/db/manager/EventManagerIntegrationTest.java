package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.TestCategories;
import com.codeheadsystems.motif.server.db.dao.CategoryDao;
import com.codeheadsystems.motif.server.db.dao.EventDao;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Tag;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventManagerIntegrationTest extends DatabaseTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = TestCategories.of(OWNER.identifier(), "test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY.identifier(), "test-subject");

  private EventManager eventManager;
  private OwnerDao ownerDao;
  private SubjectDao subjectDao;
  private CategoryDao categoryDao;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM tags");
      handle.execute("DELETE FROM notes");
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM categories");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    categoryDao = jdbi.onDemand(CategoryDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    EventDao eventDao = jdbi.onDemand(EventDao.class);
    TagsDao tagsDao = jdbi.onDemand(TagsDao.class);
    TagsManager tagsManager = new TagsManager(tagsDao);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    storeCategory(CATEGORY);
    storeSubject(SUBJECT);
    eventManager = new EventManager(jdbi, eventDao, tagsManager);
  }

  private void storeCategory(Category c) {
    categoryDao.upsert(c.identifier().uuid(), c.ownerIdentifier().uuid(), c.name(), c.color(), c.icon());
  }

  private void storeSubject(Subject s) {
    subjectDao.upsert(s.identifier().uuid(), s.ownerIdentifier().uuid(), s.categoryIdentifier().uuid(), s.value());
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
    eventManager.update(updated);

    Optional<Event> result = eventManager.get(OWNER, event.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated");
  }

  @Test
  void updateThrowsWhenEventDoesNotExist() {
    Event event = Event.builder().owner(OWNER).subject(SUBJECT).value("nonexistent").build();

    assertThatThrownBy(() -> eventManager.update(event))
        .isInstanceOf(NotFoundException.class);
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
    Subject other = new Subject(OWNER.identifier(), CATEGORY.identifier(), "other-subject");
    storeSubject(other);

    Event e1 = Event.builder().owner(OWNER).subject(SUBJECT).value("event 1")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z"))).build();
    Event e2 = Event.builder().owner(OWNER).subject(SUBJECT).value("event 2")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z"))).build();
    Event e3 = Event.builder().owner(OWNER).subject(other).value("event 3")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:30:00Z"))).build();

    eventManager.store(e1);
    eventManager.store(e2);
    eventManager.store(e3);

    Page<Event> results = eventManager.findBySubject(OWNER, SUBJECT, PageRequest.first());

    assertThat(results.items()).hasSize(2);
    assertThat(results.items()).extracting(Event::value).containsExactly("event 1", "event 2");
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

    Page<Event> results = eventManager.findByTimeRange(OWNER,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")),
        PageRequest.first());

    assertThat(results.items()).hasSize(1);
    assertThat(results.items().getFirst().value()).isEqualTo("in range");
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeFiltersBoth() {
    Subject other = new Subject(OWNER.identifier(), CATEGORY.identifier(), "other-subject");
    storeSubject(other);

    Event match = Event.builder().owner(OWNER).subject(SUBJECT).value("match")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z"))).build();
    Event wrongSubject = Event.builder().owner(OWNER).subject(other).value("wrong subject")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T12:00:00Z"))).build();
    Event wrongTime = Event.builder().owner(OWNER).subject(SUBJECT).value("wrong time")
        .timestamp(new Timestamp(Instant.parse("2026-03-29T12:00:00Z"))).build();

    eventManager.store(match);
    eventManager.store(wrongSubject);
    eventManager.store(wrongTime);

    Page<Event> results = eventManager.findBySubjectAndTimeRange(OWNER, SUBJECT,
        new Timestamp(Instant.parse("2026-03-28T00:00:00Z")),
        new Timestamp(Instant.parse("2026-03-28T23:59:59Z")),
        PageRequest.first());

    assertThat(results.items()).hasSize(1);
    assertThat(results.items().getFirst().value()).isEqualTo("match");
  }

  // --- owner isolation ---

  @Test
  void eventsAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.upsert(other.identifier().uuid(), other.value(), false);
    Category otherCategory = TestCategories.of(other.identifier(), "test-category");
    storeCategory(otherCategory);
    Subject otherSubject = new Subject(other.identifier(), otherCategory.identifier(), "test-subject");
    storeSubject(otherSubject);

    Event e1 = Event.builder().owner(OWNER).subject(SUBJECT).value("owner event").build();
    Event e2 = Event.builder().owner(other).subject(otherSubject).value("other event").build();

    eventManager.store(e1);
    eventManager.store(e2);

    assertThat(eventManager.get(OWNER, e1.identifier())).isPresent();
    assertThat(eventManager.get(other, e1.identifier())).isEmpty();
    assertThat(eventManager.get(other, e2.identifier())).isPresent();
  }
}
