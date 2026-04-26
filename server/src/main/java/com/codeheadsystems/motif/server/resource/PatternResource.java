package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.PatternDetectionManager;
import com.codeheadsystems.motif.server.db.manager.PatternManager;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("/api/patterns")
@Produces(MediaType.APPLICATION_JSON)
public class PatternResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.pattern");
  private static final int DEFAULT_LIMIT = 5;
  private static final int MAX_LIMIT = 50;

  private final PatternManager patternManager;
  private final PatternDetectionManager detectionManager;
  private final OwnerResolver ownerResolver;

  @Inject
  public PatternResource(final PatternManager patternManager,
                         final PatternDetectionManager detectionManager,
                         final OwnerResolver ownerResolver) {
    this.patternManager = patternManager;
    this.detectionManager = detectionManager;
    this.ownerResolver = ownerResolver;
  }

  @GET
  public Page<Pattern> list(@Auth HofmannPrincipal principal,
                            @QueryParam("limit") @DefaultValue("5") int limit) {
    int clamped = Math.max(1, Math.min(limit, MAX_LIMIT));
    Owner owner = ownerResolver.resolve(principal);
    return patternManager.findByOwner(owner, PageRequest.first(clamped));
  }

  /**
   * Force an immediate re-run of the detector for the calling owner. Returns the freshly
   * computed top patterns. Useful in dev and from the webapp's "refresh" button.
   */
  @POST
  @Path("/recompute")
  public Response recompute(@Auth HofmannPrincipal principal,
                            @QueryParam("limit") @DefaultValue("5") int limit) {
    int clamped = Math.max(1, Math.min(limit, MAX_LIMIT));
    Owner owner = ownerResolver.resolve(principal);
    int written = detectionManager.detectForOwner(owner);
    AUDIT.info("pattern.recompute.requested owner={} patternsWritten={}",
        owner.value(), written);
    Page<Pattern> page = patternManager.findByOwner(owner, PageRequest.first(clamped));
    return Response.ok(page).build();
  }
}
