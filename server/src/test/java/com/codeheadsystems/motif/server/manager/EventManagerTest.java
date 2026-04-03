package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.dao.EventDao;
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
  private TagsManager tagsManager;

  private EventManager eventManager;

  @BeforeEach
  void setUp() {
    eventManager = new EventManager(eventDao, tagsManager);
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
    verify(tagsManager).syncTags(EVENT.identifier(), EVENT.tags());
  }

  @Test
  void storeWithTagsSyncsTagsWithTagsManager() {
    Event withTags = Event.from(EVENT).tags(List.of(new Tag("A"), new Tag("B"))).build();

    eventManager.store(withTags);

    verify(tagsManager).syncTags(withTags.identifier(), List.of(new Tag("A"), new Tag("B")));
  }

  // --- get ---

  @Test
  void getHydratesTagsFromTagsManager() {
    when(eventDao.findByOwnerAndIdentifier(OWNER.identifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.of(EVENT));
    when(tagsManager.tagsFor(EVENT.identifier()))
        .thenReturn(List.of(new Tag("X")));

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
    verify(tagsManager).deleteAllTags(EVENT.identifier());
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
    assertThat(eventManager.update(updated)).isTrue();

    verify(eventDao).upsert(
        updated.identifier().uuid(),
        updated.ownerIdentifier().uuid(),
        updated.subject().identifier().uuid(),
        "updated",
        updated.timestamp().toOffsetDateTime());
    verify(tagsManager).syncTags(eq(updated.identifier()), any());
  }

  @Test
  void updateReturnsFalseWhenEventDoesNotExist() {
    when(eventDao.findByOwnerAndIdentifier(EVENT.ownerIdentifier().uuid(), EVENT.identifier().uuid()))
        .thenReturn(Optional.empty());

    assertThat(eventManager.update(EVENT)).isFalse();
    verifyNoInteractions(tagsManager);
  }

  // --- findBySubject ---

  @Test
  void findBySubjectHydratesTags() {
    when(eventDao.findByOwnerAndSubject(OWNER.identifier().uuid(), SUBJECT.identifier().uuid()))
        .thenReturn(List.of(EVENT));
    when(tagsManager.tagsFor(EVENT.identifier()))
        .thenReturn(List.of(new Tag("Y")));

    List<Event> result = eventManager.findBySubject(OWNER, SUBJECT);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().tags()).containsExactly(new Tag("Y"));
  }

  // --- findByTimeRange ---

  @Test
  void findByTimeRangeHydratesTags() {
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    when(eventDao.findByOwnerAndTimeRange(
        OWNER.identifier().uuid(), from.toOffsetDateTime(), to.toOffsetDateTime()))
        .thenReturn(List.of(EVENT));
    when(tagsManager.tagsFor(EVENT.identifier()))
        .thenReturn(List.of(new Tag("Z")));

    List<Event> result = eventManager.findByTimeRange(OWNER, from, to);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().tags()).containsExactly(new Tag("Z"));
  }

  // --- findBySubjectAndTimeRange ---

  @Test
  void findBySubjectAndTimeRangeHydratesTags() {
    Timestamp from = new Timestamp(Instant.parse("2026-03-28T00:00:00Z"));
    Timestamp to = new Timestamp(Instant.parse("2026-03-28T23:59:59Z"));
    when(eventDao.findByOwnerSubjectAndTimeRange(
        OWNER.identifier().uuid(), SUBJECT.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime()))
        .thenReturn(List.of(EVENT));
    when(tagsManager.tagsFor(EVENT.identifier()))
        .thenReturn(List.of(new Tag("W")));

    List<Event> result = eventManager.findBySubjectAndTimeRange(OWNER, SUBJECT, from, to);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().tags()).containsExactly(new Tag("W"));
  }
}
