package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("/api/subjects")
@Produces(MediaType.APPLICATION_JSON)
public class SubjectResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.subject");

  private final SubjectManager subjectManager;
  private final OwnerResolver ownerResolver;

  @Inject
  public SubjectResource(final SubjectManager subjectManager, final OwnerResolver ownerResolver) {
    this.subjectManager = subjectManager;
    this.ownerResolver = ownerResolver;
  }

  @GET
  public Response list(@Auth HofmannPrincipal principal,
                       @QueryParam("category") UUID categoryId,
                       @QueryParam("page") @DefaultValue("0") int page,
                       @QueryParam("size") @DefaultValue("50") int size) {
    if (categoryId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required query param: category (uuid)").build();
    }
    Owner owner = ownerResolver.resolve(principal);
    Page<Subject> result = subjectManager.findByCategory(
        owner, new Identifier(categoryId), new PageRequest(page, size));
    return Response.ok(result).build();
  }

  @GET
  @Path("/{id}")
  public Response get(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    return subjectManager.get(owner, new Identifier(id))
        .map(s -> Response.ok(s).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(@Auth HofmannPrincipal principal, Map<String, String> body) {
    if (body == null || body.get("categoryId") == null || body.get("value") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required fields: categoryId, value").build();
    }
    UUID categoryUuid = parseUuidOrNull(body.get("categoryId"));
    if (categoryUuid == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Invalid UUID for categoryId").build();
    }
    Owner owner = ownerResolver.resolve(principal);
    Subject subject = new Subject(owner.identifier(), new Identifier(categoryUuid), body.get("value"));
    subjectManager.store(subject);
    AUDIT.info("subject.created owner={} id={} categoryId={}",
        owner.value(), subject.identifier().uuid(), categoryUuid);
    return Response.status(Response.Status.CREATED).entity(subject).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@Auth HofmannPrincipal principal, @PathParam("id") UUID id, Map<String, String> body) {
    if (body == null || body.get("categoryId") == null || body.get("value") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required fields: categoryId, value").build();
    }
    UUID categoryUuid = parseUuidOrNull(body.get("categoryId"));
    if (categoryUuid == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Invalid UUID for categoryId").build();
    }
    Owner owner = ownerResolver.resolve(principal);
    Subject subject = Subject.builder()
        .ownerIdentifier(owner.identifier())
        .identifier(new Identifier(id))
        .categoryIdentifier(new Identifier(categoryUuid))
        .value(body.get("value"))
        .build();
    subjectManager.update(subject);
    AUDIT.info("subject.updated owner={} id={}", owner.value(), id);
    return Response.ok(subject).build();
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    // Look up the subject first so deletion is owner-scoped (avoid IDOR via direct delete-by-id).
    return subjectManager.get(owner, new Identifier(id))
        .map(subject -> {
          subjectManager.delete(subject);
          AUDIT.info("subject.deleted owner={} id={}", owner.value(), id);
          return Response.noContent().build();
        })
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  private static UUID parseUuidOrNull(String s) {
    try {
      return UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
