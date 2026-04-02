package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.dao.NoteDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Note;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Tag;
import com.codeheadsystems.motif.server.model.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
  private NoteDao noteDao;
  @Mock
  private TagsDao tagsDao;

  private NoteManager noteManager;

  @BeforeEach
  void setUp() {
    noteManager = new NoteManager(noteDao, tagsDao);
  }

  // --- store ---

  @Test
  void storeCallsUpsertWithCorrectArgs() {
    noteManager.store(NOTE);

    verify(noteDao).upsert(
        NOTE.identifier().uuid(),
        NOTE.ownerIdentifier().uuid(),
        NOTE.subjectIdentifier().uuid(),
        NOTE.eventIdentifier().uuid(),
        NOTE.value(),
        NOTE.timestamp().toOffsetDateTime());
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
  }

  @Test
  void storeWithTagsCallsInsertTagForEach() {
    Note withTags = Note.from(NOTE).tags(List.of(new Tag("A"), new Tag("B"))).build();

    noteManager.store(withTags);

    verify(tagsDao).insertTag(withTags.identifier().uuid(), "A");
    verify(tagsDao).insertTag(withTags.identifier().uuid(), "B");
    verifyNoMoreInteractions(tagsDao);
  }

  // --- get ---

  @Test
  void getDelegatesToDao() {
    when(noteDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(Optional.of(NOTE));

    Optional<Note> result = noteManager.get(OWNER, NOTE.identifier());

    assertThat(result).contains(NOTE);
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(noteDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), unknown.uuid()))
        .thenReturn(Optional.empty());

    assertThat(noteManager.get(OWNER, unknown)).isEmpty();
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenRowDeleted() {
    when(noteDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(1);

    assertThat(noteManager.delete(OWNER, NOTE.identifier())).isTrue();
  }

  @Test
  void deleteReturnsFalseWhenNoRowDeleted() {
    when(noteDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(0);

    assertThat(noteManager.delete(OWNER, NOTE.identifier())).isFalse();
  }

  // --- update ---

  @Test
  void updateStoresWhenNoteExists() {
    when(noteDao.findByOwnerAndIdentifier(NOTE.ownerIdentifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(Optional.of(NOTE));

    Note updated = Note.from(NOTE).value("updated").build();
    assertThat(noteManager.update(updated)).isTrue();

    verify(noteDao).upsert(
        updated.identifier().uuid(),
        updated.ownerIdentifier().uuid(),
        updated.subjectIdentifier().uuid(),
        updated.eventIdentifier().uuid(),
        "updated",
        updated.timestamp().toOffsetDateTime());
  }

  @Test
  void updateReturnsFalseWhenNoteDoesNotExist() {
    when(noteDao.findByOwnerAndIdentifier(NOTE.ownerIdentifier().uuid(), NOTE.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThat(noteManager.update(NOTE)).isFalse();
    verifyNoMoreInteractions(tagsDao);
  }

  // --- findBySubject ---

  @Test
  void findBySubjectDelegatesToDao() {
    List<Note> expected = List.of(NOTE);
    when(noteDao.findByOwnerAndSubject(OWNER.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(expected);

    assertThat(noteManager.findBySubject(OWNER, SUBJECT)).isEqualTo(expected);
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeDelegatesToDao() {
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    List<Note> expected = List.of(NOTE);
    when(noteDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(), SUBJECT.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime()))
        .thenReturn(expected);

    assertThat(noteManager.findBySubjectAndTimeRange(OWNER, SUBJECT, from, to))
        .isEqualTo(expected);
  }

  // --- findByEvent ---

  @Test
  void findByEventDelegatesToDao() {
    List<Note> expected = List.of(NOTE);
    when(noteDao.findByOwnerAndEvent(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(expected);

    assertThat(noteManager.findByEvent(OWNER, EVENT.identifier())).isEqualTo(expected);
  }

  // --- findByEventAndTimeRange ---

  @Test
  void findByEventAndTimeRangeDelegatesToDao() {
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    List<Note> expected = List.of(NOTE);
    when(noteDao.findByOwnerEventAndTimeRange(
        OWNER.identifier().uuid(), EVENT.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime()))
        .thenReturn(expected);

    assertThat(noteManager.findByEventAndTimeRange(OWNER, EVENT.identifier(), from, to))
        .isEqualTo(expected);
  }
}
