package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.PatternDetectionManager;
import com.codeheadsystems.motif.server.db.manager.PatternManager;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatternResourceTest {

  private static final Owner OWNER = new Owner("ALICE");
  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");

  @Mock
  private PatternManager patternManager;
  @Mock
  private PatternDetectionManager detectionManager;
  @Mock
  private OwnerResolver ownerResolver;

  private PatternResource resource;

  @BeforeEach
  void setUp() {
    lenient().when(ownerResolver.resolve(PRINCIPAL)).thenReturn(OWNER);
    resource = new PatternResource(patternManager, detectionManager, ownerResolver);
  }

  @Test
  void listClampsLimitToMax() {
    Page<Pattern> empty = new Page<>(List.of(), 0, 50, false);
    when(patternManager.findByOwner(eq(OWNER), any(PageRequest.class))).thenReturn(empty);

    resource.list(PRINCIPAL, 1000);

    verify(patternManager).findByOwner(eq(OWNER), any(PageRequest.class));
  }

  @Test
  void listClampsLimitToMin() {
    Page<Pattern> empty = new Page<>(List.of(), 0, 1, false);
    when(patternManager.findByOwner(eq(OWNER), any(PageRequest.class))).thenReturn(empty);

    Page<Pattern> result = resource.list(PRINCIPAL, 0);

    assertThat(result).isNotNull();
  }

  @Test
  void recomputeTriggersDetectorThenReturnsPage() {
    when(detectionManager.detectForOwner(OWNER)).thenReturn(3);
    Page<Pattern> page = new Page<>(List.of(), 0, 5, false);
    when(patternManager.findByOwner(eq(OWNER), any(PageRequest.class))).thenReturn(page);

    Response response = resource.recompute(PRINCIPAL, 5);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(detectionManager).detectForOwner(OWNER);
  }
}
