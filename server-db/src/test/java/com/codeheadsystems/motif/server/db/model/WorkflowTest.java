package com.codeheadsystems.motif.server.db.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowTest {

  private static final Identifier OWNER_ID = new Identifier();

  @Test
  void rejectsNullOwner() {
    assertThatThrownBy(() -> Workflow.builder().name("X").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("ownerIdentifier cannot be null");
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> Workflow.builder().ownerIdentifier(OWNER_ID).name("  ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name cannot be empty");
  }

  @Test
  void renumbersStepsToContiguous1Based() {
    // Pass positions 5, 10, 15 — should come out as 1, 2, 3.
    Workflow workflow = Workflow.builder()
        .ownerIdentifier(OWNER_ID).name("X")
        .steps(List.of(
            new WorkflowStep(null, 5, "first", null, null),
            new WorkflowStep(null, 10, "second", null, null),
            new WorkflowStep(null, 15, "third", null, null)))
        .build();
    assertThat(workflow.steps()).extracting(WorkflowStep::position)
        .containsExactly(1, 2, 3);
    assertThat(workflow.steps()).extracting(WorkflowStep::name)
        .containsExactly("first", "second", "third");
  }

  @Test
  void sortsOutOfOrderStepsByPosition() {
    Workflow workflow = Workflow.builder()
        .ownerIdentifier(OWNER_ID).name("X")
        .steps(List.of(
            new WorkflowStep(null, 3, "third", null, null),
            new WorkflowStep(null, 1, "first", null, null),
            new WorkflowStep(null, 2, "second", null, null)))
        .build();
    assertThat(workflow.steps()).extracting(WorkflowStep::name)
        .containsExactly("first", "second", "third");
  }

  @Test
  void emptyStepsAllowed() {
    Workflow workflow = Workflow.builder()
        .ownerIdentifier(OWNER_ID).name("X").build();
    assertThat(workflow.steps()).isEmpty();
  }

  @Test
  void stepsListIsImmutable() {
    Workflow workflow = Workflow.builder()
        .ownerIdentifier(OWNER_ID).name("X")
        .steps(List.of(new WorkflowStep(null, 1, "only", null, null)))
        .build();
    assertThatThrownBy(() -> workflow.steps().add(new WorkflowStep(null, 2, "x", null, null)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void buildAutoGeneratesIdentifierAndTimestamps() {
    Workflow workflow = Workflow.builder().ownerIdentifier(OWNER_ID).name("X").build();
    assertThat(workflow.identifier()).isNotNull();
    assertThat(workflow.createdAt()).isNotNull();
    assertThat(workflow.updatedAt()).isNotNull();
  }
}
