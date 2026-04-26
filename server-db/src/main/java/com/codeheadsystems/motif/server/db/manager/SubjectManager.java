package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

@Singleton
public class SubjectManager {

  private final Jdbi jdbi;
  private final SubjectDao subjectDao;

  @Inject
  public SubjectManager(final Jdbi jdbi, final SubjectDao subjectDao) {
    this.jdbi = jdbi;
    this.subjectDao = subjectDao;
  }

  public Optional<Subject> get(Identifier identifier) {
    return subjectDao.findByIdentifier(identifier.uuid());
  }

  public Optional<Subject> get(Owner owner, Identifier identifier) {
    return subjectDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  public Page<Subject> findByCategory(Owner owner, Category category, PageRequest pageRequest) {
    return findByCategory(owner, category.identifier(), pageRequest);
  }

  public Page<Subject> findByCategory(Owner owner, Identifier categoryIdentifier, PageRequest pageRequest) {
    List<Subject> results = subjectDao.findByOwnerAndCategory(
        owner.identifier().uuid(), categoryIdentifier.uuid(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(results, pageRequest);
  }

  public Optional<Subject> find(Owner owner, Category category, String value) {
    return subjectDao.findByOwnerCategoryAndValue(
        owner.identifier().uuid(), category.identifier().uuid(), value);
  }

  public Page<Subject> findByProject(Owner owner, Identifier projectIdentifier, PageRequest pageRequest) {
    List<Subject> results = subjectDao.findByOwnerAndProject(
        owner.identifier().uuid(), projectIdentifier.uuid(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(results, pageRequest);
  }

  public void store(Subject subject) {
    subjectDao.upsert(
        subject.identifier().uuid(),
        subject.ownerIdentifier().uuid(),
        subject.categoryIdentifier().uuid(),
        subject.projectIdentifier() == null ? null : subject.projectIdentifier().uuid(),
        subject.value());
  }

  public boolean delete(Subject subject) {
    return subjectDao.deleteByOwnerAndIdentifier(
        subject.ownerIdentifier().uuid(),
        subject.identifier().uuid()) > 0;
  }

  public void update(Subject subject) {
    jdbi.useTransaction(handle -> {
      SubjectDao txSubjectDao = handle.attach(SubjectDao.class);
      Optional<Subject> existing = txSubjectDao.findByOwnerAndIdentifier(
          subject.ownerIdentifier().uuid(), subject.identifier().uuid());
      if (existing.isEmpty()) {
        throw new NotFoundException("Subject not found: " + subject.identifier().uuid());
      }
      txSubjectDao.upsert(
          subject.identifier().uuid(),
          subject.ownerIdentifier().uuid(),
          subject.categoryIdentifier().uuid(),
          subject.projectIdentifier() == null ? null : subject.projectIdentifier().uuid(),
          subject.value());
    });
  }
}
