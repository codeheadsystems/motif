package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import jakarta.ws.rs.core.Response;
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
class SubjectResourceTest {

  private static final Owner OWNER = new Owner("ALICE");
  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");
  private static final Identifier CATEGORY_ID = new Identifier();

  @Mock
  private SubjectManager subjectManager;
  @Mock
  private OwnerResolver ownerResolver;

  private SubjectResource resource;

  @BeforeEach
  void setUp() {
    // lenient: not every test path resolves the principal (e.g. validation rejects before auth check)
    lenient().when(ownerResolver.resolve(PRINCIPAL)).thenReturn(OWNER);
    resource = new SubjectResource(subjectManager, ownerResolver);
  }

  @Test
  void listReturnsSubjects() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY_ID, "sub-1");
    when(subjectManager.findByCategory(eq(OWNER), any(Identifier.class), any(PageRequest.class)))
        .thenReturn(new Page<>(List.of(subject), 0, 50, false));

    Response response = resource.list(PRINCIPAL, CATEGORY_ID.uuid(), 0, 50);

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void listReturns400WhenCategoryMissing() {
    Response response = resource.list(PRINCIPAL, null, 0, 50);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void getReturnsSubject() {
    Subject subject = new Subject(OWNER.identifier(), CATEGORY_ID, "sub-1");
    when(subjectManager.get(eq(OWNER), any()))
        .thenReturn(Optional.of(subject));

    Response response = resource.get(PRINCIPAL, subject.identifier().uuid());

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void getReturns404WhenNotFound() {
    when(subjectManager.get(eq(OWNER), any()))
        .thenReturn(Optional.empty());

    Response response = resource.get(PRINCIPAL, UUID.randomUUID());

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void createReturns201() {
    Response response = resource.create(PRINCIPAL,
        Map.of("categoryId", CATEGORY_ID.uuid().toString(), "value", "new-subject"));

    assertThat(response.getStatus()).isEqualTo(201);
  }

  @Test
  void createReturns400OnMissingFields() {
    Response response = resource.create(PRINCIPAL, Map.of("value", "no-category"));
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void createReturns400OnInvalidCategoryUuid() {
    Response response = resource.create(PRINCIPAL,
        Map.of("categoryId", "not-a-uuid", "value", "x"));
    assertThat(response.getStatus()).isEqualTo(400);
  }
}
