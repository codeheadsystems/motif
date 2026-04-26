package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;

/**
 * Thrown by manager-layer enforcement when an owner attempts an operation that requires a
 * higher subscription tier than they currently hold. Mapped to HTTP 402 Payment Required by
 * the corresponding ExceptionMapper in the server module.
 */
public class TierRequiredException extends RuntimeException {

  private final Tier required;
  private final Tier actual;

  public TierRequiredException(Tier required, Tier actual) {
    super("Operation requires tier " + required + "; owner is " + actual);
    this.required = required;
    this.actual = actual;
  }

  public TierRequiredException(Tier required, Owner owner) {
    this(required, owner.tier());
  }

  public Tier required() {
    return required;
  }

  public Tier actual() {
    return actual;
  }
}
