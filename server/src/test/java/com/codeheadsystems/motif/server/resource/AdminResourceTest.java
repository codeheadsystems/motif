package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminResourceTest {

  private static final String TOKEN = "secret-admin-token";
  private static final String VALID_AUTH = "Bearer " + TOKEN;

  @Mock
  private OwnerManager ownerManager;

  @Test
  void unsetTokenReturns503() {
    AdminResource resource = new AdminResource(ownerManager, null);

    Response response = resource.setTier(VALID_AUTH, UUID.randomUUID(),
        Map.of("tier", "PREMIUM"));

    assertThat(response.getStatus()).isEqualTo(503);
    verify(ownerManager, never()).updateTier(any(), any());
  }

  @Test
  void blankTokenReturns503() {
    AdminResource resource = new AdminResource(ownerManager, "   ");

    Response response = resource.setTier(VALID_AUTH, UUID.randomUUID(),
        Map.of("tier", "PREMIUM"));

    assertThat(response.getStatus()).isEqualTo(503);
  }

  @Test
  void missingHeaderReturns401() {
    AdminResource resource = new AdminResource(ownerManager, TOKEN);

    Response response = resource.setTier(null, UUID.randomUUID(), Map.of("tier", "PREMIUM"));

    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void wrongTokenReturns401() {
    AdminResource resource = new AdminResource(ownerManager, TOKEN);

    Response response = resource.setTier("Bearer not-the-token", UUID.randomUUID(),
        Map.of("tier", "PREMIUM"));

    assertThat(response.getStatus()).isEqualTo(401);
    verify(ownerManager, never()).updateTier(any(), any());
  }

  @Test
  void invalidTierStringReturns400() {
    AdminResource resource = new AdminResource(ownerManager, TOKEN);

    Response response = resource.setTier(VALID_AUTH, UUID.randomUUID(),
        Map.of("tier", "GOLD"));

    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void unknownOwnerReturns404() {
    AdminResource resource = new AdminResource(ownerManager, TOKEN);
    UUID id = UUID.randomUUID();
    when(ownerManager.updateTier(new Identifier(id), Tier.PREMIUM)).thenReturn(false);

    Response response = resource.setTier(VALID_AUTH, id, Map.of("tier", "PREMIUM"));

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void validRequestPromotesAndReturnsOwner() {
    AdminResource resource = new AdminResource(ownerManager, TOKEN);
    UUID id = UUID.randomUUID();
    Owner promoted = Owner.builder().value("ALICE")
        .identifier(new Identifier(id)).tier(Tier.PREMIUM).build();
    when(ownerManager.updateTier(new Identifier(id), Tier.PREMIUM)).thenReturn(true);
    when(ownerManager.get(new Identifier(id))).thenReturn(Optional.of(promoted));

    Response response = resource.setTier(VALID_AUTH, id, Map.of("tier", "PREMIUM"));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getEntity()).isEqualTo(promoted);
    verify(ownerManager).updateTier(new Identifier(id), Tier.PREMIUM);
  }
}
