package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.dao.CategoryDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

@Singleton
public class CategoryManager {

  /**
   * Default categories seeded for every newly-created owner. Order is preserved when
   * rendering the sidebar.
   */
  public static final List<DefaultCategory> DEFAULTS = List.of(
      new DefaultCategory("Home", "#3B82F6", "house"),
      new DefaultCategory("Health", "#10B981", "heart"),
      new DefaultCategory("Creative", "#F59E0B", "palette"),
      new DefaultCategory("Learning", "#8B5CF6", "book"),
      new DefaultCategory("Professional", "#EF4444", "briefcase")
  );

  private final Jdbi jdbi;
  private final CategoryDao categoryDao;

  @Inject
  public CategoryManager(final Jdbi jdbi, final CategoryDao categoryDao) {
    this.jdbi = jdbi;
    this.categoryDao = categoryDao;
  }

  public Optional<Category> get(Owner owner, Identifier identifier) {
    return categoryDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  public Optional<Category> findByName(Owner owner, String name) {
    return categoryDao.findByOwnerAndName(owner.identifier().uuid(), name.strip());
  }

  public Page<Category> findByOwner(Owner owner, PageRequest pageRequest) {
    List<Category> results = categoryDao.findByOwner(
        owner.identifier().uuid(),
        pageRequest.pageSize() + 1,
        pageRequest.offset());
    return Page.of(results, pageRequest);
  }

  public void store(Category category) {
    categoryDao.upsert(
        category.identifier().uuid(),
        category.ownerIdentifier().uuid(),
        category.name(),
        category.color(),
        category.icon());
  }

  /**
   * Inserts the category only if no row with the same (owner, name) already exists. Returns the
   * persisted category — either the one just inserted, or the pre-existing one with the same
   * name (which keeps its original identifier, color, and icon).
   */
  public Category findOrCreate(Category candidate) {
    return jdbi.inTransaction(handle -> {
      CategoryDao txDao = handle.attach(CategoryDao.class);
      txDao.insertIfAbsent(
          candidate.identifier().uuid(),
          candidate.ownerIdentifier().uuid(),
          candidate.name(),
          candidate.color(),
          candidate.icon());
      return txDao.findByOwnerAndName(candidate.ownerIdentifier().uuid(), candidate.name())
          .orElseThrow(() -> new IllegalStateException(
              "category missing immediately after insertIfAbsent: " + candidate.name()));
    });
  }

  /**
   * Updates the category. Throws NotFoundException if the row does not exist for this owner
   * (prevents IDOR — see SubjectManager.update for the same pattern).
   */
  public void update(Category category) {
    jdbi.useTransaction(handle -> {
      CategoryDao txDao = handle.attach(CategoryDao.class);
      Optional<Category> existing = txDao.findByOwnerAndIdentifier(
          category.ownerIdentifier().uuid(), category.identifier().uuid());
      if (existing.isEmpty()) {
        throw new NotFoundException("Category not found: " + category.identifier().uuid());
      }
      txDao.upsert(
          category.identifier().uuid(),
          category.ownerIdentifier().uuid(),
          category.name(),
          category.color(),
          category.icon());
    });
  }

  /**
   * Deletes the category, refusing if any subject still references it (FK is RESTRICT, but we
   * check first to return a clean 409 instead of letting the FK violation bubble out).
   *
   * @return true if a row was deleted, false if no matching row existed
   * @throws CategoryInUseException if subjects still reference the category
   */
  public boolean delete(Owner owner, Identifier identifier) {
    return jdbi.inTransaction(handle -> {
      CategoryDao txDao = handle.attach(CategoryDao.class);
      long subjectCount = txDao.countSubjectsByCategory(identifier.uuid());
      if (subjectCount > 0) {
        throw new CategoryInUseException(identifier, subjectCount);
      }
      return txDao.deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
    });
  }

  /**
   * Seeds the canonical default category set for a freshly created owner. Idempotent: an
   * owner who already has a category by a given default name keeps the existing row.
   */
  public void seedDefaults(Owner owner) {
    jdbi.useTransaction(handle -> {
      CategoryDao txDao = handle.attach(CategoryDao.class);
      for (DefaultCategory dc : DEFAULTS) {
        Category candidate = Category.builder()
            .owner(owner)
            .name(dc.name())
            .color(dc.color())
            .icon(dc.icon())
            .build();
        txDao.insertIfAbsent(
            candidate.identifier().uuid(),
            candidate.ownerIdentifier().uuid(),
            candidate.name(),
            candidate.color(),
            candidate.icon());
      }
    });
  }

  public record DefaultCategory(String name, String color, String icon) {}

  public static class CategoryInUseException extends RuntimeException {
    private final Identifier identifier;
    private final long subjectCount;

    public CategoryInUseException(Identifier identifier, long subjectCount) {
      super("Category " + identifier.uuid() + " has " + subjectCount + " subject(s) and cannot be deleted");
      this.identifier = identifier;
      this.subjectCount = subjectCount;
    }

    public Identifier identifier() {
      return identifier;
    }

    public long subjectCount() {
      return subjectCount;
    }
  }
}
