package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
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

  public boolean update(Event event) {
    return jdbi.inTransaction(handle -> {
      EventDao txEventDao = handle.attach(EventDao.class);
      Optional<Event> existing = txEventDao.findByOwnerAndIdentifier(
          event.ownerIdentifier().uuid(), event.identifier().uuid());
      if (existing.isEmpty()) {
        return false;
      }
      storeInTransaction(handle, event);
      return true;
    });
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
}
