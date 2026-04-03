package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Tag;
import com.codeheadsystems.motif.server.model.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventManager {

  private final EventDao eventDao;
  private final TagsManager tagsManager;

  @Inject
  public EventManager(final EventDao eventDao, final TagsManager tagsManager) {
    this.eventDao = eventDao;
    this.tagsManager = tagsManager;
  }

  public void store(Event event) {
    eventDao.upsert(
        event.identifier().uuid(),
        event.ownerIdentifier().uuid(),
        event.subject().identifier().uuid(),
        event.value(),
        event.timestamp().toOffsetDateTime());
    tagsManager.syncTags(event.identifier(), event.tags());
  }

  public Optional<Event> get(Owner owner, Identifier identifier) {
    return eventDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid())
        .map(this::hydrateTags);
  }

  public boolean delete(Owner owner, Identifier identifier) {
    boolean deleted = eventDao.deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
    if (deleted) {
      tagsManager.deleteAllTags(identifier);
    }
    return deleted;
  }

  public boolean update(Event event) {
    Optional<Event> existing = eventDao.findByOwnerAndIdentifier(
        event.ownerIdentifier().uuid(), event.identifier().uuid());
    if (existing.isEmpty()) {
      return false;
    }
    store(event);
    return true;
  }

  public List<Event> findBySubject(Owner owner, Subject subject) {
    return eventDao.findByOwnerAndSubject(owner.identifier().uuid(), subject.identifier().uuid())
        .stream().map(this::hydrateTags).toList();
  }

  public List<Event> findByTimeRange(Owner owner, Timestamp from, Timestamp to) {
    return eventDao.findByOwnerAndTimeRange(
        owner.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime())
        .stream().map(this::hydrateTags).toList();
  }

  public List<Event> findBySubjectAndTimeRange(Owner owner, Subject subject,
                                                Timestamp from, Timestamp to) {
    return eventDao.findByOwnerSubjectAndTimeRange(
        owner.identifier().uuid(),
        subject.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime())
        .stream().map(this::hydrateTags).toList();
  }

  private Event hydrateTags(Event event) {
    List<Tag> tags = tagsManager.tagsFor(event.identifier());
    return Event.from(event).tags(tags).build();
  }
}
