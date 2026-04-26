package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.ProjectManager;
import com.codeheadsystems.motif.server.db.manager.TierRequiredException;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Project;
import com.codeheadsystems.motif.server.db.model.ProjectStatus;
import com.codeheadsystems.motif.server.db.model.Tier;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectResourceTest {

  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");
  private static final Owner PREMIUM = Owner.builder().value("ALICE").tier(Tier.PREMIUM).build();

  @Mock
  private ProjectManager projectManager;
  @Mock
  private OwnerResolver ownerResolver;

  private ProjectResource resource;

  @BeforeEach
  void setUp() {
    lenient().when(ownerResolver.resolve(PRINCIPAL)).thenReturn(PREMIUM);
    resource = new ProjectResource(projectManager, ownerResolver);
  }

  @Test
  void createReturns201OnHappyPath() {
    Response response = resource.create(PRINCIPAL,
        Map.of("name", "Garden", "status", "ACTIVE"));

    assertThat(response.getStatus()).isEqualTo(201);
    verify(projectManager).store(eq(PREMIUM), any(Project.class));
  }

  @Test
  void createReturns400OnMissingName() {
    Response response = resource.create(PRINCIPAL, Map.of("status", "ACTIVE"));
    assertThat(response.getStatus()).isEqualTo(400);
    verify(projectManager, never()).store(any(), any());
  }

  @Test
  void createReturns400OnInvalidStatus() {
    Response response = resource.create(PRINCIPAL,
        Map.of("name", "Garden", "status", "FAKE_STATUS"));
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void createDefaultsToActiveStatus() {
    Response response = resource.create(PRINCIPAL, Map.of("name", "Garden"));

    assertThat(response.getStatus()).isEqualTo(201);
    Project saved = (Project) response.getEntity();
    assertThat(saved.status()).isEqualTo(ProjectStatus.ACTIVE);
  }

  @Test
  void tierRequiredPropagatesToMapper() {
    // When the manager throws, the resource doesn't catch — the JAX-RS mapper handles it.
    when(projectManager.findByOwner(any(), any())).thenThrow(
        new TierRequiredException(Tier.PREMIUM, Tier.FREE_SYNCED));

    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
        resource.list(PRINCIPAL, 0, 50)
    ).isInstanceOf(TierRequiredException.class);
  }

  @Test
  void updateReturnsBadRequestOnMissingFields() {
    Response response = resource.update(PRINCIPAL, UUID.randomUUID(),
        Map.of("name", "X"));  // missing status
    assertThat(response.getStatus()).isEqualTo(400);
  }
}
