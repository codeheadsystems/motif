package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.model.Owner;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Path("/api/owner")
@Produces(MediaType.APPLICATION_JSON)
public class OwnerResource {

  private final OwnerManager ownerManager;

  @Inject
  public OwnerResource(final OwnerManager ownerManager) {
    this.ownerManager = ownerManager;
  }

  @GET
  public Owner getOrCreateOwner(@Auth HofmannPrincipal principal) {
    return resolveOwner(principal);
  }

  Owner resolveOwner(HofmannPrincipal principal) {
    String credId = principal.credentialIdentifier();
    return ownerManager.find(credId)
        .orElseGet(() -> {
          Owner owner = new Owner(credId);
          ownerManager.store(owner);
          return owner;
        });
  }
}
