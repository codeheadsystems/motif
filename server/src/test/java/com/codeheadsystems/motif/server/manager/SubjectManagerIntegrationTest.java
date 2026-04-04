package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.dao.OwnerDao;
import com.codeheadsystems.motif.server.dao.SubjectDao;
import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
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
class SubjectManagerIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");

  private static Jdbi jdbi;
  private SubjectManager subjectManager;
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
    SubjectDao subjectDao = jdbi.onDemand(SubjectDao.class);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    subjectManager = new SubjectManager(jdbi, subjectDao);
  }

  // --- store ---

  @Test
  void storeSubject() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "test-subject");

    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.getSubject(subject.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("test-subject");
  }

  // --- getSubject by identifier ---

  @Test
  void getSubjectByIdentifier() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "test-subject");
    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.getSubject(subject.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("test-subject");
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void getSubjectByIdentifierReturnsEmptyWhenNotFound() {
    assertThat(subjectManager.getSubject(new Identifier())).isEmpty();
  }

  // --- getSubject by owner and identifier ---

  @Test
  void getSubjectByOwnerAndIdentifier() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "test-subject");
    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.getSubject(OWNER, subject.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("test-subject");
  }

  @Test
  void getSubjectByOwnerAndIdentifierReturnsEmptyForWrongOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.upsert(other.identifier().uuid(), other.value(), false);

    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "test-subject");
    subjectManager.store(subject);

    assertThat(subjectManager.getSubject(other, subject.identifier())).isEmpty();
  }

  // --- findByCategory ---

  @Test
  void findByCategoryReturnsMatchingSubjects() {
    Category other = new Category("other-category");

    subjectManager.store(new Subject(OWNER.identifier(), CATEGORY, "alpha"));
    subjectManager.store(new Subject(OWNER.identifier(), CATEGORY, "beta"));
    subjectManager.store(new Subject(OWNER.identifier(), other, "gamma"));

    Page<Subject> results = subjectManager.findByCategory(OWNER, CATEGORY, PageRequest.first());

    assertThat(results.items()).hasSize(2);
    assertThat(results.items()).extracting(Subject::value).containsExactly("alpha", "beta");
    assertThat(results.hasMore()).isFalse();
  }

  @Test
  void findByCategoryReturnsEmptyWhenNoMatches() {
    assertThat(subjectManager.findByCategory(OWNER, new Category("nonexistent"), PageRequest.first()).isEmpty()).isTrue();
  }

  @Test
  void findByCategoryPaginates() {
    subjectManager.store(new Subject(OWNER.identifier(), CATEGORY, "a"));
    subjectManager.store(new Subject(OWNER.identifier(), CATEGORY, "b"));
    subjectManager.store(new Subject(OWNER.identifier(), CATEGORY, "c"));

    Page<Subject> page1 = subjectManager.findByCategory(OWNER, CATEGORY, PageRequest.first(2));
    assertThat(page1.items()).hasSize(2);
    assertThat(page1.hasMore()).isTrue();
    assertThat(page1.nextPageRequest()).isPresent();

    Page<Subject> page2 = subjectManager.findByCategory(OWNER, CATEGORY, page1.nextPageRequest().get());
    assertThat(page2.items()).hasSize(1);
    assertThat(page2.hasMore()).isFalse();
    assertThat(page2.nextPageRequest()).isEmpty();
  }

  // --- find by owner, category, and value ---

  @Test
  void findByOwnerCategoryAndValue() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "unique-value");
    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.find(OWNER, CATEGORY, "unique-value");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void findByOwnerCategoryAndValueReturnsEmptyWhenNotFound() {
    assertThat(subjectManager.find(OWNER, CATEGORY, "nonexistent")).isEmpty();
  }

  // --- update ---

  @Test
  void updateExistingSubject() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "original");
    subjectManager.store(subject);

    Subject updated = Subject.from(subject).value("updated").build();
    subjectManager.update(updated);

    Optional<Subject> result = subjectManager.getSubject(subject.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated");
  }

  @Test
  void updateThrowsWhenSubjectDoesNotExist() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "nonexistent");

    assertThatThrownBy(() -> subjectManager.update(subject))
        .isInstanceOf(NotFoundException.class);
  }

  // --- delete ---

  @Test
  void deleteExistingSubject() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "to-delete");
    subjectManager.store(subject);

    assertThat(subjectManager.delete(subject)).isTrue();
    assertThat(subjectManager.getSubject(subject.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenSubjectDoesNotExist() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY, "nonexistent");

    assertThat(subjectManager.delete(subject)).isFalse();
  }
}
