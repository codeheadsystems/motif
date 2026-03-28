package com.codeheadsystems.motif.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Identifier;
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

  private static final Category CATEGORY = new Category("test-category");

  private static Jdbi jdbi;
  private SubjectDao dao;

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
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
    });
    dao = jdbi.onDemand(SubjectDao.class);
  }

  // --- store and get ---

  @Test
  void storeAndRetrieveSubject() {
    Subject subject = new Subject(CATEGORY, "test-subject");

    dao.store(subject);
    Optional<Subject> result = dao.get(subject.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().category()).isEqualTo(CATEGORY);
    assertThat(result.get().value()).isEqualTo("test-subject");
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Optional<Subject> result = dao.get(new Identifier());

    assertThat(result).isEmpty();
  }

  @Test
  void storeUpdatesExistingSubject() {
    Subject original = new Subject(CATEGORY, "original");
    dao.store(original);

    Subject updated = Subject.from(original).build();
    // Upsert with same identifier but different category
    Category newCategory = new Category("new-category");
    Subject updatedSubject = new Subject(newCategory, "updated", original.identifier());
    dao.store(updatedSubject);

    Optional<Subject> result = dao.get(original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().category()).isEqualTo(newCategory);
    assertThat(result.get().value()).isEqualTo("updated");
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenSubjectExists() {
    Subject subject = new Subject(CATEGORY, "to-delete");
    dao.store(subject);

    boolean deleted = dao.delete(subject.identifier());

    assertThat(deleted).isTrue();
    assertThat(dao.get(subject.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenSubjectDoesNotExist() {
    boolean deleted = dao.delete(new Identifier());

    assertThat(deleted).isFalse();
  }

  // --- findByCategory ---

  @Test
  void findByCategoryReturnsMatchingSubjects() {
    Category other = new Category("other-category");

    Subject s1 = new Subject(CATEGORY, "alpha");
    Subject s2 = new Subject(CATEGORY, "beta");
    Subject s3 = new Subject(other, "gamma");

    dao.store(s1);
    dao.store(s2);
    dao.store(s3);

    List<Subject> results = dao.findByCategory(CATEGORY);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Subject::value)
        .containsExactly("alpha", "beta");
  }

  @Test
  void findByCategoryReturnsEmptyWhenNoMatches() {
    Category other = new Category("nonexistent");

    assertThat(dao.findByCategory(other)).isEmpty();
  }

  // --- find by category and value ---

  @Test
  void findByCategoryAndValueReturnsMatch() {
    Subject subject = new Subject(CATEGORY, "unique-value");
    dao.store(subject);

    Optional<Subject> result = dao.find(CATEGORY, "unique-value");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void findByCategoryAndValueReturnsEmptyWhenNotFound() {
    Optional<Subject> result = dao.find(CATEGORY, "nonexistent");

    assertThat(result).isEmpty();
  }
}
