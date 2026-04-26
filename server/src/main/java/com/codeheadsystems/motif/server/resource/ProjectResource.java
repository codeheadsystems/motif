package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.ProjectManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Project;
import com.codeheadsystems.motif.server.db.model.ProjectStatus;
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

/**
 * Premium-tier endpoints for Project CRUD. Tier enforcement happens in {@link ProjectManager};
 * an authenticated FREE_SYNCED owner who hits any of these endpoints gets a 402 Payment
 * Required from {@link TierRequiredExceptionMapper}.
 */
@Singleton
@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.project");

  private final ProjectManager projectManager;
  private final OwnerResolver ownerResolver;

  @Inject
  public ProjectResource(final ProjectManager projectManager, final OwnerResolver ownerResolver) {
    this.projectManager = projectManager;
    this.ownerResolver = ownerResolver;
  }

  @GET
  public Page<Project> list(@Auth HofmannPrincipal principal,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("50") int size) {
    Owner owner = ownerResolver.resolve(principal);
    return projectManager.findByOwner(owner, new PageRequest(page, size));
  }

  @GET
  @Path("/{id}")
  public Response get(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    return projectManager.get(owner, new Identifier(id))
        .map(p -> Response.ok(p).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(@Auth HofmannPrincipal principal, Map<String, String> body) {
    if (body == null || body.get("name") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required field: name").build();
    }
    ProjectStatus status = parseStatus(body.get("status"));
    if (body.get("status") != null && status == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Invalid status; expected one of ACTIVE/PAUSED/COMPLETED/ARCHIVED").build();
    }

    Owner owner = ownerResolver.resolve(principal);
    Project project;
    try {
      project = Project.builder()
          .owner(owner)
          .name(body.get("name"))
          .description(body.get("description"))
          .status(status == null ? ProjectStatus.ACTIVE : status)
          .build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    projectManager.store(owner, project);
    AUDIT.info("project.created owner={} id={} name={}",
        owner.value(), project.identifier().uuid(), project.name());
    return Response.status(Response.Status.CREATED).entity(project).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@Auth HofmannPrincipal principal,
                         @PathParam("id") UUID id,
                         Map<String, String> body) {
    if (body == null || body.get("name") == null || body.get("status") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required fields: name, status").build();
    }
    ProjectStatus status = parseStatus(body.get("status"));
    if (status == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Invalid status; expected one of ACTIVE/PAUSED/COMPLETED/ARCHIVED").build();
    }

    Owner owner = ownerResolver.resolve(principal);
    Project updated;
    try {
      updated = Project.builder()
          .owner(owner)
          .identifier(new Identifier(id))
          .name(body.get("name"))
          .description(body.get("description"))
          .status(status)
          .build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    projectManager.update(owner, updated);
    AUDIT.info("project.updated owner={} id={} status={}", owner.value(), id, status);
    return Response.ok(updated).build();
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    boolean deleted = projectManager.delete(owner, new Identifier(id));
    if (deleted) {
      AUDIT.info("project.deleted owner={} id={}", owner.value(), id);
      return Response.noContent().build();
    }
    return Response.status(Response.Status.NOT_FOUND).build();
  }

  private static ProjectStatus parseStatus(String s) {
    if (s == null) return null;
    try {
      return ProjectStatus.valueOf(s);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
