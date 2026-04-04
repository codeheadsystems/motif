package com.codeheadsystems.motif.server.db.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record Event(Identifier ownerIdentifier,
                    Subject subject,
                    String value,
                    @Nullable Identifier identifier,
                    @Nullable Timestamp timestamp,
                    @Nullable List<Tag> tags) {

  public Event {
    Objects.requireNonNull(ownerIdentifier, "ownerIdentifier cannot be null");
    Objects.requireNonNull(subject, "subject cannot be null");
    value = Objects.requireNonNull(value, "value cannot be null").strip();
    if (value.length() > 256) {
      throw new IllegalArgumentException("value cannot be longer than 256 characters");
    }
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
    timestamp = Objects.requireNonNullElse(timestamp, new Timestamp());
    tags = tags == null ? List.of() : List.copyOf(tags);
    if (!ownerIdentifier.equals(subject.ownerIdentifier())) {
      throw new IllegalArgumentException("owner must match subject's owner");
    }
  }

  public static Builder from(Event event) {
    return Builder.from(event);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Identifier ownerIdentifier;
    private Subject subject;
    private String value;
    private Identifier identifier;
    private Timestamp timestamp;
    private List<Tag> tags;

    private Builder() {
    }

    private static Builder from(Event event) {
      Objects.requireNonNull(event, "event cannot be null");
      Builder builder = new Builder();
      builder.ownerIdentifier = event.ownerIdentifier();
      builder.subject = event.subject();
      builder.value = event.value();
      builder.identifier = event.identifier();
      builder.timestamp = event.timestamp();
      builder.tags = event.tags();
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
      this.subject = subject;
      return this;
    }

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public Builder identifier(Identifier identifier) {
      this.identifier = identifier;
      return this;
    }

    public Builder timestamp(Timestamp timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder tags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public Event build() {
      return new Event(ownerIdentifier, subject, value, identifier, timestamp, tags);
    }
  }
}
