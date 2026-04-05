package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.model.Owner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerResourceTest {

  private static final Owner OWNER = new Owner("ALICE");
  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");

  @Mock
  private OwnerResolver ownerResolver;

  private OwnerResource resource;

  @BeforeEach
  void setUp() {
    when(ownerResolver.resolve(PRINCIPAL)).thenReturn(OWNER);
    resource = new OwnerResource(ownerResolver);
  }

  @Test
  void getOrCreateOwnerReturnsOwner() {
    Owner result = resource.getOrCreateOwner(PRINCIPAL);

    assertThat(result.value()).isEqualTo("ALICE");
  }
}
