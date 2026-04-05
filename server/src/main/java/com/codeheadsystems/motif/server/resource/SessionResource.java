package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import io.dropwizard.auth.Auth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the HttpOnly session cookie. The client calls POST after OPAQUE auth
 * to persist the JWT in a cookie, and DELETE to log out.
 */
@Path("/api/session")
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.session");
  private static final String COOKIE_NAME = "motif_jwt";

  /**
   * Sets an HttpOnly cookie containing the JWT. The client must send the token
   * as a Bearer header (validated by HofmannBundle); this endpoint just persists
   * it as a cookie so future requests are authenticated automatically.
   */
  @POST
  public Response createSession(@Auth HofmannPrincipal principal,
                                @Context HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing Bearer token").build();
    }
    String token = authHeader.substring("Bearer ".length());
    AUDIT.info("session.created credentialId={}", principal.credentialIdentifier());
    NewCookie cookie = new NewCookie.Builder(COOKIE_NAME)
        .value(token)
        .path("/")
        .httpOnly(true)
        .sameSite(NewCookie.SameSite.STRICT)
        .build();
    return Response.noContent().cookie(cookie).build();
  }

  /**
   * Clears the session cookie, effectively logging the user out.
   */
  @DELETE
  public Response deleteSession(@Auth HofmannPrincipal principal) {
    AUDIT.info("session.deleted credentialId={}", principal.credentialIdentifier());
    NewCookie cookie = new NewCookie.Builder(COOKIE_NAME)
        .value("")
        .path("/")
        .httpOnly(true)
        .sameSite(NewCookie.SameSite.STRICT)
        .maxAge(0)
        .build();
    return Response.noContent().cookie(cookie).build();
  }
}
