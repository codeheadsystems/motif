package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
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

      // Tags have no FK to entities, so they won't cascade — clean them up first.
      txTagsDao.deleteTagsForOwnerNotes(identifier.uuid());
      txTagsDao.deleteTagsForOwnerEvents(identifier.uuid());
      // DB cascades deletion of notes, events, and subjects via ON DELETE CASCADE.
      return txOwnerDao.deleteByIdentifier(identifier.uuid()) > 0;
    });
  }
}
