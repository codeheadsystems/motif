package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.dao.SubjectDao;
import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
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

  public Optional<Subject> getSubject(Identifier identifier) {
    return subjectDao.findByIdentifier(identifier.uuid());
  }

  public Optional<Subject> getSubject(Owner owner, Identifier identifier) {
    return subjectDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  public Page<Subject> findByCategory(Owner owner, Category category, PageRequest pageRequest) {
    List<Subject> results = subjectDao.findByOwnerAndCategory(
        owner.identifier().uuid(), category.value(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(results, pageRequest);
  }

  public Optional<Subject> find(Owner owner, Category category, String value) {
    return subjectDao.findByOwnerCategoryAndValue(owner.identifier().uuid(), category.value(), value);
  }

  public void store(Subject subject) {
    subjectDao.upsert(
        subject.identifier().uuid(),
        subject.ownerIdentifier().uuid(),
        subject.category().value(),
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
      Optional<Subject> existing = txSubjectDao.findByIdentifier(subject.identifier().uuid());
      if (existing.isEmpty()) {
        throw new NotFoundException("Subject not found: " + subject.identifier().uuid());
      }
      txSubjectDao.upsert(
          subject.identifier().uuid(),
          subject.ownerIdentifier().uuid(),
          subject.category().value(),
          subject.value());
    });
  }
}
