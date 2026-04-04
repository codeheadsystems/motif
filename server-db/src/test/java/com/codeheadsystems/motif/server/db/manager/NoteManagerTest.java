package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.dao.NoteDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Note;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Tag;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteManagerTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");
  private static final Timestamp TIMESTAMP = new Timestamp(Instant.parse("2026-03-28T12:00:00Z"));
  private static final Event EVENT = Event.builder()
      .owner(OWNER).subject(SUBJECT).value("test-event").timestamp(TIMESTAMP).build();
  private static final Note NOTE = Note.builder()
      .owner(OWNER).subject(SUBJECT).event(EVENT).value("a note")
      .timestamp(TIMESTAMP).build();

  @Mock
  private Jdbi jdbi;
  @Mock
  private Handle handle;
  @Mock
  private NoteDao noteDao;
  @Mock
  private TagsDao tagsDao;
  @Mock
  private TagsManager tagsManager;

  private NoteManager noteManager;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, HandleConsumer.class).useHandle(handle);
      return null;
    }).when(jdbi).useTransaction(any(HandleConsumer.class));
    lenient().doAnswer(invocation -> invocation.getArgument(0, HandleCallback.class).withHandle(handle))
        .when(jdbi).inTransaction(any(HandleCallback.class));
    lenient().when(handle.attach(NoteDao.class)).thenReturn(noteDao);
    lenient().when(handle.attach(TagsDao.class)).thenReturn(tagsDao);
    noteManager = new NoteManager(jdbi, noteDao, tagsManager);
  }

  // --- store ---

  @Test
  void storeCallsUpsertAndSyncsTags() {
    noteManager.store(NOTE);

    verify(noteDao).upsert(
        NOTE.identifier().uuid(),
        NOTE.ownerIdentifier().uuid(),
        NOTE.subjectIdentifier().uuid(),
        NOTE.eventIdentifier().uuid(),
        NOTE.value(),
        NOTE.timestamp().toOffsetDateTime());
    verify(tagsManager).syncTags(tagsDao, NOTE.identifier(), NOTE.tags());
  }

  @Test
  void storeNoteWithoutEvent() {
    Note noEvent = Note.builder().owner(OWNER).subject(SUBJECT).value("no event")
        .timestamp(TIMESTAMP).build();

    noteManager.store(noEvent);

    verify(noteDao).upsert(
        noEvent.identifier().uuid(),
        noEvent.ownerIdentifier().uuid(),
        noEvent.subjectIdentifier().uuid(),
        null,
        noEvent.value(),
        noEvent.timestamp().toOffsetDateTime());
    verify(tagsManager).syncTags(tagsDao, noEvent.identifier(), noEvent.tags());
  }

  @Test
  void storeWithTagsSyncsTagsWithTagsManager() {
    Note withTags = Note.from(NOTE).tags(List.of(new Tag("A"), new Tag("B"))).build();

    noteManager.store(withTags);

    verify(tagsManager).syncTags(tagsDao, withTags.identifier(), List.of(new Tag("A"), new Tag("B")));
  }

  // --- get ---

  @Test
  void getHydratesTagsFromTagsManager() {
    when(noteDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(Optional.of(NOTE));
    when(tagsManager.tagsFor(NOTE.identifier()))
        .thenReturn(List.of(new Tag("X")));

    Optional<Note> result = noteManager.get(OWNER, NOTE.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().tags()).containsExactly(new Tag("X"));
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(noteDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), unknown.uuid()))
        .thenReturn(Optional.empty());

    assertThat(noteManager.get(OWNER, unknown)).isEmpty();
    verifyNoInteractions(tagsManager);
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueAndDeletesTags() {
    when(noteDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(1);

    assertThat(noteManager.delete(OWNER, NOTE.identifier())).isTrue();
    verify(tagsManager).deleteAllTags(tagsDao, NOTE.identifier());
  }

  @Test
  void deleteReturnsFalseAndDoesNotDeleteTags() {
    when(noteDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(0);

    assertThat(noteManager.delete(OWNER, NOTE.identifier())).isFalse();
    verifyNoInteractions(tagsManager);
  }

  // --- update ---

  @Test
  void updateStoresWhenNoteExists() {
    when(noteDao.findByOwnerAndIdentifier(NOTE.ownerIdentifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(Optional.of(NOTE));

    Note updated = Note.from(NOTE).value("updated").build();
    noteManager.update(updated);

    verify(noteDao).upsert(
        updated.identifier().uuid(),
        updated.ownerIdentifier().uuid(),
        updated.subjectIdentifier().uuid(),
        updated.eventIdentifier().uuid(),
        "updated",
        updated.timestamp().toOffsetDateTime());
    verify(tagsManager).syncTags(eq(tagsDao), eq(updated.identifier()), any());
  }

  @Test
  void updateThrowsWhenNoteDoesNotExist() {
    when(noteDao.findByOwnerAndIdentifier(NOTE.ownerIdentifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteManager.update(NOTE))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(tagsManager);
  }

  // --- findBySubject ---

  @Test
  void findBySubjectHydratesTags() {
    PageRequest pr = PageRequest.first(10);
    when(noteDao.findByOwnerAndSubject(OWNER.identifier().uuid(), SUBJECT.identifier().uuid(), 11, 0))
        .thenReturn(List.of(NOTE));
    when(tagsManager.tagsFor(List.of(NOTE.identifier())))
        .thenReturn(Map.of(NOTE.identifier(), List.of(new Tag("Y"))));

    Page<Note> result = noteManager.findBySubject(OWNER, SUBJECT, pr);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().tags()).containsExactly(new Tag("Y"));
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeHydratesTags() {
    PageRequest pr = PageRequest.first(10);
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    when(noteDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(), SUBJECT.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime(), 11, 0))
        .thenReturn(List.of(NOTE));
    when(tagsManager.tagsFor(List.of(NOTE.identifier())))
        .thenReturn(Map.of(NOTE.identifier(), List.of(new Tag("Z"))));

    Page<Note> result = noteManager.findBySubjectAndTimeRange(OWNER, SUBJECT, from, to, pr);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().tags()).containsExactly(new Tag("Z"));
  }

  // --- findByEvent ---

  @Test
  void findByEventHydratesTags() {
    PageRequest pr = PageRequest.first(10);
    when(noteDao.findByOwnerAndEvent(OWNER.identifier().uuid(), EVENT.identifier().uuid(), 11, 0))
        .thenReturn(List.of(NOTE));
    when(tagsManager.tagsFor(List.of(NOTE.identifier())))
        .thenReturn(Map.of(NOTE.identifier(), List.of(new Tag("W"))));

    Page<Note> result = noteManager.findByEvent(OWNER, EVENT.identifier(), pr);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().tags()).containsExactly(new Tag("W"));
  }

  // --- findByEventAndTimeRange ---

  @Test
  void findByEventAndTimeRangeHydratesTags() {
    PageRequest pr = PageRequest.first(10);
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    when(noteDao.findByOwnerEventAndTimeRange(
        OWNER.identifier().uuid(), EVENT.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime(), 11, 0))
        .thenReturn(List.of(NOTE));
    when(tagsManager.tagsFor(List.of(NOTE.identifier())))
        .thenReturn(Map.of(NOTE.identifier(), List.of(new Tag("V"))));

    Page<Note> result = noteManager.findByEventAndTimeRange(OWNER, EVENT.identifier(), from, to, pr);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().tags()).containsExactly(new Tag("V"));
  }
}
