package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.PatternManager;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.manager.WorkflowManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Workflow;
import com.codeheadsystems.motif.server.db.model.WorkflowStep;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Premium-tier endpoints for Workflow CRUD plus the from-pattern starter. Tier enforcement
 * happens in {@link WorkflowManager}; a FREE_SYNCED owner gets a 402 Payment Required.
 */
@Singleton
@Path("/api/workflows")
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowResource {

  private static final Logger AUDIT = LoggerFactory.getLogger("audit.workflow");

  private final WorkflowManager workflowManager;
  private final PatternManager patternManager;
  private final SubjectManager subjectManager;
  private final OwnerResolver ownerResolver;

  @Inject
  public WorkflowResource(final WorkflowManager workflowManager,
                          final PatternManager patternManager,
                          final SubjectManager subjectManager,
                          final OwnerResolver ownerResolver) {
    this.workflowManager = workflowManager;
    this.patternManager = patternManager;
    this.subjectManager = subjectManager;
    this.ownerResolver = ownerResolver;
  }

  @GET
  public Page<Workflow> list(@Auth HofmannPrincipal principal,
                             @QueryParam("page") @DefaultValue("0") int page,
                             @QueryParam("size") @DefaultValue("50") int size) {
    Owner owner = ownerResolver.resolve(principal);
    return workflowManager.findByOwner(owner, new PageRequest(page, size));
  }

  @GET
  @Path("/{id}")
  public Response get(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    return workflowManager.get(owner, new Identifier(id))
        .map(w -> Response.ok(w).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @SuppressWarnings("unchecked")
  @POST
  public Response create(@Auth HofmannPrincipal principal, Map<String, Object> body) {
    if (body == null || body.get("name") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required field: name").build();
    }
    Owner owner = ownerResolver.resolve(principal);
    try {
      Workflow workflow = Workflow.builder()
          .owner(owner)
          .name((String) body.get("name"))
          .description((String) body.get("description"))
          .steps(parseSteps(body.get("steps")))
          .build();
      workflowManager.save(owner, workflow);
      AUDIT.info("workflow.created owner={} id={} steps={}",
          owner.value(), workflow.identifier().uuid(), workflow.steps().size());
      return Response.status(Response.Status.CREATED).entity(workflow).build();
    } catch (IllegalArgumentException | ClassCastException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

  @SuppressWarnings("unchecked")
  @PUT
  @Path("/{id}")
  public Response update(@Auth HofmannPrincipal principal,
                         @PathParam("id") UUID id,
                         Map<String, Object> body) {
    if (body == null || body.get("name") == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing required field: name").build();
    }
    Owner owner = ownerResolver.resolve(principal);
    // Owner-scope check first so an unknown id can't disclose existence in another owner.
    if (workflowManager.get(owner, new Identifier(id)).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    try {
      Workflow workflow = Workflow.builder()
          .owner(owner)
          .identifier(new Identifier(id))
          .name((String) body.get("name"))
          .description((String) body.get("description"))
          .steps(parseSteps(body.get("steps")))
          .build();
      workflowManager.save(owner, workflow);
      AUDIT.info("workflow.updated owner={} id={} steps={}",
          owner.value(), id, workflow.steps().size());
      return Response.ok(workflow).build();
    } catch (IllegalArgumentException | ClassCastException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@Auth HofmannPrincipal principal, @PathParam("id") UUID id) {
    Owner owner = ownerResolver.resolve(principal);
    boolean deleted = workflowManager.delete(owner, new Identifier(id));
    if (deleted) {
      AUDIT.info("workflow.deleted owner={} id={}", owner.value(), id);
      return Response.noContent().build();
    }
    return Response.status(Response.Status.NOT_FOUND).build();
  }

  /**
   * Creates a 1-step starter workflow from a detected Pattern. The new workflow's name and
   * step name are the pattern's event-value; the description summarises the cadence.
   */
  @POST
  @Path("/from-pattern/{patternId}")
  public Response fromPattern(@Auth HofmannPrincipal principal,
                              @PathParam("patternId") UUID patternId) {
    Owner owner = ownerResolver.resolve(principal);
    Pattern pattern = patternManager.get(owner, new Identifier(patternId)).orElse(null);
    if (pattern == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(Map.of("error", "pattern_not_found")).build();
    }
    String subjectLabel = subjectManager.get(owner, pattern.subjectIdentifier())
        .map(Subject::value)
        .orElse("unknown subject");
    String description = String.format(
        "From detected pattern: %s every ~%s on %s",
        pattern.eventValue(),
        humanInterval(pattern.intervalMeanSeconds()),
        subjectLabel);

    WorkflowStep step = new WorkflowStep(
        null, 1, pattern.eventValue(), pattern.intervalMeanSeconds(), null);
    Workflow workflow = Workflow.builder()
        .owner(owner)
        .name(uniqueWorkflowName(owner, pattern.eventValue()))
        .description(description)
        .steps(List.of(step))
        .build();
    workflowManager.save(owner, workflow);
    AUDIT.info("workflow.from-pattern owner={} pattern={} workflow={}",
        owner.value(), patternId, workflow.identifier().uuid());
    return Response.status(Response.Status.CREATED).entity(workflow).build();
  }

  // --- helpers ---

  /**
   * The unique constraint is (owner, name); if the user has already converted a pattern
   * with the same event-value into a workflow, append a numeric suffix so the second
   * conversion doesn't 500. Best-effort: collisions beyond #50 are rare in practice.
   */
  private String uniqueWorkflowName(Owner owner, String base) {
    String candidate = base;
    for (int i = 2; i <= 50; i++) {
      Page<Workflow> existing = workflowManager.findByOwner(owner, PageRequest.first(200));
      String c = candidate;
      boolean taken = existing.items().stream().anyMatch(w -> w.name().equalsIgnoreCase(c));
      if (!taken) return candidate;
      candidate = base + " (" + i + ")";
    }
    return base + " (" + System.currentTimeMillis() + ")";
  }

  @SuppressWarnings("unchecked")
  private List<WorkflowStep> parseSteps(Object raw) {
    if (raw == null) return List.of();
    List<Map<String, Object>> stepMaps = (List<Map<String, Object>>) raw;
    List<WorkflowStep> out = new ArrayList<>(stepMaps.size());
    for (int i = 0; i < stepMaps.size(); i++) {
      Map<String, Object> m = stepMaps.get(i);
      String name = (String) m.get("name");
      if (name == null) {
        throw new IllegalArgumentException("step at index " + i + " missing name");
      }
      Long duration = m.get("expectedDurationSeconds") == null ? null
          : ((Number) m.get("expectedDurationSeconds")).longValue();
      String notes = (String) m.get("notes");
      // Position from request is ignored — the Workflow constructor renumbers from list order.
      out.add(new WorkflowStep(null, i + 1, name, duration, notes));
    }
    return out;
  }

  private static String humanInterval(long seconds) {
    if (seconds < 90 * 60) return Math.max(1, seconds / 60) + " minutes";
    if (seconds < 36 * 3600) return Math.max(1, seconds / 3600) + " hours";
    if (seconds < 12 * 86400) return Math.max(1, seconds / 86400) + " days";
    if (seconds < 60 * 86400) return Math.max(1, seconds / (7 * 86400)) + " weeks";
    return Math.max(1, seconds / (30 * 86400)) + " months";
  }
}
