package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.dao.EventDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Event;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Subject;
import com.codeheadsystems.motif.server.db.model.Tag;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

@Singleton
public class EventManager {

  private final Jdbi jdbi;
  private final EventDao eventDao;
  private final TagsManager tagsManager;

  @Inject
  public EventManager(final Jdbi jdbi, final EventDao eventDao, final TagsManager tagsManager) {
    this.jdbi = jdbi;
    this.eventDao = eventDao;
    this.tagsManager = tagsManager;
  }

  public void store(Event event) {
    jdbi.useTransaction(handle -> storeInTransaction(handle, event));
  }

  public Optional<Event> get(Owner owner, Identifier identifier) {
    return eventDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid())
        .map(this::hydrateTags);
  }

  public boolean delete(Owner owner, Identifier identifier) {
    return jdbi.inTransaction(handle -> {
      EventDao txEventDao = handle.attach(EventDao.class);
      TagsDao txTagsDao = handle.attach(TagsDao.class);
      boolean deleted = txEventDao.deleteByOwnerAndIdentifier(
          owner.identifier().uuid(), identifier.uuid()) > 0;
      if (deleted) {
        tagsManager.deleteAllTags(txTagsDao, identifier);
      }
      return deleted;
    });
  }

  public void update(Event event) {
    jdbi.useTransaction(handle -> {
      EventDao txEventDao = handle.attach(EventDao.class);
      Optional<Event> existing = txEventDao.findByOwnerAndIdentifier(
          event.ownerIdentifier().uuid(), event.identifier().uuid());
      if (existing.isEmpty()) {
        throw new NotFoundException("Event not found: " + event.identifier().uuid());
      }
      storeInTransaction(handle, event);
    });
  }

  public Page<Event> findRecent(Owner owner, PageRequest pageRequest) {
    List<Event> results = eventDao.findRecentByOwner(
        owner.identifier().uuid(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  public Page<Event> findBySubject(Owner owner, Subject subject, PageRequest pageRequest) {
    List<Event> results = eventDao.findByOwnerAndSubject(
        owner.identifier().uuid(), subject.identifier().uuid(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  public Page<Event> findByTimeRange(Owner owner, Timestamp from, Timestamp to,
                                      PageRequest pageRequest) {
    List<Event> results = eventDao.findByOwnerAndTimeRange(
        owner.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  public Page<Event> findBySubjectAndTimeRange(Owner owner, Subject subject,
                                                Timestamp from, Timestamp to,
                                                PageRequest pageRequest) {
    List<Event> results = eventDao.findByOwnerSubjectAndTimeRange(
        owner.identifier().uuid(),
        subject.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  private void storeInTransaction(Handle handle, Event event) {
    EventDao txEventDao = handle.attach(EventDao.class);
    TagsDao txTagsDao = handle.attach(TagsDao.class);
    txEventDao.upsert(
        event.identifier().uuid(),
        event.ownerIdentifier().uuid(),
        event.subject().identifier().uuid(),
        event.value(),
        event.timestamp().toOffsetDateTime());
    tagsManager.syncTags(txTagsDao, event.identifier(), event.tags());
  }

  private Event hydrateTags(Event event) {
    List<Tag> tags = tagsManager.tagsFor(event.identifier());
    return Event.from(event).tags(tags).build();
  }

  private List<Event> hydrateTagsBatch(List<Event> events) {
    List<Identifier> ids = events.stream().map(Event::identifier).toList();
    Map<Identifier, List<Tag>> tagMap = tagsManager.tagsFor(ids);
    return events.stream()
        .map(e -> Event.from(e).tags(tagMap.getOrDefault(e.identifier(), List.of())).build())
        .toList();
  }
}
