package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.dao.NoteDao;
import com.codeheadsystems.motif.server.dao.OwnerDao;
import com.codeheadsystems.motif.server.dao.SubjectDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

@Singleton
public class OwnerManager {

  private final Jdbi jdbi;
  private final OwnerDao ownerDao;

  @Inject
  public OwnerManager(final Jdbi jdbi, final OwnerDao ownerDao) {
    this.jdbi = jdbi;
    this.ownerDao = ownerDao;
  }

  public void store(Owner owner) {
    ownerDao.upsert(owner.identifier().uuid(), owner.value(), owner.deleted());
  }

  public Optional<Owner> get(Identifier identifier) {
    return ownerDao.findByIdentifier(identifier.uuid());
  }

  public Optional<Owner> get(Identifier identifier, boolean includeSoftDeleted) {
    if (includeSoftDeleted) {
      return ownerDao.findByIdentifierIncludingDeleted(identifier.uuid());
    }
    return ownerDao.findByIdentifier(identifier.uuid());
  }

  public Optional<Owner> find(String value) {
    return ownerDao.findByValue(value.strip().toUpperCase());
  }

  public Optional<Owner> find(String value, boolean includeSoftDeleted) {
    String normalized = value.strip().toUpperCase();
    if (includeSoftDeleted) {
      return ownerDao.findByValueIncludingDeleted(normalized);
    }
    return ownerDao.findByValue(normalized);
  }

  public boolean softDelete(Identifier identifier) {
    return ownerDao.softDelete(identifier.uuid()) > 0;
  }

  public boolean hardDelete(Identifier identifier) {
    return jdbi.inTransaction(handle -> {
      OwnerDao txOwnerDao = handle.attach(OwnerDao.class);
      TagsDao txTagsDao = handle.attach(TagsDao.class);
      NoteDao txNoteDao = handle.attach(NoteDao.class);
      EventDao txEventDao = handle.attach(EventDao.class);
      SubjectDao txSubjectDao = handle.attach(SubjectDao.class);

      txTagsDao.deleteTagsForOwnerNotes(identifier.uuid());
      txTagsDao.deleteTagsForOwnerEvents(identifier.uuid());
      txNoteDao.deleteByOwner(identifier.uuid());
      txEventDao.deleteByOwner(identifier.uuid());
      txSubjectDao.deleteByOwner(identifier.uuid());
      return txOwnerDao.deleteByIdentifier(identifier.uuid()) > 0;
    });
  }
}
