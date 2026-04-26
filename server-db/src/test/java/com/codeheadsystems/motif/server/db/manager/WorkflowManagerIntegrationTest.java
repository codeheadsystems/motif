package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.WorkflowDao;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;
import com.codeheadsystems.motif.server.db.model.Workflow;
import com.codeheadsystems.motif.server.db.model.WorkflowStep;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowManagerIntegrationTest extends DatabaseTest {

  private OwnerDao ownerDao;
  private WorkflowManager workflowManager;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM workflow_steps");
      handle.execute("DELETE FROM workflows");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    workflowManager = new WorkflowManager(jdbi, jdbi.onDemand(WorkflowDao.class));
  }

  private Owner createOwner(String value, Tier tier) {
    Owner owner = Owner.builder().value(value).tier(tier).build();
    ownerDao.upsert(owner.identifier().uuid(), owner.value(), false, tier.name());
    return owner;
  }

  @Test
  void freeSyncedOwnerCannotSaveWorkflow() {
    Owner free = createOwner("FREE", Tier.FREE_SYNCED);
    Workflow workflow = Workflow.builder().owner(free).name("X").build();

    assertThatThrownBy(() -> workflowManager.save(free, workflow))
        .isInstanceOf(TierRequiredException.class);
  }

  @Test
  void premiumOwnerCanSaveAndRoundtripWithSteps() {
    Owner premium = createOwner("PREMIUM", Tier.PREMIUM);
    Workflow workflow = Workflow.builder().owner(premium).name("Garden Watering")
        .description("weekly cycle")
        .steps(List.of(
            new WorkflowStep(null, 1, "Water", 600L, "use lukewarm water"),
            new WorkflowStep(null, 2, "Inspect for pests", 180L, null),
            new WorkflowStep(null, 3, "Log it", null, null)))
        .build();

    workflowManager.save(premium, workflow);

    Workflow fetched = workflowManager.get(premium, workflow.identifier()).orElseThrow();
    assertThat(fetched.name()).isEqualTo("Garden Watering");
    assertThat(fetched.description()).isEqualTo("weekly cycle");
    assertThat(fetched.steps()).extracting(WorkflowStep::position).containsExactly(1, 2, 3);
    assertThat(fetched.steps()).extracting(WorkflowStep::name)
        .containsExactly("Water", "Inspect for pests", "Log it");
    assertThat(fetched.steps().get(0).expectedDurationSeconds()).isEqualTo(600L);
    assertThat(fetched.steps().get(2).expectedDurationSeconds()).isNull();
  }

  @Test
  void saveReplacesExistingStepsInOneTransaction() {
    Owner premium = createOwner("PREMIUM", Tier.PREMIUM);
    Workflow original = Workflow.builder().owner(premium).name("Process")
        .steps(List.of(
            new WorkflowStep(null, 1, "alpha", null, null),
            new WorkflowStep(null, 2, "beta", null, null),
            new WorkflowStep(null, 3, "gamma", null, null)))
        .build();
    workflowManager.save(premium, original);

    // Replace with a single different step.
    Workflow updated = Workflow.from(original)
        .steps(List.of(new WorkflowStep(null, 1, "only-step", null, null)))
        .build();
    workflowManager.save(premium, updated);

    Workflow fetched = workflowManager.get(premium, original.identifier()).orElseThrow();
    assertThat(fetched.steps()).hasSize(1);
    assertThat(fetched.steps().get(0).name()).isEqualTo("only-step");
  }

  @Test
  void listReturnsHeadersOnly() {
    Owner premium = createOwner("PREMIUM", Tier.PREMIUM);
    Workflow workflow = Workflow.builder().owner(premium).name("With Steps")
        .steps(List.of(new WorkflowStep(null, 1, "a", null, null),
            new WorkflowStep(null, 2, "b", null, null)))
        .build();
    workflowManager.save(premium, workflow);

    var listed = workflowManager.findByOwner(premium, PageRequest.first());
    assertThat(listed.items()).hasSize(1);
    // The list endpoint returns headers — the steps list is empty for cheap rendering.
    assertThat(listed.items().get(0).steps()).isEmpty();
  }

  @Test
  void deleteCascadesToSteps() {
    Owner premium = createOwner("PREMIUM", Tier.PREMIUM);
    Workflow workflow = Workflow.builder().owner(premium).name("X")
        .steps(List.of(new WorkflowStep(null, 1, "s1", null, null)))
        .build();
    workflowManager.save(premium, workflow);

    assertThat(workflowManager.delete(premium, workflow.identifier())).isTrue();

    long remainingSteps = jdbi.withHandle(h ->
        h.createQuery("SELECT COUNT(*) FROM workflow_steps WHERE workflow_uuid = ?")
            .bind(0, workflow.identifier().uuid())
            .mapTo(Long.class).one());
    assertThat(remainingSteps).isZero();
  }

  @Test
  void ownerIsolation() {
    Owner alice = createOwner("ALICE", Tier.PREMIUM);
    Owner bob = createOwner("BOB", Tier.PREMIUM);
    Workflow aliceWorkflow = Workflow.builder().owner(alice).name("Alice's").build();
    workflowManager.save(alice, aliceWorkflow);

    assertThat(workflowManager.get(alice, aliceWorkflow.identifier())).isPresent();
    assertThat(workflowManager.get(bob, aliceWorkflow.identifier())).isEmpty();
    assertThat(workflowManager.delete(bob, aliceWorkflow.identifier())).isFalse();
  }

  @Test
  void updatePreservesCreatedAt() throws InterruptedException {
    Owner premium = createOwner("PREMIUM", Tier.PREMIUM);
    Workflow original = Workflow.builder().owner(premium).name("X").build();
    workflowManager.save(premium, original);
    var origCreated = workflowManager.get(premium, original.identifier()).orElseThrow().createdAt();

    Thread.sleep(10);
    Workflow updated = Workflow.from(original).description("changed").build();
    workflowManager.save(premium, updated);

    Workflow fetched = workflowManager.get(premium, original.identifier()).orElseThrow();
    assertThat(fetched.createdAt().timestamp().toEpochMilli())
        .isEqualTo(origCreated.timestamp().toEpochMilli());
    assertThat(fetched.updatedAt().timestamp().toEpochMilli())
        .isGreaterThanOrEqualTo(origCreated.timestamp().toEpochMilli());
    assertThat(fetched.description()).isEqualTo("changed");
  }
}
