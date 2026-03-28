package com.codeheadsystems.motif.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record Event(Subject subject,
                    String value,
                    @Nullable Identifier identifier,
                    @Nullable Timestamp timestamp,
                    @Nullable List<Tag> tags) {

  public Event {
    Objects.requireNonNull(subject, "subject cannot be null");
    value = Objects.requireNonNull(value, "value cannot be null").strip();
    if (value.length() > 256) {
      throw new IllegalArgumentException("value cannot be longer than 256 characters");
    }
    identifier = Objects.requireNonNullElseGet(identifier, () -> new Identifier(Event.class));
    timestamp = Objects.requireNonNullElse(timestamp, new Timestamp());
    tags = Objects.requireNonNullElseGet(tags, List::of);
  }

  public static Builder from(Event event) {
    return Builder.from(event);
  }

  public static Builder builder(Subject subject, String value) {
    return new Builder(subject, value);
  }

  public static class Builder {
    private final Subject subject;
    private final String value;
    private Identifier identifier;
    private Timestamp timestamp;
    private List<Tag> tags;

    private Builder(Subject subject, String value) {
      this.subject = Objects.requireNonNull(subject, "subject cannot be null");
      this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    private static Builder from(Event event) {
      Objects.requireNonNull(event, "event cannot be null");
      Builder builder = new Builder(event.subject(), event.value());
      builder.identifier = event.identifier();
      builder.timestamp = event.timestamp();
      builder.tags = event.tags();
      return builder;
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
      return new Event(subject, value, identifier, timestamp, tags);
    }
  }
}
