package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.dao.NoteDao;
import com.codeheadsystems.motif.server.dao.OwnerDao;
import com.codeheadsystems.motif.server.dao.SubjectDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Note;
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
class OwnerManagerIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;
  private OwnerManager ownerManager;
  private SubjectDao subjectDao;
  private EventDao eventDao;
  private NoteDao noteDao;
  private TagsDao tagsDao;

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
    OwnerDao ownerDao = jdbi.onDemand(OwnerDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    eventDao = jdbi.onDemand(EventDao.class);
    noteDao = jdbi.onDemand(NoteDao.class);
    tagsDao = jdbi.onDemand(TagsDao.class);
    ownerManager = new OwnerManager(jdbi, ownerDao);
  }

  @Test
  void storeAndGetOwner() {
    Owner owner = new Owner("TEST-OWNER");
    ownerManager.store(owner);

    Optional<Owner> result = ownerManager.get(owner.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("TEST-OWNER");
    assertThat(result.get().deleted()).isFalse();
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(ownerManager.get(new Identifier())).isEmpty();
  }

  @Test
  void storeUpdatesExistingOwner() {
    Owner original = new Owner("ORIGINAL");
    ownerManager.store(original);

    Owner updated = new Owner("UPDATED", original.identifier(), false);
    ownerManager.store(updated);

    Optional<Owner> result = ownerManager.get(original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("UPDATED");
  }

  // --- soft delete ---

  @Test
  void softDeleteMarksOwnerAsDeleted() {
    Owner owner = new Owner("TO-DELETE");
    ownerManager.store(owner);

    assertThat(ownerManager.softDelete(owner.identifier())).isTrue();

    // Default get excludes soft-deleted
    assertThat(ownerManager.get(owner.identifier())).isEmpty();

    // Including soft-deleted finds it
    Optional<Owner> result = ownerManager.get(owner.identifier(), true);
    assertThat(result).isPresent();
    assertThat(result.get().deleted()).isTrue();
  }

  @Test
  void softDeleteReturnsFalseWhenOwnerDoesNotExist() {
    assertThat(ownerManager.softDelete(new Identifier())).isFalse();
  }

  @Test
  void softDeleteReturnsFalseWhenAlreadySoftDeleted() {
    Owner owner = new Owner("TO-DELETE");
    ownerManager.store(owner);

    assertThat(ownerManager.softDelete(owner.identifier())).isTrue();
    assertThat(ownerManager.softDelete(owner.identifier())).isFalse();
  }

  // --- hard delete ---

  @Test
  void hardDeleteCascadesAllData() {
    Owner owner = new Owner("CASCADE-OWNER");
    ownerManager.store(owner);

    Category category = new Category("test-cat");
    Subject subject = new Subject(owner.identifier(), category, "test-subject");
    subjectDao.upsert(subject.identifier().uuid(), subject.ownerIdentifier().uuid(),
        subject.category().value(), subject.value());

    Event event = Event.builder().owner(owner).subject(subject).value("test-event")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T10:00:00Z"))).build();
    eventDao.upsert(event.identifier().uuid(), event.ownerIdentifier().uuid(),
        event.subject().identifier().uuid(), event.value(),
        event.timestamp().toOffsetDateTime());
    tagsDao.insertTag(event.identifier().uuid(), "event-tag");

    Note note = Note.builder().owner(owner).subject(subject).event(event).value("test-note")
        .timestamp(new Timestamp(Instant.parse("2026-03-28T11:00:00Z"))).build();
    noteDao.upsert(note.identifier().uuid(), note.ownerIdentifier().uuid(),
        note.subjectIdentifier().uuid(), note.eventIdentifier().uuid(),
        note.value(), note.timestamp().toOffsetDateTime());
    tagsDao.insertTag(note.identifier().uuid(), "note-tag");

    // Must soft-delete first
    ownerManager.softDelete(owner.identifier());
    assertThat(ownerManager.hardDelete(owner.identifier())).isTrue();

    // Everything is gone
    assertThat(ownerManager.get(owner.identifier(), true)).isEmpty();
    assertThat(tagsDao.tagValuesFor(event.identifier().uuid())).isEmpty();
    assertThat(tagsDao.tagValuesFor(note.identifier().uuid())).isEmpty();
  }

  @Test
  void hardDeleteReturnsFalseWhenOwnerNotSoftDeleted() {
    Owner owner = new Owner("NOT-DELETED");
    ownerManager.store(owner);

    assertThat(ownerManager.hardDelete(owner.identifier())).isFalse();
    // Owner still exists
    assertThat(ownerManager.get(owner.identifier())).isPresent();
  }

  @Test
  void hardDeleteReturnsFalseWhenOwnerDoesNotExist() {
    assertThat(ownerManager.hardDelete(new Identifier())).isFalse();
  }

  // --- find ---

  @Test
  void findByValue() {
    Owner owner = new Owner("FINDABLE");
    ownerManager.store(owner);

    Optional<Owner> result = ownerManager.find("findable");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(owner.identifier().uuid());
  }

  @Test
  void findByValueExcludesSoftDeleted() {
    Owner owner = new Owner("FINDABLE");
    ownerManager.store(owner);
    ownerManager.softDelete(owner.identifier());

    assertThat(ownerManager.find("findable")).isEmpty();
    assertThat(ownerManager.find("findable", true)).isPresent();
  }

  @Test
  void findByValueReturnsEmptyWhenNotFound() {
    assertThat(ownerManager.find("NONEXISTENT")).isEmpty();
  }
}
