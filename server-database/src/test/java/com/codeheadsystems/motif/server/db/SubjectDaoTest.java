package com.codeheadsystems.motif.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Owner;
import com.codeheadsystems.motif.model.Subject;
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
    ownerDao.store(OWNER);
  }

  // --- store and get ---

  @Test
  void storeAndRetrieveSubject() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

    subjectDao.store(subject);
    Optional<Subject> result = subjectDao.get(OWNER, subject.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().ownerIdentifier().uuid()).isEqualTo(OWNER.identifier().uuid());
    assertThat(result.get().category()).isEqualTo(CATEGORY);
    assertThat(result.get().value()).isEqualTo("test-subject");
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(subjectDao.get(OWNER, new Identifier())).isEmpty();
  }

  @Test
  void storeUpdatesExistingSubject() {
    Subject original = new Subject(OWNER.identifier(), CATEGORY, "original");
    subjectDao.store(original);

    Category newCategory = new Category("new-category");
    Subject updated = new Subject(OWNER.identifier(), newCategory, "updated", original.identifier());
    subjectDao.store(updated);

    Optional<Subject> result = subjectDao.get(OWNER, original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().category()).isEqualTo(newCategory);
    assertThat(result.get().value()).isEqualTo("updated");
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenSubjectExists() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "to-delete");
    subjectDao.store(subject);

    assertThat(subjectDao.delete(OWNER, subject.identifier())).isTrue();
    assertThat(subjectDao.get(OWNER, subject.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenSubjectDoesNotExist() {
    assertThat(subjectDao.delete(OWNER, new Identifier())).isFalse();
  }

  // --- findByCategory ---

  @Test
  void findByCategoryReturnsMatchingSubjects() {
    Category other = new Category("other-category");

    Subject s1 = new Subject(OWNER.identifier(), CATEGORY, "alpha");
    Subject s2 = new Subject(OWNER.identifier(), CATEGORY, "beta");
    Subject s3 = new Subject(OWNER.identifier(), other, "gamma");

    subjectDao.store(s1);
    subjectDao.store(s2);
    subjectDao.store(s3);

    List<Subject> results = subjectDao.findByCategory(OWNER, CATEGORY);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Subject::value)
        .containsExactly("alpha", "beta");
  }

  @Test
  void findByCategoryReturnsEmptyWhenNoMatches() {
    assertThat(subjectDao.findByCategory(OWNER, new Category("nonexistent"))).isEmpty();
  }

  // --- find by category and value ---

  @Test
  void findByCategoryAndValueReturnsMatch() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "unique-value");
    subjectDao.store(subject);

    Optional<Subject> result = subjectDao.find(OWNER, CATEGORY, "unique-value");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void findByCategoryAndValueReturnsEmptyWhenNotFound() {
    assertThat(subjectDao.find(OWNER, CATEGORY, "nonexistent")).isEmpty();
  }

  // --- owner isolation ---

  @Test
  void subjectsAreIsolatedByOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.store(other);

    Subject s1 = new Subject(OWNER.identifier(), CATEGORY, "shared-name");
    Subject s2 = new Subject(other.identifier(), CATEGORY, "shared-name");

    subjectDao.store(s1);
    subjectDao.store(s2);

    assertThat(subjectDao.findByCategory(OWNER, CATEGORY)).hasSize(1);
    assertThat(subjectDao.findByCategory(other, CATEGORY)).hasSize(1);
    assertThat(subjectDao.get(OWNER, s1.identifier())).isPresent();
    assertThat(subjectDao.get(other, s1.identifier())).isEmpty();
  }
}
