package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.OwnerManager;
import com.codeheadsystems.motif.server.db.model.Owner;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerResolverTest {

  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");

  @Mock
  private OwnerManager ownerManager;

  private OwnerResolver ownerResolver;

  @BeforeEach
  void setUp() {
    ownerResolver = new OwnerResolver(ownerManager);
  }

  @Test
  void resolveReturnsExistingOwner() {
    Owner existing = new Owner("ALICE");
    when(ownerManager.find("ALICE")).thenReturn(Optional.of(existing));

    Owner result = ownerResolver.resolve(PRINCIPAL);

    assertThat(result).isSameAs(existing);
    verify(ownerManager, never()).store(any());
  }

  @Test
  void resolveCreatesOwnerWhenNotFound() {
    when(ownerManager.find("ALICE")).thenReturn(Optional.empty());

    Owner result = ownerResolver.resolve(PRINCIPAL);

    assertThat(result.value()).isEqualTo("ALICE");
    verify(ownerManager).store(any(Owner.class));
  }
}
