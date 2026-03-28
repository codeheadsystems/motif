package com.codeheadsystems.motif.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Owner are strings that are between 1 and 256 chars in size, inclusive.
 * They will be forced to be upper-case.
 *
 * @param value      The owner value.
 * @param identifier The identifier of the owner.
 */
public record Owner(String value, @Nullable Identifier identifier) {

  public Owner {
    value = Objects.requireNonNull(value, "value cannot be null")
        .strip()
        .toUpperCase();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value cannot be empty");
    }
    if (value.length() > 256) {
      throw new IllegalArgumentException("value cannot be longer than 256 characters");
    }
    identifier = Objects.requireNonNullElseGet(identifier, Identifier::new);
  }

  public Owner(String value) {
    this(value, null);
  }

  public static Builder from(Owner owner) {
    return Builder.from(owner);
  }

  public static Builder builder(String value) {
    return new Builder(value);
  }

  public static class Builder {
    private final String value;
    private Identifier identifier;

    private Builder(String value) {
      this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    private static Builder from(Owner owner) {
      Objects.requireNonNull(owner, "owner cannot be null");
      Builder builder = new Builder(owner.value());
      builder.identifier = owner.identifier();
      return builder;
    }

    public Builder identifier(Identifier identifier) {
      this.identifier = identifier;
      return this;
    }

    public Owner build() {
      return new Owner(value, identifier);
    }
  }
}
