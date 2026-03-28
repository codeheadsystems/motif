package com.codeheadsystems.motif.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Subjects are value objects that live in a category. They are strings less than
 * 128 chars.
 *
 * @param category   The category of the subject.
 * @param value      The value of the subject.
 * @param identifier The identifier of the subject.
 */
public record Subject(Category category, String value, @Nullable Identifier identifier) {

  public Subject {
    Objects.requireNonNull(category, "category cannot be null");
    value = Objects.requireNonNull(value, "value cannot be null")
        .strip();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value cannot be empty");
    }
    if (value.length() > 128) {
      throw new IllegalArgumentException("value cannot be longer than 128 characters");
    }
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
  }

  public Subject(Category category, String value) {
    this(category, value, null);
  }

  public static Builder from(Subject subject) {
    return Builder.from(subject);
  }

  public static Builder builder(Category category, String value) {
    return new Builder(category, value);
  }

  public static class Builder {
    private final Category category;
    private final String value;
    private Identifier identifier;

    private Builder(Category category, String value) {
      this.category = Objects.requireNonNull(category, "category cannot be null");
      this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    private static Builder from(Subject subject) {
      Objects.requireNonNull(subject, "subject cannot be null");
      Builder builder = new Builder(subject.category(), subject.value());
      builder.identifier = subject.identifier();
      return builder;
    }

    public Builder identifier(Identifier identifier) {
      this.identifier = identifier;
      return this;
    }

    public Subject build() {
      return new Subject(category, value, identifier);
    }
  }
}
