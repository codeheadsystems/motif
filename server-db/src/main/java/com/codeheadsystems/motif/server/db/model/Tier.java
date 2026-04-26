package com.codeheadsystems.motif.server.db.model;

/**
 * Subscription tier for an Owner. Ordering matters: a request requiring at least
 * {@code PREMIUM} is satisfied by both {@code PREMIUM} and {@code BUSINESS} owners.
 *
 * <ul>
 *   <li>{@link #FREE_SYNCED} — has an account and cloud sync, no paid features. Default
 *       for newly-created owners. Note: pure-local Android free users have no Owner row
 *       at all; the FREE_SYNCED label here means "free, but synced".</li>
 *   <li>{@link #PREMIUM} — individual paid plan; unlocks Projects, Workflows, Attachments.</li>
 *   <li>{@link #BUSINESS} — organization plan; everything in PREMIUM plus collaboration.</li>
 * </ul>
 */
public enum Tier {
  FREE_SYNCED,
  PREMIUM,
  BUSINESS;

  /** True if this tier satisfies the requirement of {@code required}. */
  public boolean satisfies(Tier required) {
    return this.ordinal() >= required.ordinal();
  }
}
