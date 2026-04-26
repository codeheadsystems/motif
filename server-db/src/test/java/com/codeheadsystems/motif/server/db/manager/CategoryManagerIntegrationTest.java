package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.dao.CategoryDao;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.manager.CategoryManager.CategoryInUseException;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryManagerIntegrationTest extends DatabaseTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");

  private CategoryManager categoryManager;
  private SubjectDao subjectDao;
  private CategoryDao categoryDao;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM categories");
      handle.execute("DELETE FROM owners");
    });
    OwnerDao ownerDao = jdbi.onDemand(OwnerDao.class);
    categoryDao = jdbi.onDemand(CategoryDao.class);
    subjectDao = jdbi.onDemand(SubjectDao.class);
    ownerDao.upsert(OWNER.identifier().uuid(), OWNER.value(), false);
    categoryManager = new CategoryManager(jdbi, categoryDao);
  }

  private Category newCategory(String name) {
    return Category.builder().owner(OWNER).name(name).color("#3B82F6").icon("house").build();
  }

  @Test
  void storeAndGet() {
    Category category = newCategory("Home");
    categoryManager.store(category);

    Optional<Category> result = categoryManager.get(OWNER, category.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("Home");
  }

  @Test
  void findOrCreateInsertsNewRow() {
    Category candidate = newCategory("Home");
    Category persisted = categoryManager.findOrCreate(candidate);
    assertThat(persisted).isEqualTo(candidate);
  }

  @Test
  void findOrCreateReturnsExistingWhenNamePresent() {
    Category first = categoryManager.findOrCreate(newCategory("Home"));
    Category second = categoryManager.findOrCreate(newCategory("Home"));
    assertThat(second.identifier()).isEqualTo(first.identifier());
  }

  @Test
  void seedDefaultsInsertsAllFiveDefaults() {
    categoryManager.seedDefaults(OWNER);
    var page = categoryManager.findByOwner(OWNER, PageRequest.first());
    assertThat(page.items()).hasSize(5);
    assertThat(page.items()).extracting(Category::name)
        .containsExactly("Creative", "Health", "Home", "Learning", "Professional");
  }

  @Test
  void seedDefaultsIsIdempotent() {
    categoryManager.seedDefaults(OWNER);
    categoryManager.seedDefaults(OWNER);
    var page = categoryManager.findByOwner(OWNER, PageRequest.first());
    assertThat(page.items()).hasSize(5);
  }

  @Test
  void deleteSucceedsWhenNoSubjectsReferenceCategory() {
    Category category = categoryManager.findOrCreate(newCategory("Home"));
    assertThat(categoryManager.delete(OWNER, category.identifier())).isTrue();
    assertThat(categoryManager.get(OWNER, category.identifier())).isEmpty();
  }

  @Test
  void deleteThrowsCategoryInUseWhenSubjectsReference() {
    Category category = categoryManager.findOrCreate(newCategory("Home"));
    Subject subject = new Subject(OWNER.identifier(), category.identifier(), "plant");
    subjectDao.upsert(subject.identifier().uuid(), subject.ownerIdentifier().uuid(),
        subject.categoryIdentifier().uuid(), subject.value());

    assertThatThrownBy(() -> categoryManager.delete(OWNER, category.identifier()))
        .isInstanceOf(CategoryInUseException.class);
    assertThat(categoryManager.get(OWNER, category.identifier())).isPresent();
  }

  @Test
  void updateRejectsCategoryNotOwnedByOwner() {
    Category foreignOwnerCategory = Category.builder()
        .ownerIdentifier(new Identifier()).name("foreign").color("#000000").icon("tag").build();

    assertThatThrownBy(() -> categoryManager.update(foreignOwnerCategory))
        .isInstanceOf(NotFoundException.class);
  }
}
