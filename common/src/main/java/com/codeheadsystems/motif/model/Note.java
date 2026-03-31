package com.codeheadsystems.motif.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record Note(Owner owner,
                   Subject subject,
                   String value,
                   @Nullable List<Tag> tags,
                   @Nullable Identifier identifier,
                   @Nullable Event event,
                   @Nullable Timestamp timestamp) {

  public Note {
    Objects.requireNonNull(owner, "owner cannot be null");
    Objects.requireNonNull(subject, "subject cannot be null");
    value = Objects.requireNonNull(value, "value cannot be null").strip();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value cannot be empty");
    }
    if (value.length() > 4096) {
      throw new IllegalArgumentException("value cannot be longer than 4096 characters");
    }
    tags = tags == null ? List.of() : List.copyOf(tags);
    if (!owner.identifier().equals(subject.ownerIdentifier())) {
      throw new IllegalArgumentException("owner must match subject's owner");
    }
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
    timestamp = Objects.requireNonNullElse(timestamp, new Timestamp());
  }

  public static Builder from(Note note) {
    return Builder.from(note);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Owner owner;
    private Subject subject;
    private String value;
    private List<Tag> tags;
    private Identifier identifier;
    private Event event;
    private Timestamp timestamp;

    private Builder() {
    }

    private static Builder from(Note note) {
      Objects.requireNonNull(note, "note cannot be null");
      Builder builder = new Builder();
      builder.owner = note.owner();
      builder.subject = note.subject();
      builder.value = note.value();
      builder.tags = note.tags();
      builder.identifier = note.identifier();
      builder.event = note.event();
      builder.timestamp = note.timestamp();
      return builder;
    }

    public Builder owner(Owner owner) {
      this.owner = owner;
      return this;
    }

    public Builder subject(Subject subject) {
      this.subject = subject;
      return this;
    }

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public Builder tags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public Builder identifier(Identifier identifier) {
      this.identifier = identifier;
      return this;
    }

    public Builder event(Event event) {
      this.event = event;
      return this;
    }

    public Builder timestamp(Timestamp timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Note build() {
      return new Note(owner, subject, value, tags, identifier, event, timestamp);
    }
  }
}
