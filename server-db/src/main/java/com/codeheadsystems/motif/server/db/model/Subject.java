package com.codeheadsystems.motif.server.db.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;


/**
 * Subjects are value objects that live in a category. They are strings less than
 * 128 chars.
 *
 * @param ownerIdentifier The identifier of the owner.
 * @param category        The category of the subject.
 * @param value           The value of the subject.
 * @param identifier      The identifier of the subject.
 */
public record Subject(Identifier ownerIdentifier, Category category, String value, @Nullable Identifier identifier) {

  public Subject {
    Objects.requireNonNull(ownerIdentifier, "ownerIdentifier cannot be null");
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

  public Subject(Identifier ownerIdentifier, Category category, String value) {
    this(ownerIdentifier, category, value, null);
  }

  public static Builder from(Subject subject) {
    return Builder.from(subject);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Identifier ownerIdentifier;
    private Category category;
    private String value;
    private Identifier identifier;

    private Builder() {
    }

    private static Builder from(Subject subject) {
      Objects.requireNonNull(subject, "subject cannot be null");
      Builder builder = new Builder();
      builder.ownerIdentifier = subject.ownerIdentifier();
      builder.category = subject.category();
      builder.value = subject.value();
      builder.identifier = subject.identifier();
      return builder;
    }

    public Builder owner(Owner owner) {
      return ownerIdentifier(owner.identifier());
    }

    public Builder ownerIdentifier(Identifier ownerIdentifier) {
      this.ownerIdentifier = ownerIdentifier;
      return this;
    }

    public Builder category(Category category) {
      this.category = category;
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

    public Subject build() {
      return new Subject(ownerIdentifier, category, value, identifier);
    }
  }
}
