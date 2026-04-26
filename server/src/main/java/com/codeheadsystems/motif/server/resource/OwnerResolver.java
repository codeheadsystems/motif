package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.CategoryManager;
import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.model.Owner;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OwnerResolver {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.owner");

  private final OwnerManager ownerManager;
  private final CategoryManager categoryManager;

  @Inject
  public OwnerResolver(final OwnerManager ownerManager, final CategoryManager categoryManager) {
    this.ownerManager = ownerManager;
    this.categoryManager = categoryManager;
  }

  public Owner resolve(HofmannPrincipal principal) {
    String credId = principal.credentialIdentifier();
    Optional<Owner> existing = ownerManager.find(credId);
    if (existing.isPresent()) {
      return existing.get();
    }
    // First time this credential is seen — create the Owner and seed their default category set.
    // seedDefaults is idempotent (INSERT...ON CONFLICT DO NOTHING) so a concurrent second caller
    // that loses the find race will no-op safely.
    Owner owner = ownerManager.findOrCreate(credId);
    categoryManager.seedDefaults(owner);
    AUDIT.info("owner.resolved credentialId={} uuid={}", credId, owner.identifier().uuid());
    return owner;
  }
}
