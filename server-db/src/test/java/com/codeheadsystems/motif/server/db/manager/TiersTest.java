package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;
import org.junit.jupiter.api.Test;

class TiersTest {

  @Test
  void freeSyncedFailsPremiumGate() {
    Owner owner = Owner.builder().value("ALICE").tier(Tier.FREE_SYNCED).build();
    assertThatThrownBy(() -> Tiers.require(owner, Tier.PREMIUM))
        .isInstanceOf(TierRequiredException.class)
        .satisfies(t -> {
          TierRequiredException tre = (TierRequiredException) t;
          assertThat(tre.required()).isEqualTo(Tier.PREMIUM);
          assertThat(tre.actual()).isEqualTo(Tier.FREE_SYNCED);
        });
  }

  @Test
  void premiumPassesPremiumGate() {
    Owner owner = Owner.builder().value("ALICE").tier(Tier.PREMIUM).build();
    Tiers.require(owner, Tier.PREMIUM);
  }

  @Test
  void businessPassesPremiumGate() {
    Owner owner = Owner.builder().value("ALICE").tier(Tier.BUSINESS).build();
    Tiers.require(owner, Tier.PREMIUM);
  }

  @Test
  void premiumFailsBusinessGate() {
    Owner owner = Owner.builder().value("ALICE").tier(Tier.PREMIUM).build();
    assertThatThrownBy(() -> Tiers.require(owner, Tier.BUSINESS))
        .isInstanceOf(TierRequiredException.class);
  }

  @Test
  void anyTierPassesFreeSyncedGate() {
    Tiers.require(Owner.builder().value("A").tier(Tier.FREE_SYNCED).build(), Tier.FREE_SYNCED);
    Tiers.require(Owner.builder().value("B").tier(Tier.PREMIUM).build(), Tier.FREE_SYNCED);
    Tiers.require(Owner.builder().value("C").tier(Tier.BUSINESS).build(), Tier.FREE_SYNCED);
  }
}
