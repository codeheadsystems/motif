package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;

/**
 * Manager-layer tier gate. Call {@link #require(Owner, Tier)} at the entry of any manager
 * method that touches a Premium- or Business-only entity. Defense in depth: enforcement also
 * happens via auth + resource filters, but the manager is the last line.
 */
public final class Tiers {

  private Tiers() {}

  /**
   * @throws TierRequiredException if {@code owner.tier} does not satisfy {@code required}
   */
  public static void require(Owner owner, Tier required) {
    if (!owner.tier().satisfies(required)) {
      throw new TierRequiredException(required, owner);
    }
  }
}
