package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.model.Owner;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OwnerResolver {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.owner");

  private final OwnerManager ownerManager;

  @Inject
  public OwnerResolver(final OwnerManager ownerManager) {
    this.ownerManager = ownerManager;
  }

  public Owner resolve(HofmannPrincipal principal) {
    String credId = principal.credentialIdentifier();
    return ownerManager.find(credId)
        .orElseGet(() -> {
          Owner owner = new Owner(credId);
          ownerManager.store(owner);
          AUDIT.info("owner.created credentialId={}", credId);
          return owner;
        });
  }
}
