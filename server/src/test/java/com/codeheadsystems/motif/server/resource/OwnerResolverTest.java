package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.CategoryManager;
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
  @Mock
  private CategoryManager categoryManager;

  private OwnerResolver ownerResolver;

  @BeforeEach
  void setUp() {
    ownerResolver = new OwnerResolver(ownerManager, categoryManager);
  }

  @Test
  void resolveReturnsExistingOwner() {
    Owner existing = new Owner("ALICE");
    when(ownerManager.find("ALICE")).thenReturn(Optional.of(existing));

    Owner result = ownerResolver.resolve(PRINCIPAL);

    assertThat(result).isSameAs(existing);
    verify(ownerManager, never()).findOrCreate("ALICE");
    verify(categoryManager, never()).seedDefaults(existing);
  }

  @Test
  void resolveCreatesOwnerAndSeedsCategoriesWhenNotFound() {
    Owner created = new Owner("ALICE");
    when(ownerManager.find("ALICE")).thenReturn(Optional.empty());
    when(ownerManager.findOrCreate("ALICE")).thenReturn(created);

    Owner result = ownerResolver.resolve(PRINCIPAL);

    assertThat(result).isSameAs(created);
    verify(ownerManager).findOrCreate("ALICE");
    verify(categoryManager).seedDefaults(created);
  }
}
