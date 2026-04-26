package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.CategoryManager;
import com.codeheadsystems.motif.server.db.manager.CategoryManager.CategoryInUseException;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
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
@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.category");

  private final CategoryManager categoryManager;
  private final OwnerResolver ownerResolver;

  @Inject
  public CategoryResource(final CategoryManager categoryManager, final OwnerResolver ownerResolver) {
    this.categoryManager = categoryManager;
    this.ownerResolver = ownerResolver;
  }

  @GET
  public Page<Category> list(@Auth HofmannPrincipal principal,
                             @QueryParam("page") @DefaultValue("0") int page,
                             @QueryParam("size") @DefaultValue("50") int size) {
    Owner owner = ownerResolver.resolve(principal);
    return categoryManager.findByOwner(owner, new PageRequest(page, size));
  }

  @GET
  @Path("/{id}")
  public Response get(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    return categoryManager.get(owner, new Identifier(id))
        .map(c -> Response.ok(c).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(@Auth HofmannPrincipal principal, Map<String, String> body) {
    if (body == null
        || body.get("name") == null
        || body.get("color") == null
        || body.get("icon") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required fields: name, color, icon").build();
    }
    Owner owner = ownerResolver.resolve(principal);
    Category category;
    try {
      category = Category.builder()
          .owner(owner)
          .name(body.get("name"))
          .color(body.get("color"))
          .icon(body.get("icon"))
          .build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    Category persisted = categoryManager.findOrCreate(category);
    AUDIT.info("category.created owner={} id={} name={}",
        owner.value(), persisted.identifier().uuid(), persisted.name());
    return Response.status(Response.Status.CREATED).entity(persisted).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@Auth HofmannPrincipal principal,
                         @PathParam("id") UUID id,
                         Map<String, String> body) {
    if (body == null
        || body.get("name") == null
        || body.get("color") == null
        || body.get("icon") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required fields: name, color, icon").build();
    }
    Owner owner = ownerResolver.resolve(principal);
    Category updated;
    try {
      updated = Category.builder()
          .owner(owner)
          .identifier(new Identifier(id))
          .name(body.get("name"))
          .color(body.get("color"))
          .icon(body.get("icon"))
          .build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    categoryManager.update(updated);
    AUDIT.info("category.updated owner={} id={}", owner.value(), id);
    return Response.ok(updated).build();
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    try {
      boolean deleted = categoryManager.delete(owner, new Identifier(id));
      if (deleted) {
        AUDIT.info("category.deleted owner={} id={}", owner.value(), id);
        return Response.noContent().build();
      }
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (CategoryInUseException e) {
      return Response.status(Response.Status.CONFLICT)
          .entity(Map.of(
              "error", "category_in_use",
              "subjectCount", e.subjectCount(),
              "message", "Category has subjects; reassign or delete them first"))
          .build();
    }
  }
}
