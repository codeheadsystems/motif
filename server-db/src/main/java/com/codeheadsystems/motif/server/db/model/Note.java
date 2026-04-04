package com.codeheadsystems.motif.server.db.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record Note(Identifier ownerIdentifier,
                   Identifier subjectIdentifier,
                   String value,
                   @Nullable List<Tag> tags,
                   @Nullable Identifier identifier,
                   @Nullable Identifier eventIdentifier,
                   @Nullable Timestamp timestamp) {

  public Note {
    Objects.requireNonNull(ownerIdentifier, "ownerIdentifier cannot be null");
    Objects.requireNonNull(subjectIdentifier, "subjectIdentifier cannot be null");
    value = Objects.requireNonNull(value, "value cannot be null").strip();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value cannot be empty");
    }
    if (value.length() > 4096) {
      throw new IllegalArgumentException("value cannot be longer than 4096 characters");
    }
    tags = tags == null ? List.of() : List.copyOf(tags);
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
    private Identifier ownerIdentifier;
    private Identifier subjectIdentifier;
    private String value;
    private List<Tag> tags;
    private Identifier identifier;
    private Identifier eventIdentifier;
    private Timestamp timestamp;

    private Builder() {
    }

    private static Builder from(Note note) {
      Objects.requireNonNull(note, "note cannot be null");
      Builder builder = new Builder();
      builder.ownerIdentifier = note.ownerIdentifier();
      builder.subjectIdentifier = note.subjectIdentifier();
      builder.value = note.value();
      builder.tags = note.tags();
      builder.identifier = note.identifier();
      builder.eventIdentifier = note.eventIdentifier();
      builder.timestamp = note.timestamp();
      return builder;
    }

    public Builder owner(Owner owner) {
      return ownerIdentifier(owner.identifier());
    }

    public Builder ownerIdentifier(Identifier ownerIdentifier) {
      this.ownerIdentifier = ownerIdentifier;
      return this;
    }

    public Builder subject(Subject subject) {
      return subjectIdentifier(subject.identifier());
    }

    public Builder subjectIdentifier(Identifier subjectIdentifier) {
      this.subjectIdentifier = subjectIdentifier;
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
      return eventIdentifier(event.identifier());
    }

    public Builder eventIdentifier(Identifier eventIdentifier) {
      this.eventIdentifier = eventIdentifier;
      return this;
    }

    public Builder timestamp(Timestamp timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Note build() {
      return new Note(ownerIdentifier, subjectIdentifier, value, tags, identifier, eventIdentifier, timestamp);
    }
  }
}
