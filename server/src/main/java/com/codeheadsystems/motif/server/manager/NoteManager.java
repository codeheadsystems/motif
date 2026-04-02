package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.NoteDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Note;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NoteManager {

  private final NoteDao noteDao;
  private final TagsDao tagsDao;

  @Inject
  public NoteManager(final NoteDao noteDao, final TagsDao tagsDao) {
    this.noteDao = noteDao;
    this.tagsDao = tagsDao;
  }

  public void store(Note note) {
    noteDao.upsert(
        note.identifier().uuid(),
        note.ownerIdentifier().uuid(),
        note.subjectIdentifier().uuid(),
        note.eventIdentifier() != null ? note.eventIdentifier().uuid() : null,
        note.value(),
        note.timestamp().toOffsetDateTime());
    for (var tag : note.tags()) {
      tagsDao.insertTag(note.identifier().uuid(), tag.value());
    }
  }

  public Optional<Note> get(Owner owner, Identifier identifier) {
    return noteDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  public boolean delete(Owner owner, Identifier identifier) {
    return noteDao.deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
  }

  public boolean update(Note note) {
    Optional<Note> existing = noteDao.findByOwnerAndIdentifier(
        note.ownerIdentifier().uuid(), note.identifier().uuid());
    if (existing.isEmpty()) {
      return false;
    }
    store(note);
    return true;
  }

  public List<Note> findBySubject(Owner owner, Subject subject) {
    return noteDao.findByOwnerAndSubject(owner.identifier().uuid(), subject.identifier().uuid());
  }

  public List<Note> findBySubjectAndTimeRange(Owner owner, Subject subject,
                                               Timestamp from, Timestamp to) {
    return noteDao.findByOwnerSubjectAndTimeRange(
        owner.identifier().uuid(),
        subject.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime());
  }

  public List<Note> findByEvent(Owner owner, Identifier eventIdentifier) {
    return noteDao.findByOwnerAndEvent(owner.identifier().uuid(), eventIdentifier.uuid());
  }

  public List<Note> findByEventAndTimeRange(Owner owner, Identifier eventIdentifier,
                                             Timestamp from, Timestamp to) {
    return noteDao.findByOwnerEventAndTimeRange(
        owner.identifier().uuid(),
        eventIdentifier.uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime());
  }
}
