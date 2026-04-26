package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.TestCategories;
import com.codeheadsystems.motif.server.db.dao.CategoryDao;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubjectManagerIntegrationTest extends DatabaseTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");

  private Category category;
  private SubjectManager subjectManager;
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
    SubjectDao subjectDao = jdbi.onDemand(SubjectDao.class);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    category = TestCategories.of(OWNER.identifier(), "test-category");
    storeCategory(category);
    subjectManager = new SubjectManager(jdbi, subjectDao);
  }

  private void storeCategory(Category c) {
    categoryDao.upsert(c.identifier().uuid(), c.ownerIdentifier().uuid(), c.name(), c.color(), c.icon());
  }

  // --- store ---

  @Test
  void storeSubject() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "test-subject");

    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.get(subject.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("test-subject");
  }

  @Test
  void getByIdentifier() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "test-subject");
    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.get(subject.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("test-subject");
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void getByIdentifierReturnsEmptyWhenNotFound() {
    assertThat(subjectManager.get(new Identifier())).isEmpty();
  }

  @Test
  void getByOwnerAndIdentifier() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "test-subject");
    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.get(OWNER, subject.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("test-subject");
  }

  @Test
  void getByOwnerAndIdentifierReturnsEmptyForWrongOwner() {
    Owner other = new Owner("OTHER-OWNER");
    ownerDao.upsert(other.identifier().uuid(), other.value(), false);

    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "test-subject");
    subjectManager.store(subject);

    assertThat(subjectManager.get(other, subject.identifier())).isEmpty();
  }

  @Test
  void findByCategoryReturnsMatchingSubjects() {
    Category other = TestCategories.of(OWNER.identifier(), "other-category");
    storeCategory(other);

    subjectManager.store(new Subject(OWNER.identifier(), category.identifier(), "alpha"));
    subjectManager.store(new Subject(OWNER.identifier(), category.identifier(), "beta"));
    subjectManager.store(new Subject(OWNER.identifier(), other.identifier(), "gamma"));

    Page<Subject> results = subjectManager.findByCategory(OWNER, category, PageRequest.first());

    assertThat(results.items()).hasSize(2);
    assertThat(results.items()).extracting(Subject::value).containsExactly("alpha", "beta");
    assertThat(results.hasMore()).isFalse();
  }

  @Test
  void findByCategoryReturnsEmptyWhenNoMatches() {
    Identifier missing = new Identifier();
    assertThat(subjectManager.findByCategory(OWNER, missing, PageRequest.first()).isEmpty()).isTrue();
  }

  @Test
  void findByCategoryPaginates() {
    subjectManager.store(new Subject(OWNER.identifier(), category.identifier(), "a"));
    subjectManager.store(new Subject(OWNER.identifier(), category.identifier(), "b"));
    subjectManager.store(new Subject(OWNER.identifier(), category.identifier(), "c"));

    Page<Subject> page1 = subjectManager.findByCategory(OWNER, category, PageRequest.first(2));
    assertThat(page1.items()).hasSize(2);
    assertThat(page1.hasMore()).isTrue();
    assertThat(page1.nextPageRequest()).isPresent();

    Page<Subject> page2 = subjectManager.findByCategory(OWNER, category, page1.nextPageRequest().get());
    assertThat(page2.items()).hasSize(1);
    assertThat(page2.hasMore()).isFalse();
    assertThat(page2.nextPageRequest()).isEmpty();
  }

  @Test
  void findByOwnerCategoryAndValue() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "unique-value");
    subjectManager.store(subject);

    Optional<Subject> result = subjectManager.find(OWNER, category, "unique-value");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(subject.identifier().uuid());
  }

  @Test
  void findByOwnerCategoryAndValueReturnsEmptyWhenNotFound() {
    assertThat(subjectManager.find(OWNER, category, "nonexistent")).isEmpty();
  }

  @Test
  void updateExistingSubject() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "original");
    subjectManager.store(subject);

    Subject updated = Subject.from(subject).value("updated").build();
    subjectManager.update(updated);

    Optional<Subject> result = subjectManager.get(subject.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("updated");
  }

  @Test
  void updateThrowsWhenSubjectDoesNotExist() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "nonexistent");

    assertThatThrownBy(() -> subjectManager.update(subject))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deleteExistingSubject() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "to-delete");
    subjectManager.store(subject);

    assertThat(subjectManager.delete(subject)).isTrue();
    assertThat(subjectManager.get(subject.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenSubjectDoesNotExist() {
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "nonexistent");

    assertThat(subjectManager.delete(subject)).isFalse();
  }
}
