package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin endpoints, protected by the {@code MOTIF_ADMIN_TOKEN} env var. Operates entirely
 * outside the Hofmann/JWT user-auth path: callers send {@code Authorization: Bearer <token>}
 * with the value of that env var.
 *
 * <p>If {@code MOTIF_ADMIN_TOKEN} is unset at boot every endpoint here returns 503. That is
 * intentional — silently allowing all requests would be dangerous, and silently rejecting
 * with 401 would be confusing during dev. 503 says "this is wired wrong, fix the env."
 *
 * <p>Phase 3a only ships tier promotion. Future admin operations (user lookups, hard-delete
 * runners, …) belong here too.
 */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.admin");

  private final OwnerManager ownerManager;
  private final byte @Nullable [] adminTokenBytes;

  public AdminResource(OwnerManager ownerManager, @Nullable String adminToken) {
    this.ownerManager = ownerManager;
    this.adminTokenBytes = (adminToken == null || adminToken.isBlank())
        ? null : adminToken.getBytes();
  }

  @POST
  @Path("/owners/{id}/tier")
  public Response setTier(@HeaderParam("Authorization") @Nullable String authHeader,
                          @PathParam("id") UUID ownerId,
                          Map<String, String> body) {
    Response authFailure = checkAuth(authHeader);
    if (authFailure != null) return authFailure;

    if (body == null || body.get("tier") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", "missing_field", "field", "tier")).build();
    }
    Tier tier;
    try {
      tier = Tier.valueOf(body.get("tier"));
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", "invalid_tier", "value", body.get("tier"))).build();
    }

    boolean updated = ownerManager.updateTier(new Identifier(ownerId), tier);
    if (!updated) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(Map.of("error", "owner_not_found")).build();
    }
    Owner owner = ownerManager.get(new Identifier(ownerId)).orElseThrow();
    AUDIT.info("admin.tier.set ownerId={} tier={}", ownerId, tier);
    return Response.ok(owner).build();
  }

  /**
   * Validates the admin bearer token in constant time. Returns null on success;
   * returns a populated Response on failure (caller should short-circuit with it).
   */
  private @Nullable Response checkAuth(@Nullable String authHeader) {
    if (adminTokenBytes == null) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE)
          .entity(Map.of(
              "error", "admin_disabled",
              "message", "Set MOTIF_ADMIN_TOKEN to enable admin endpoints"))
          .build();
    }
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(Map.of("error", "missing_bearer_token")).build();
    }
    byte[] presented = authHeader.substring("Bearer ".length()).getBytes();
    if (presented.length != adminTokenBytes.length
        || !MessageDigest.isEqual(presented, adminTokenBytes)) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(Map.of("error", "invalid_token")).build();
    }
    return null;
  }
}
