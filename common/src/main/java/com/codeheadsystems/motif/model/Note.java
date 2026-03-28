package com.codeheadsystems.motif.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record Note(Owner owner,
                   Subject subject,
                   @Nullable String value,
                   @Nullable List<Tag> tags,
                   @Nullable Identifier identifier,
                   @Nullable Event event,
                   @Nullable Timestamp timestamp) {

  public Note {
    Objects.requireNonNull(owner, "owner cannot be null");
    Objects.requireNonNull(subject, "subject cannot be null");
    value = Objects.requireNonNullElse(value, "").strip();
    if (value.length() > 4096) {
      throw new IllegalArgumentException("value cannot be longer than 4096 characters");
    }
    tags = Objects.requireNonNullElseGet(tags, List::of);
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
    timestamp = Objects.requireNonNullElse(timestamp, new Timestamp());
  }

  public static Builder from(Note note) {
    return Builder.from(note);
  }

  public static Builder builder(Owner owner, Subject subject) {
    return new Builder(owner, subject);
  }

  public static class Builder {
    private final Owner owner;
    private final Subject subject;
    private String value;
    private List<Tag> tags;
    private Identifier identifier;
    private Event event;
    private Timestamp timestamp;

    private Builder(Owner owner, Subject subject) {
      this.owner = Objects.requireNonNull(owner, "owner cannot be null");
      this.subject = Objects.requireNonNull(subject, "subject cannot be null");
    }

    private static Builder from(Note note) {
      Objects.requireNonNull(note, "note cannot be null");
      Builder builder = new Builder(note.owner(), note.subject());
      builder.value = note.value();
      builder.tags = note.tags();
      builder.identifier = note.identifier();
      builder.event = note.event();
      builder.timestamp = note.timestamp();
      return builder;
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
