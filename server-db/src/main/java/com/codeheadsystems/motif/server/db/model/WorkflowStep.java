package com.codeheadsystems.motif.server.db.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A single step within a {@link Workflow}. Position is 1-indexed and contiguous within the
 * workflow (the {@link Workflow} canonical constructor renumbers to enforce this). Duration
 * is optional and represents the typical time the user spends before moving to the next
 * step (or completing the workflow if this is the last step).
 *
 * @param identifier              stable identifier; auto-generated if null
 * @param position                1-based position; renumbered by {@link Workflow}
 * @param name                    1–128 chars
 * @param expectedDurationSeconds optional; positive when present
 * @param notes                   optional; ≤2048 chars
 */
public record WorkflowStep(
    @Nullable Identifier identifier,
    int position,
    String name,
    @Nullable Long expectedDurationSeconds,
    @Nullable String notes) {

  public WorkflowStep {
    if (position < 1) {
      throw new IllegalArgumentException("position must be >= 1");
    }
    name = Objects.requireNonNull(name, "name cannot be null").strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be empty");
    }
    if (name.length() > 128) {
      throw new IllegalArgumentException("name cannot be longer than 128 characters");
    }
    if (expectedDurationSeconds != null && expectedDurationSeconds <= 0) {
      throw new IllegalArgumentException("expectedDurationSeconds must be positive when present");
    }
    if (notes != null) {
      notes = notes.strip();
      if (notes.isEmpty()) {
        notes = null;
      } else if (notes.length() > 2048) {
        throw new IllegalArgumentException("notes cannot be longer than 2048 characters");
      }
    }
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
  }

  /** Convenience for callers that only know name + (optional) duration. */
  public static WorkflowStep of(int position, String name) {
    return new WorkflowStep(null, position, name, null, null);
  }

  /** Returns a copy of this step with a different position. Used by Workflow's renumbering. */
  WorkflowStep withPosition(int newPosition) {
    return new WorkflowStep(identifier, newPosition, name, expectedDurationSeconds, notes);
  }
}
