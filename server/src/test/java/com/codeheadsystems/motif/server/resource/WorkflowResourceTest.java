package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.PatternManager;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.manager.WorkflowManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import com.codeheadsystems.motif.server.db.model.PeriodClassification;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Tier;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import com.codeheadsystems.motif.server.db.model.Workflow;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowResourceTest {

  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");
  private static final Owner PREMIUM = Owner.builder().value("ALICE").tier(Tier.PREMIUM).build();

  @Mock
  private WorkflowManager workflowManager;
  @Mock
  private PatternManager patternManager;
  @Mock
  private SubjectManager subjectManager;
  @Mock
  private OwnerResolver ownerResolver;

  private WorkflowResource resource;

  @BeforeEach
  void setUp() {
    lenient().when(ownerResolver.resolve(PRINCIPAL)).thenReturn(PREMIUM);
    resource = new WorkflowResource(workflowManager, patternManager, subjectManager, ownerResolver);
  }

  @Test
  void createReturns201WithStepsParsedFromBody() {
    Response response = resource.create(PRINCIPAL, Map.of(
        "name", "Garden Watering",
        "description", "weekly",
        "steps", List.of(
            Map.of("name", "Water"),
            Map.of("name", "Inspect", "expectedDurationSeconds", 180))));

    assertThat(response.getStatus()).isEqualTo(201);
    Workflow saved = (Workflow) response.getEntity();
    assertThat(saved.steps()).hasSize(2);
    assertThat(saved.steps()).extracting(s -> s.name())
        .containsExactly("Water", "Inspect");
    verify(workflowManager).save(eq(PREMIUM), any(Workflow.class));
  }

  @Test
  void createReturns400OnMissingName() {
    Response response = resource.create(PRINCIPAL, Map.of("steps", List.of()));
    assertThat(response.getStatus()).isEqualTo(400);
    verify(workflowManager, never()).save(any(), any());
  }

  @Test
  void createReturns400OnStepWithoutName() {
    Response response = resource.create(PRINCIPAL, Map.of(
        "name", "X",
        "steps", List.of(Map.of("expectedDurationSeconds", 60))));
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void updateReturns404WhenWorkflowMissing() {
    UUID id = UUID.randomUUID();
    when(workflowManager.get(eq(PREMIUM), eq(new Identifier(id)))).thenReturn(Optional.empty());

    Response response = resource.update(PRINCIPAL, id, Map.of("name", "X"));

    assertThat(response.getStatus()).isEqualTo(404);
    verify(workflowManager, never()).save(any(), any());
  }

  @Test
  void fromPatternReturns404WhenPatternMissing() {
    UUID patternId = UUID.randomUUID();
    when(patternManager.get(eq(PREMIUM), eq(new Identifier(patternId)))).thenReturn(Optional.empty());

    Response response = resource.fromPattern(PRINCIPAL, patternId);

    assertThat(response.getStatus()).isEqualTo(404);
    verify(workflowManager, never()).save(any(), any());
  }

  @Test
  void fromPatternCreatesSingleStepStarter() {
    UUID patternId = UUID.randomUUID();
    Identifier subjectId = new Identifier();
    Subject subject = new Subject(PREMIUM.identifier(), new Identifier(), "Jade Plant",
        subjectId, null);
    Pattern pattern = Pattern.builder()
        .ownerIdentifier(PREMIUM.identifier())
        .subjectIdentifier(subjectId)
        .identifier(new Identifier(patternId))
        .eventValue("watered")
        .period(PeriodClassification.WEEKLY)
        .intervalMeanSeconds(7L * 86400)
        .occurrenceCount(4)
        .confidence(1.0)
        .lastSeenAt(new Timestamp(Instant.parse("2026-04-25T00:00:00Z")))
        .nextExpectedAt(new Timestamp(Instant.parse("2026-05-02T00:00:00Z")))
        .score(4.0)
        .detectedAt(new Timestamp(Instant.parse("2026-04-25T00:00:00Z")))
        .build();
    when(patternManager.get(eq(PREMIUM), eq(new Identifier(patternId))))
        .thenReturn(Optional.of(pattern));
    when(subjectManager.get(eq(PREMIUM), eq(subjectId))).thenReturn(Optional.of(subject));
    when(workflowManager.findByOwner(eq(PREMIUM), any())).thenReturn(
        new com.codeheadsystems.motif.common.Page<>(List.of(), 0, 200, false));

    Response response = resource.fromPattern(PRINCIPAL, patternId);

    assertThat(response.getStatus()).isEqualTo(201);
    Workflow saved = (Workflow) response.getEntity();
    assertThat(saved.steps()).hasSize(1);
    assertThat(saved.steps().get(0).name()).isEqualTo("watered");
    assertThat(saved.steps().get(0).expectedDurationSeconds()).isEqualTo(7L * 86400);
    assertThat(saved.description()).contains("Jade Plant").contains("watered");
  }
}
