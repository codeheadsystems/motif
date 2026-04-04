package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
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
class SubjectDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");

  private static Jdbi jdbi;
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
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
  }

  private void storeSubject(Subject subject) {
    subjectDao.upsert(
        subject.identifier().uuid(),
        subject.ownerIdentifier().uuid(),
        subject.category().value(),
        subject.value());
  }

  // --- upsert and find ---

  @Test
  void upsertAndFindByOwnerAndIdentifier() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

    storeSubject(subject);
    Optional<Subject> result = subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), subject.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().ownerIdentifier().uuid()).isEqualTo(OWNER.identifier().uuid());
    assertThat(result.get().category()).isEqualTo(CATEGORY);
    assertThat(result.get().value()).isEqualTo("test-subject");
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void findByOwnerAndIdentifierReturnsEmptyWhenNotFound() {
    assertThat(subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), new Identifier().uuid())).isEmpty();
  }

  @Test
  void findByIdentifier() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "test-subject");
    storeSubject(subject);

    Optional<Subject> result = subjectDao.findByIdentifier(subject.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void upsertUpdatesExistingSubject() {
    Subject original = new Subject(OWNER.identifier(), CATEGORY, "original");
    storeSubject(original);

    Category newCategory = new Category("new-category");
    Subject updated = new Subject(OWNER.identifier(), newCategory, "updated", original.identifier());
    storeSubject(updated);

    Optional<Subject> result = subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), original.identifier().uuid());
    assertThat(result).isPresent();
    assertThat(result.get().category()).isEqualTo(newCategory);
    assertThat(result.get().value()).isEqualTo("updated");
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenSubjectExists() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "to-delete");
    storeSubject(subject);

    int deleted = subjectDao.deleteByOwnerAndIdentifier(
        OWNER.identifier().uuid(), subject.identifier().uuid());
    assertThat(deleted).isEqualTo(1);
    assertThat(subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), subject.identifier().uuid())).isEmpty();
  }

  @Test
  void deleteReturnsZeroWhenSubjectDoesNotExist() {
    assertThat(subjectDao.deleteByOwnerAndIdentifier(
        OWNER.identifier().uuid(), new Identifier().uuid())).isEqualTo(0);
  }

  // --- findByOwnerAndCategory ---

  @Test
  void findByOwnerAndCategoryReturnsMatchingSubjects() {
    Category other = new Category("other-category");

    Subject s1 = new Subject(OWNER.identifier(), CATEGORY, "alpha");
    Subject s2 = new Subject(OWNER.identifier(), CATEGORY, "beta");
    Subject s3 = new Subject(OWNER.identifier(), other, "gamma");

    storeSubject(s1);
    storeSubject(s2);
    storeSubject(s3);

    List<Subject> results = subjectDao.findByOwnerAndCategory(
        OWNER.identifier().uuid(), CATEGORY.value(), Integer.MAX_VALUE, 0);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Subject::value)
        .containsExactly("alpha", "beta");
  }

  @Test
  void findByOwnerAndCategoryReturnsEmptyWhenNoMatches() {
    assertThat(subjectDao.findByOwnerAndCategory(
        OWNER.identifier().uuid(), "nonexistent", Integer.MAX_VALUE, 0)).isEmpty();
  }

  // --- findByOwnerCategoryAndValue ---

  @Test
  void findByOwnerCategoryAndValueReturnsMatch() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "unique-value");
    storeSubject(subject);

    Optional<Subject> result = subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), CATEGORY.value(), "unique-value");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void findByOwnerCategoryAndValueReturnsEmptyWhenNotFound() {
    assertThat(subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), CATEGORY.value(), "nonexistent")).isEmpty();
  }

  // --- findByValue ---

  @Test
  void findByValueReturnsMatchingSubjects() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "shared-name");
    storeSubject(subject);

    List<Subject> results = subjectDao.findByValue("shared-name");
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().value()).isEqualTo("shared-name");
  }

  @Test
  void findByValueReturnsEmptyWhenNotFound() {
    assertThat(subjectDao.findByValue("nonexistent")).isEmpty();
  }

  // --- owner isolation ---

  @Test
  void subjectsAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.upsert(other.identifier().uuid(), other.value(), false);

    Subject s1 = new Subject(OWNER.identifier(), CATEGORY, "shared-name");
    Subject s2 = new Subject(other.identifier(), CATEGORY, "shared-name");

    storeSubject(s1);
    storeSubject(s2);

    assertThat(subjectDao.findByOwnerAndCategory(
        OWNER.identifier().uuid(), CATEGORY.value(), Integer.MAX_VALUE, 0)).hasSize(1);
    assertThat(subjectDao.findByOwnerAndCategory(
        other.identifier().uuid(), CATEGORY.value(), Integer.MAX_VALUE, 0)).hasSize(1);
    assertThat(subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), s1.identifier().uuid())).isPresent();
    assertThat(subjectDao.findByOwnerAndIdentifier(
        other.identifier().uuid(), s1.identifier().uuid())).isEmpty();
  }
}
