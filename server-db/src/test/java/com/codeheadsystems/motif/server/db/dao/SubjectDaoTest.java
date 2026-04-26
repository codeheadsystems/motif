package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.TestCategories;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubjectDaoTest extends DatabaseTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");

  private Category category;
  private Category otherCategory;
  private SubjectDao subjectDao;
  private CategoryDao categoryDao;
  private OwnerDao ownerDao;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM categories");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    categoryDao = jdbi.onDemand(CategoryDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    category = TestCategories.of(OWNER.identifier(), "test-category");
    storeCategory(category);
    otherCategory = TestCategories.of(OWNER.identifier(), "other-category");
    storeCategory(otherCategory);
  }

  private void storeCategory(Category c) {
    categoryDao.upsert(c.identifier().uuid(), c.ownerIdentifier().uuid(), c.name(), c.color(), c.icon());
  }

  private void storeSubject(Subject subject) {
    subjectDao.upsert(
        subject.identifier().uuid(),
        subject.ownerIdentifier().uuid(),
        subject.categoryIdentifier().uuid(),
        subject.value());
  }

  // --- upsert and find ---

  @Test
  void upsertAndFindByOwnerAndIdentifier() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "test-subject");

    storeSubject(subject);
    Optional<Subject> result = subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), subject.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().ownerIdentifier().uuid()).isEqualTo(OWNER.identifier().uuid());
    assertThat(result.get().categoryIdentifier()).isEqualTo(category.identifier());
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
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "test-subject");
    storeSubject(subject);

    Optional<Subject> result = subjectDao.findByIdentifier(subject.identifier().uuid());

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void upsertUpdatesExistingSubject() {
    Subject original = new Subject(OWNER.identifier(), category.identifier(), "original");
    storeSubject(original);

    Subject updated = new Subject(OWNER.identifier(), otherCategory.identifier(), "updated", original.identifier());
    storeSubject(updated);

    Optional<Subject> result = subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), original.identifier().uuid());
    assertThat(result).isPresent();
    assertThat(result.get().categoryIdentifier()).isEqualTo(otherCategory.identifier());
    assertThat(result.get().value()).isEqualTo("updated");
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenSubjectExists() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "to-delete");
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
    Subject s1 = new Subject(OWNER.identifier(), category.identifier(), "alpha");
    Subject s2 = new Subject(OWNER.identifier(), category.identifier(), "beta");
    Subject s3 = new Subject(OWNER.identifier(), otherCategory.identifier(), "gamma");

    storeSubject(s1);
    storeSubject(s2);
    storeSubject(s3);

    List<Subject> results = subjectDao.findByOwnerAndCategory(
        OWNER.identifier().uuid(), category.identifier().uuid(), Integer.MAX_VALUE, 0);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(Subject::value)
        .containsExactly("alpha", "beta");
  }

  @Test
  void findByOwnerAndCategoryReturnsEmptyWhenNoMatches() {
    assertThat(subjectDao.findByOwnerAndCategory(
        OWNER.identifier().uuid(), new Identifier().uuid(), Integer.MAX_VALUE, 0)).isEmpty();
  }

  // --- findByOwnerCategoryAndValue ---

  @Test
  void findByOwnerCategoryAndValueReturnsMatch() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "unique-value");
    storeSubject(subject);

    Optional<Subject> result = subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), category.identifier().uuid(), "unique-value");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void findByOwnerCategoryAndValueReturnsEmptyWhenNotFound() {
    assertThat(subjectDao.findByOwnerCategoryAndValue(
        OWNER.identifier().uuid(), category.identifier().uuid(), "nonexistent")).isEmpty();
  }

  // --- findByValue ---

  @Test
  void findByValueReturnsMatchingSubjects() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "shared-name");
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
    Category otherOwnerCategory = TestCategories.of(other.identifier(), "test-category");
    storeCategory(otherOwnerCategory);

    Subject s1 = new Subject(OWNER.identifier(), category.identifier(), "shared-name");
    Subject s2 = new Subject(other.identifier(), otherOwnerCategory.identifier(), "shared-name");

    storeSubject(s1);
    storeSubject(s2);

    assertThat(subjectDao.findByOwnerAndCategory(
        OWNER.identifier().uuid(), category.identifier().uuid(), Integer.MAX_VALUE, 0)).hasSize(1);
    assertThat(subjectDao.findByOwnerAndCategory(
        other.identifier().uuid(), otherOwnerCategory.identifier().uuid(), Integer.MAX_VALUE, 0)).hasSize(1);
    assertThat(subjectDao.findByOwnerAndIdentifier(
        OWNER.identifier().uuid(), s1.identifier().uuid())).isPresent();
    assertThat(subjectDao.findByOwnerAndIdentifier(
        other.identifier().uuid(), s1.identifier().uuid())).isEmpty();
  }
}
