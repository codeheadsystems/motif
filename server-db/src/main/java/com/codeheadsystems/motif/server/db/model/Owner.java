package com.codeheadsystems.motif.server.db.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Owner are strings that are between 1 and 256 chars in size, inclusive.
 * They will be forced to be upper-case.
 *
 * @param value      The owner value.
 * @param identifier The identifier of the owner.
 * @param deleted    Whether this owner has been soft-deleted.
 * @param tier       Subscription tier; defaults to FREE_SYNCED for newly-created owners.
 */
public record Owner(String value, @Nullable Identifier identifier, boolean deleted, Tier tier) {

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
    tier = Objects.requireNonNullElse(tier, Tier.FREE_SYNCED);
  }

  public Owner(String value) {
    this(value, null, false, Tier.FREE_SYNCED);
  }

  /** Convenience constructor preserved for callers that don't care about tier. Defaults FREE_SYNCED. */
  public Owner(String value, @Nullable Identifier identifier, boolean deleted) {
    this(value, identifier, deleted, Tier.FREE_SYNCED);
  }

  public static Builder from(Owner owner) {
    return Builder.from(owner);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String value;
    private Identifier identifier;
    private boolean deleted;
    private Tier tier;

    private Builder() {
    }

    private static Builder from(Owner owner) {
      Objects.requireNonNull(owner, "owner cannot be null");
      Builder builder = new Builder();
      builder.value = owner.value();
      builder.identifier = owner.identifier();
      builder.deleted = owner.deleted();
      builder.tier = owner.tier();
      return builder;
    }

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public Builder identifier(Identifier identifier) {
      this.identifier = identifier;
      return this;
    }

    public Builder deleted(boolean deleted) {
      this.deleted = deleted;
      return this;
    }

    public Builder tier(Tier tier) {
      this.tier = tier;
      return this;
    }

    public Owner build() {
      return new Owner(value, identifier, deleted, tier);
    }
  }
}
