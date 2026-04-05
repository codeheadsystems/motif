package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectResourceTest {

  private static final Owner OWNER = new Owner("ALICE");
  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");

  @Mock
  private SubjectManager subjectManager;
  @Mock
  private OwnerResolver ownerResolver;

  private SubjectResource resource;

  @BeforeEach
  void setUp() {
    when(ownerResolver.resolve(PRINCIPAL)).thenReturn(OWNER);
    resource = new SubjectResource(subjectManager, ownerResolver);
  }

  @Test
  void listReturnsSubjects() {
    Subject subject = new Subject(OWNER.identifier(), new Category("test"), "sub-1");
    when(subjectManager.findByCategory(eq(OWNER), any(Category.class), any(PageRequest.class)))
        .thenReturn(new Page<>(List.of(subject), 0, 50, false));

    Page<Subject> result = resource.list(PRINCIPAL, "test", 0, 50);

    assertThat(result.items()).hasSize(1);
  }

  @Test
  void getReturnsSubject() {
    Subject subject = new Subject(OWNER.identifier(), new Category("test"), "sub-1");
    when(subjectManager.get(eq(OWNER), any()))
        .thenReturn(Optional.of(subject));

    Response response = resource.get(PRINCIPAL, subject.identifier().uuid());

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void getReturns404WhenNotFound() {
    when(subjectManager.get(eq(OWNER), any()))
        .thenReturn(Optional.empty());

    Response response = resource.get(PRINCIPAL, java.util.UUID.randomUUID());

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void createReturns201() {
    Response response = resource.create(PRINCIPAL, Map.of("category", "test", "value", "new-subject"));

    assertThat(response.getStatus()).isEqualTo(201);
  }
}
