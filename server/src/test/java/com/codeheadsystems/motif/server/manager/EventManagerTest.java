package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Category;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
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
class EventManagerTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");
  private static final Timestamp TIMESTAMP = new Timestamp(Instant.parse("2026-03-28T12:00:00Z"));
  private static final Event EVENT = Event.builder()
      .owner(OWNER).subject(SUBJECT).value("something happened")
      .timestamp(TIMESTAMP).build();

  @Mock
  private EventDao eventDao;
  @Mock
  private TagsDao tagsDao;

  private EventManager eventManager;

  @BeforeEach
  void setUp() {
    eventManager = new EventManager(eventDao, tagsDao);
  }

  // --- store ---

  @Test
  void storeCallsUpsertWithNoTags() {
    eventManager.store(EVENT);

    verify(eventDao).upsert(
        EVENT.identifier().uuid(),
        EVENT.ownerIdentifier().uuid(),
        EVENT.subject().identifier().uuid(),
        EVENT.value(),
        EVENT.timestamp().toOffsetDateTime());
    verifyNoMoreInteractions(tagsDao);
  }

  @Test
  void storeWithTagsCallsInsertTagForEach() {
    Event withTags = Event.from(EVENT).tags(List.of(new Tag("A"), new Tag("B"))).build();

    eventManager.store(withTags);

    verify(tagsDao).insertTag(withTags.identifier().uuid(), "A");
    verify(tagsDao).insertTag(withTags.identifier().uuid(), "B");
    verifyNoMoreInteractions(tagsDao);
  }

  // --- get ---

  @Test
  void getDelegatesToDao() {
    when(eventDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.of(EVENT));

    Optional<Event> result = eventManager.get(OWNER, EVENT.identifier());

    assertThat(result).contains(EVENT);
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(eventDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), unknown.uuid()))
        .thenReturn(Optional.empty());

    assertThat(eventManager.get(OWNER, unknown)).isEmpty();
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenRowDeleted() {
    when(eventDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(1);

    assertThat(eventManager.delete(OWNER, EVENT.identifier())).isTrue();
  }

  @Test
  void deleteReturnsFalseWhenNoRowDeleted() {
    when(eventDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(0);

    assertThat(eventManager.delete(OWNER, EVENT.identifier())).isFalse();
  }

  // --- update ---

  @Test
  void updateStoresWhenEventExists() {
    when(eventDao.findByOwnerAndIdentifier(EVENT.ownerIdentifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.of(EVENT));

    Event updated = Event.from(EVENT).value("updated").build();
    assertThat(eventManager.update(updated)).isTrue();

    verify(eventDao).upsert(
        updated.identifier().uuid(),
        updated.ownerIdentifier().uuid(),
        updated.subject().identifier().uuid(),
        "updated",
        updated.timestamp().toOffsetDateTime());
  }

  @Test
  void updateReturnsFalseWhenEventDoesNotExist() {
    when(eventDao.findByOwnerAndIdentifier(EVENT.ownerIdentifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThat(eventManager.update(EVENT)).isFalse();
    verifyNoMoreInteractions(tagsDao);
  }

  // --- findBySubject ---

  @Test
  void findBySubjectDelegatesToDao() {
    List<Event> expected = List.of(EVENT);
    when(eventDao.findByOwnerAndSubject(OWNER.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(expected);

    assertThat(eventManager.findBySubject(OWNER, SUBJECT)).isEqualTo(expected);
  }

  // --- findByTimeRange ---

  @Test
  void findByTimeRangeDelegatesToDao() {
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    List<Event> expected = List.of(EVENT);
    when(eventDao.findByOwnerAndTimeRange(
        OWNER.identifier().uuid(), from.toOffsetDateTime(), to.toOffsetDateTime()))
        .thenReturn(expected);

    assertThat(eventManager.findByTimeRange(OWNER, from, to)).isEqualTo(expected);
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeDelegatesToDao() {
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    List<Event> expected = List.of(EVENT);
    when(eventDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(), SUBJECT.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime()))
        .thenReturn(expected);

    assertThat(eventManager.findBySubjectAndTimeRange(OWNER, SUBJECT, from, to))
        .isEqualTo(expected);
  }
}
