package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.dropwizard.auth.HofmannPrincipal;
import com.codeheadsystems.motif.server.db.manager.NoteManager;
import com.codeheadsystems.motif.server.db.manager.SubjectManager;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Note;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteResourceTest {

  private static final Owner OWNER = new Owner("ALICE");
  private static final Identifier CATEGORY_ID = new Identifier();
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY_ID, "sub-1");
  private static final HofmannPrincipal PRINCIPAL = new HofmannPrincipal("ALICE", "jti-1");

  @Mock
  private NoteManager noteManager;
  @Mock
  private SubjectManager subjectManager;
  @Mock
  private OwnerResolver ownerResolver;

  private NoteResource resource;

  @BeforeEach
  void setUp() {
    when(ownerResolver.resolve(PRINCIPAL)).thenReturn(OWNER);
    resource = new NoteResource(noteManager, subjectManager, ownerResolver);
  }

  @Test
  void getReturnsNote() {
    Note note = Note.builder().owner(OWNER).subject(SUBJECT).value("test-note").build();
    when(noteManager.get(eq(OWNER), any(Identifier.class)))
        .thenReturn(Optional.of(note));

    Response response = resource.get(PRINCIPAL, note.identifier().uuid());

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void getReturns404WhenNotFound() {
    when(noteManager.get(eq(OWNER), any(Identifier.class)))
        .thenReturn(Optional.empty());

    Response response = resource.get(PRINCIPAL, UUID.randomUUID());

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void createReturns201() {
    when(subjectManager.get(eq(OWNER), any(Identifier.class)))
        .thenReturn(Optional.of(SUBJECT));

    Response response = resource.create(PRINCIPAL,
        Map.of("subjectId", SUBJECT.identifier().uuid().toString(),
            "value", "new-note"));

    assertThat(response.getStatus()).isEqualTo(201);
  }

  @Test
  void deleteReturns204() {
    when(noteManager.delete(eq(OWNER), any(Identifier.class))).thenReturn(true);

    Response response = resource.delete(PRINCIPAL, UUID.randomUUID());

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void deleteReturns404WhenNotFound() {
    when(noteManager.delete(eq(OWNER), any(Identifier.class))).thenReturn(false);

    Response response = resource.delete(PRINCIPAL, UUID.randomUUID());

    assertThat(response.getStatus()).isEqualTo(404);
  }
}
