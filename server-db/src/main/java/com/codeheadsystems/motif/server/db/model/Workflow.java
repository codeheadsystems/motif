package com.codeheadsystems.motif.server.db.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Workflow (Premium tier): an ordered sequence of {@link WorkflowStep}s. The canonical
 * constructor sorts the supplied steps by their declared position and then renumbers them
 * 1..N — callers can pass any positive positions (or the convenience {@link WorkflowStep#of})
 * and the workflow guarantees a contiguous 1-based sequence on the way out.
 *
 * @param ownerIdentifier owner this workflow belongs to
 * @param identifier      stable identifier; auto-generated if null
 * @param name            1–128 chars; unique per owner
 * @param description     optional; ≤2048 chars
 * @param steps           ordered list of steps; never null (empty list permitted)
 * @param createdAt       when the workflow was first created
 * @param updatedAt       when the workflow was last touched
 */
public record Workflow(
    Identifier ownerIdentifier,
    @Nullable Identifier identifier,
    String name,
    @Nullable String description,
    List<WorkflowStep> steps,
    Timestamp createdAt,
    Timestamp updatedAt) {

  public Workflow {
    Objects.requireNonNull(ownerIdentifier, "ownerIdentifier cannot be null");
    name = Objects.requireNonNull(name, "name cannot be null").strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be empty");
    }
    if (name.length() > 128) {
      throw new IllegalArgumentException("name cannot be longer than 128 characters");
    }
    if (description != null) {
      description = description.strip();
      if (description.isEmpty()) {
        description = null;
      } else if (description.length() > 2048) {
        throw new IllegalArgumentException("description cannot be longer than 2048 characters");
      }
    }
    Objects.requireNonNull(createdAt, "createdAt cannot be null");
    Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);

    // Defensive copy + sort by declared position + renumber to 1..N. Defended against:
    //  - external mutation after construction (defensive copy)
    //  - callers passing positions out of order (sort)
    //  - callers passing gaps like [1,3,5] (renumber)
    List<WorkflowStep> sorted = new ArrayList<>(
        Objects.requireNonNullElseGet(steps, List::<WorkflowStep>of));
    sorted.sort(Comparator.comparingInt(WorkflowStep::position));
    List<WorkflowStep> renumbered = new ArrayList<>(sorted.size());
    for (int i = 0; i < sorted.size(); i++) {
      WorkflowStep step = sorted.get(i);
      int target = i + 1;
      renumbered.add(step.position() == target ? step : step.withPosition(target));
    }
    steps = List.copyOf(renumbered);
  }

  public static Builder from(Workflow workflow) {
    return Builder.from(workflow);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Identifier ownerIdentifier;
    private Identifier identifier;
    private String name;
    private String description;
    private List<WorkflowStep> steps = List.of();
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private Builder() {}

    private static Builder from(Workflow workflow) {
      Objects.requireNonNull(workflow, "workflow cannot be null");
      Builder b = new Builder();
      b.ownerIdentifier = workflow.ownerIdentifier();
      b.identifier = workflow.identifier();
      b.name = workflow.name();
      b.description = workflow.description();
      b.steps = workflow.steps();
      b.createdAt = workflow.createdAt();
      b.updatedAt = workflow.updatedAt();
      return b;
    }

    public Builder owner(Owner owner) { return ownerIdentifier(owner.identifier()); }
    public Builder ownerIdentifier(Identifier v) { this.ownerIdentifier = v; return this; }
    public Builder identifier(Identifier v) { this.identifier = v; return this; }
    public Builder name(String v) { this.name = v; return this; }
    public Builder description(@Nullable String v) { this.description = v; return this; }
    public Builder steps(List<WorkflowStep> v) { this.steps = v; return this; }
    public Builder createdAt(Timestamp v) { this.createdAt = v; return this; }
    public Builder updatedAt(Timestamp v) { this.updatedAt = v; return this; }

    public Workflow build() {
      Timestamp now = createdAt == null || updatedAt == null ? new Timestamp() : null;
      return new Workflow(
          ownerIdentifier, identifier, name, description, steps,
          createdAt == null ? now : createdAt,
          updatedAt == null ? now : updatedAt);
    }
  }
}
