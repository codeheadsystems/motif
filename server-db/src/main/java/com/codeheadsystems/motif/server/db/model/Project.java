package com.codeheadsystems.motif.server.db.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Project (Premium tier): an initiative that groups Subjects across Categories. A
 * Project has its own lifecycle (ACTIVE → PAUSED/COMPLETED/ARCHIVED) independent of its
 * Subjects.
 *
 * @param ownerIdentifier owner this project belongs to
 * @param name            display name, 1–128 characters; unique per owner
 * @param description     freeform description, ≤ 2048 characters; null permitted
 * @param status          lifecycle state
 * @param createdAt       when the project was first created
 * @param updatedAt       when the project was last touched
 * @param identifier      stable identifier; auto-generated if null
 */
public record Project(
    Identifier ownerIdentifier,
    String name,
    @Nullable String description,
    ProjectStatus status,
    Timestamp createdAt,
    Timestamp updatedAt,
    @Nullable Identifier identifier) {

  public Project {
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
    Objects.requireNonNull(status, "status cannot be null");
    Objects.requireNonNull(createdAt, "createdAt cannot be null");
    Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
  }

  public static Builder from(Project project) {
    return Builder.from(project);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Identifier ownerIdentifier;
    private String name;
    private String description;
    private ProjectStatus status = ProjectStatus.ACTIVE;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Identifier identifier;

    private Builder() {}

    private static Builder from(Project project) {
      Objects.requireNonNull(project, "project cannot be null");
      Builder b = new Builder();
      b.ownerIdentifier = project.ownerIdentifier();
      b.name = project.name();
      b.description = project.description();
      b.status = project.status();
      b.createdAt = project.createdAt();
      b.updatedAt = project.updatedAt();
      b.identifier = project.identifier();
      return b;
    }

    public Builder owner(Owner owner) { return ownerIdentifier(owner.identifier()); }
    public Builder ownerIdentifier(Identifier v) { this.ownerIdentifier = v; return this; }
    public Builder name(String v) { this.name = v; return this; }
    public Builder description(@Nullable String v) { this.description = v; return this; }
    public Builder status(ProjectStatus v) { this.status = v; return this; }
    public Builder createdAt(Timestamp v) { this.createdAt = v; return this; }
    public Builder updatedAt(Timestamp v) { this.updatedAt = v; return this; }
    public Builder identifier(Identifier v) { this.identifier = v; return this; }

    public Project build() {
      Timestamp now = createdAt == null || updatedAt == null ? new Timestamp() : null;
      return new Project(
          ownerIdentifier, name, description, status,
          createdAt == null ? now : createdAt,
          updatedAt == null ? now : updatedAt,
          identifier);
    }
  }
}
