package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.dao.WorkflowDao;
import com.codeheadsystems.motif.server.db.dao.WorkflowStepDao;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import com.codeheadsystems.motif.server.db.model.Workflow;
import com.codeheadsystems.motif.server.db.model.WorkflowStep;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

/**
 * Premium-tier manager for Workflows. Reads return header rows alone (cheap for sidebar
 * listings); {@link #get(Owner, Identifier)} hydrates with the step list. Writes use
 * replace-all-steps semantics: every save deletes the workflow's existing rows in
 * workflow_steps and re-inserts the supplied list inside one transaction.
 */
@Singleton
public class WorkflowManager {

  private final Jdbi jdbi;
  private final WorkflowDao workflowDao;

  @Inject
  public WorkflowManager(final Jdbi jdbi, final WorkflowDao workflowDao) {
    this.jdbi = jdbi;
    this.workflowDao = workflowDao;
  }

  /** Lists workflows by owner, headers only (steps not populated). */
  public Page<Workflow> findByOwner(Owner owner, PageRequest pageRequest) {
    Tiers.require(owner, Tier.PREMIUM);
    List<Workflow> results = workflowDao.findByOwner(
        owner.identifier().uuid(),
        pageRequest.pageSize() + 1,
        pageRequest.offset());
    return Page.of(results, pageRequest);
  }

  /** Fetches a workflow with its full step list. */
  public Optional<Workflow> get(Owner owner, Identifier identifier) {
    Tiers.require(owner, Tier.PREMIUM);
    return jdbi.inTransaction(handle -> {
      WorkflowDao txWorkflowDao = handle.attach(WorkflowDao.class);
      Optional<Workflow> headerOpt = txWorkflowDao.findByOwnerAndIdentifier(
          owner.identifier().uuid(), identifier.uuid());
      if (headerOpt.isEmpty()) {
        return Optional.<Workflow>empty();
      }
      WorkflowStepDao txStepDao = handle.attach(WorkflowStepDao.class);
      List<WorkflowStep> steps = txStepDao.findByWorkflow(identifier.uuid());
      return Optional.of(Workflow.from(headerOpt.get()).steps(steps).build());
    });
  }

  /**
   * Creates the workflow if missing, otherwise overwrites it. Steps are replace-all: any
   * existing rows in workflow_steps for this workflow are deleted, then the supplied
   * steps are inserted. Single transaction.
   */
  public void save(Owner owner, Workflow workflow) {
    Tiers.require(owner, Tier.PREMIUM);
    jdbi.useTransaction(handle -> {
      WorkflowDao txWorkflowDao = handle.attach(WorkflowDao.class);
      WorkflowStepDao txStepDao = handle.attach(WorkflowStepDao.class);

      // Preserve created_at on update; use supplied/now on first insert.
      Optional<Workflow> existing = txWorkflowDao.findByOwnerAndIdentifier(
          workflow.ownerIdentifier().uuid(), workflow.identifier().uuid());
      Timestamp createdAt = existing.map(Workflow::createdAt).orElse(workflow.createdAt());
      Timestamp updatedAt = new Timestamp();

      txWorkflowDao.upsert(
          workflow.identifier().uuid(),
          workflow.ownerIdentifier().uuid(),
          workflow.name(),
          workflow.description(),
          createdAt.toOffsetDateTime(),
          updatedAt.toOffsetDateTime());

      txStepDao.deleteByWorkflow(workflow.identifier().uuid());
      for (WorkflowStep step : workflow.steps()) {
        txStepDao.insert(
            step.identifier().uuid(),
            workflow.identifier().uuid(),
            step.position(),
            step.name(),
            step.expectedDurationSeconds(),
            step.notes());
      }
    });
  }

  public boolean delete(Owner owner, Identifier identifier) {
    Tiers.require(owner, Tier.PREMIUM);
    // workflow_steps cascade-delete via FK.
    return workflowDao.deleteByOwnerAndIdentifier(
        owner.identifier().uuid(), identifier.uuid()) > 0;
  }
}
