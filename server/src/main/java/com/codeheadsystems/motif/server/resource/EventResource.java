package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.EventManager;
import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Tag;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Path("/api/events")
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {

  private final EventManager eventManager;
  private final SubjectManager subjectManager;
  private final OwnerManager ownerManager;

  @Inject
  public EventResource(final EventManager eventManager,
                       final SubjectManager subjectManager,
                       final OwnerManager ownerManager) {
    this.eventManager = eventManager;
    this.subjectManager = subjectManager;
    this.ownerManager = ownerManager;
  }

  @GET
  public Response list(@Auth HofmannPrincipal principal,
                       @QueryParam("subject") UUID subjectId,
                       @QueryParam("page") @DefaultValue("0") int page,
                       @QueryParam("size") @DefaultValue("50") int size) {
    Owner owner = resolveOwner(principal);
    return subjectManager.getSubject(owner, new Identifier(subjectId))
        .map(subject -> {
          Page<Event> events = eventManager.findBySubject(owner, subject, new PageRequest(page, size));
          return Response.ok(events).build();
        })
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @GET
  @Path("/{id}")
  public Response get(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = resolveOwner(principal);
    return eventManager.get(owner, new Identifier(id))
        .map(e -> Response.ok(e).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @SuppressWarnings("unchecked")
  @POST
  public Response create(@Auth HofmannPrincipal principal, Map<String, Object> body) {
    Owner owner = resolveOwner(principal);
    UUID subjectId = UUID.fromString((String) body.get("subjectId"));
    return subjectManager.getSubject(owner, new Identifier(subjectId))
        .map(subject -> {
          List<Tag> tags = ((List<String>) body.getOrDefault("tags", Collections.emptyList()))
              .stream().map(Tag::new).toList();
          Event event = Event.builder()
              .owner(owner).subject(subject)
              .value((String) body.get("value"))
              .tags(tags)
              .build();
          eventManager.store(event);
          return Response.status(Response.Status.CREATED).entity(event).build();
        })
        .orElse(Response.status(Response.Status.NOT_FOUND).entity("Subject not found").build());
  }

  @SuppressWarnings("unchecked")
  @PUT
  @Path("/{id}")
  public Response update(@Auth HofmannPrincipal principal, @PathParam("id") UUID id, Map<String, Object> body) {
    Owner owner = resolveOwner(principal);
    return eventManager.get(owner, new Identifier(id))
        .map(existing -> {
          List<Tag> tags = ((List<String>) body.getOrDefault("tags", Collections.emptyList()))
              .stream().map(Tag::new).toList();
          Event updated = Event.from(existing)
              .value((String) body.get("value"))
              .tags(tags)
              .build();
          eventManager.update(updated);
          return Response.ok(updated).build();
        })
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = resolveOwner(principal);
    boolean deleted = eventManager.delete(owner, new Identifier(id));
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
