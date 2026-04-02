package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventManager {

  private final EventDao eventDao;
  private final TagsDao tagsDao;

  @Inject
  public EventManager(final EventDao eventDao, final TagsDao tagsDao) {
    this.eventDao = eventDao;
    this.tagsDao = tagsDao;
  }

  public void store(Event event) {
    eventDao.upsert(
        event.identifier().uuid(),
        event.ownerIdentifier().uuid(),
        event.subject().identifier().uuid(),
        event.value(),
        event.timestamp().toOffsetDateTime());
    tagsDao.addTags(event.identifier(), event.tags());
  }

  public Optional<Event> get(Owner owner, Identifier identifier) {
    return eventDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  public boolean delete(Owner owner, Identifier identifier) {
    return eventDao.deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
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
    return eventDao.findByOwnerAndSubject(owner.identifier().uuid(), subject.identifier().uuid());
  }

  public List<Event> findByTimeRange(Owner owner, Timestamp from, Timestamp to) {
    return eventDao.findByOwnerAndTimeRange(
        owner.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime());
  }

  public List<Event> findBySubjectAndTimeRange(Owner owner, Subject subject,
                                                Timestamp from, Timestamp to) {
    return eventDao.findByOwnerSubjectAndTimeRange(
        owner.identifier().uuid(),
        subject.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime());
  }
}
