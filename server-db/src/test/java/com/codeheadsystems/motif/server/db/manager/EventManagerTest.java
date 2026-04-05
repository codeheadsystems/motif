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
import com.codeheadsystems.motif.server.db.dao.EventDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Tag;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.time.Instant;
import java.util.List;
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
class EventManagerTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");
  private static final Category CATEGORY = new Category("test-category");
  private static final Subject SUBJECT = new Subject(OWNER.identifier(), CATEGORY, "test-subject");
  private static final Timestamp TIMESTAMP = new Timestamp(Instant.parse("2026-03-28T12:00:00Z"));
  private static final Event EVENT = Event.builder()
      .owner(OWNER).subject(SUBJECT).value("something happened")
      .timestamp(TIMESTAMP).build();

  @Mock
  private Jdbi jdbi;
  @Mock
  private Handle handle;
  @Mock
  private EventDao eventDao;
  @Mock
  private TagsDao tagsDao;
  @Mock
  private TagsManager tagsManager;

  private EventManager eventManager;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, HandleConsumer.class).useHandle(handle);
      return null;
    }).when(jdbi).useTransaction(any(HandleConsumer.class));
    lenient().doAnswer(invocation -> invocation.getArgument(0, HandleCallback.class).withHandle(handle))
        .when(jdbi).inTransaction(any(HandleCallback.class));
    lenient().when(handle.attach(EventDao.class)).thenReturn(eventDao);
    lenient().when(handle.attach(TagsDao.class)).thenReturn(tagsDao);
    eventManager = new EventManager(jdbi, eventDao, tagsManager);
  }

  // --- store ---

  @Test
  void storeCallsUpsertAndSyncsTags() {
    eventManager.store(EVENT);

    verify(eventDao).upsert(
        EVENT.identifier().uuid(),
        EVENT.ownerIdentifier().uuid(),
        EVENT.subject().identifier().uuid(),
        EVENT.value(),
        EVENT.timestamp().toOffsetDateTime());
    verify(tagsManager).syncTags(tagsDao, EVENT.identifier(), EVENT.tags());
  }

  @Test
  void storeWithTagsSyncsTagsWithTagsManager() {
    Event withTags = Event.from(EVENT).tags(List.of(new Tag("A"), new Tag("B"))).build();

    eventManager.store(withTags);

    verify(tagsManager).syncTags(tagsDao, withTags.identifier(), List.of(new Tag("A"), new Tag("B")));
  }

  // --- get ---

  @Test
  void getHydratesTagsFromTagsManager() {
    Event hydrated = Event.from(EVENT).tags(List.of(new Tag("X"))).build();
    when(eventDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.of(EVENT));
    when(tagsManager.hydrate(eq(EVENT), any(), any())).thenReturn(hydrated);

    Optional<Event> result = eventManager.get(OWNER, EVENT.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().tags()).containsExactly(new Tag("X"));
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(eventDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), unknown.uuid()))
        .thenReturn(Optional.empty());

    assertThat(eventManager.get(OWNER, unknown)).isEmpty();
    verifyNoInteractions(tagsManager);
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueAndDeletesTags() {
    when(eventDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(1);

    assertThat(eventManager.delete(OWNER, EVENT.identifier())).isTrue();
    verify(tagsManager).deleteAllTags(tagsDao, EVENT.identifier());
  }

  @Test
  void deleteReturnsFalseAndDoesNotDeleteTags() {
    when(eventDao.deleteByOwnerAndIdentifier(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(0);

    assertThat(eventManager.delete(OWNER, EVENT.identifier())).isFalse();
    verifyNoInteractions(tagsManager);
  }

  // --- update ---

  @Test
  void updateStoresWhenEventExists() {
    when(eventDao.findByOwnerAndIdentifier(EVENT.ownerIdentifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.of(EVENT));

    Event updated = Event.from(EVENT).value("updated").build();
    when(tagsManager.hydrate(eq(updated), any(), any())).thenReturn(updated);

    Event result = eventManager.update(updated);

    assertThat(result).isNotNull();
    verify(eventDao).upsert(
        updated.identifier().uuid(),
        updated.ownerIdentifier().uuid(),
        updated.subject().identifier().uuid(),
        "updated",
        updated.timestamp().toOffsetDateTime());
    verify(tagsManager).syncTags(eq(tagsDao), eq(updated.identifier()), any());
  }

  @Test
  void updateThrowsWhenEventDoesNotExist() {
    when(eventDao.findByOwnerAndIdentifier(EVENT.ownerIdentifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventManager.update(EVENT))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(tagsManager);
  }

  // --- findBySubject ---

  @Test
  void findBySubjectHydratesTags() {
    PageRequest pr = PageRequest.first(10);
    Event hydrated = Event.from(EVENT).tags(List.of(new Tag("Y"))).build();
    when(eventDao.findByOwnerAndSubject(OWNER.identifier().uuid(), SUBJECT.identifier().uuid(), 11, 0))
        .thenReturn(List.of(EVENT));
    when(tagsManager.hydrateBatch(eq(List.of(EVENT)), any(), any()))
        .thenReturn(List.of(hydrated));

    Page<Event> result = eventManager.findBySubject(OWNER, SUBJECT, pr);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().tags()).containsExactly(new Tag("Y"));
    assertThat(result.hasMore()).isFalse();
  }

  // --- findByTimeRange ---

  @Test
  void findByTimeRangeHydratesTags() {
    PageRequest pr = PageRequest.first(10);
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    Event hydrated = Event.from(EVENT).tags(List.of(new Tag("Z"))).build();
    when(eventDao.findByOwnerAndTimeRange(
        OWNER.identifier().uuid(), from.toOffsetDateTime(), to.toOffsetDateTime(), 11, 0))
        .thenReturn(List.of(EVENT));
    when(tagsManager.hydrateBatch(eq(List.of(EVENT)), any(), any()))
        .thenReturn(List.of(hydrated));

    Page<Event> result = eventManager.findByTimeRange(OWNER, from, to, pr);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().tags()).containsExactly(new Tag("Z"));
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeHydratesTags() {
    PageRequest pr = PageRequest.first(10);
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    Event hydrated = Event.from(EVENT).tags(List.of(new Tag("W"))).build();
    when(eventDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(), SUBJECT.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime(), 11, 0))
        .thenReturn(List.of(EVENT));
    when(tagsManager.hydrateBatch(eq(List.of(EVENT)), any(), any()))
        .thenReturn(List.of(hydrated));

    Page<Event> result = eventManager.findBySubjectAndTimeRange(OWNER, SUBJECT, from, to, pr);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().tags()).containsExactly(new Tag("W"));
  }
}
