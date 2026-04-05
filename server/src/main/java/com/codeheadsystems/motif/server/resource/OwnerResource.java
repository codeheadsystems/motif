package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.model.Owner;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("/api/owner")
@Produces(MediaType.APPLICATION_JSON)
public class OwnerResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.owner");

  private final OwnerResolver ownerResolver;

  @Inject
  public OwnerResource(final OwnerResolver ownerResolver) {
    this.ownerResolver = ownerResolver;
  }

  @GET
  public Owner getOrCreateOwner(@Auth HofmannPrincipal principal) {
    AUDIT.info("owner.access credentialId={}", principal.credentialIdentifier());
    return ownerResolver.resolve(principal);
  }
}
