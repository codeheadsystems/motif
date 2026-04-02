package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.OwnerDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OwnerManager {

  private final OwnerDao ownerDao;

  @Inject
  public OwnerManager(final OwnerDao ownerDao) {
    this.ownerDao = ownerDao;
  }

  public void store(Owner owner) {
    ownerDao.upsert(owner.identifier().uuid(), owner.value());
  }

  public Optional<Owner> get(Identifier identifier) {
    return ownerDao.findByIdentifier(identifier.uuid());
  }

  public boolean delete(Identifier identifier) {
    return ownerDao.deleteByIdentifier(identifier.uuid()) > 0;
  }

  public Optional<Owner> find(String value) {
    return ownerDao.findByValue(value.strip().toUpperCase());
  }
}
