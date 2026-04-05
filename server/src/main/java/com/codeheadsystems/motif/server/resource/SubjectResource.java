package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.model.Category;
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
import java.util.List;
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
  private final OwnerManager ownerManager;

  @Inject
  public SubjectResource(final SubjectManager subjectManager, final OwnerManager ownerManager) {
    this.subjectManager = subjectManager;
    this.ownerManager = ownerManager;
  }

  @GET
  @Path("/categories")
  public List<Category> categories(@Auth HofmannPrincipal principal) {
    Owner owner = resolveOwner(principal);
    return subjectManager.findCategories(owner);
  }

  @GET
  public Page<Subject> list(@Auth HofmannPrincipal principal,
                            @QueryParam("category") String category,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("50") int size) {
    Owner owner = resolveOwner(principal);
    return subjectManager.findByCategory(owner, new Category(category), new PageRequest(page, size));
  }

  @GET
  @Path("/{id}")
  public Response get(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = resolveOwner(principal);
    return subjectManager.getSubject(owner, new Identifier(id))
        .map(s -> Response.ok(s).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(@Auth HofmannPrincipal principal, Map<String, String> body) {
    if (body == null || body.get("category") == null || body.get("value") == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing required fields: category, value").build();
    }
    Owner owner = resolveOwner(principal);
    Subject subject = new Subject(owner.identifier(), new Category(body.get("category")), body.get("value"));
    subjectManager.store(subject);
    AUDIT.info("subject.created owner={} id={} category={}", owner.value(), subject.identifier().uuid(), body.get("category"));
    return Response.status(Response.Status.CREATED).entity(subject).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@Auth HofmannPrincipal principal, @PathParam("id") UUID id, Map<String, String> body) {
    if (body == null || body.get("category") == null || body.get("value") == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing required fields: category, value").build();
    }
    Owner owner = resolveOwner(principal);
    Subject subject = Subject.builder()
        .ownerIdentifier(owner.identifier())
        .identifier(new Identifier(id))
        .category(new Category(body.get("category")))
        .value(body.get("value"))
        .build();
    subjectManager.update(subject);
    AUDIT.info("subject.updated owner={} id={}", owner.value(), id);
    return Response.ok(subject).build();
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = resolveOwner(principal);
    Subject subject = Subject.builder()
        .ownerIdentifier(owner.identifier())
        .identifier(new Identifier(id))
        .category(new Category("placeholder"))
        .value("placeholder")
        .build();
    boolean deleted = subjectManager.delete(subject);
    if (deleted) {
      AUDIT.info("subject.deleted owner={} id={}", owner.value(), id);
    }
    return deleted ? Response.noContent().build() : Response.status(Response.Status.NOT_FOUND).build();
  }

  private Owner resolveOwner(HofmannPrincipal principal) {
    String credId = principal.credentialIdentifier();
    return ownerManager.find(credId)
        .orElseGet(() -> {
          Owner owner = new Owner(credId);
          ownerManager.store(owner);
          return owner;
        });
  }
}
