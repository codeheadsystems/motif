package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.NoteDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Note;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Tag;
import com.codeheadsystems.motif.server.model.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NoteManager {

  private final NoteDao noteDao;
  private final TagsManager tagsManager;

  @Inject
  public NoteManager(final NoteDao noteDao, final TagsManager tagsManager) {
    this.noteDao = noteDao;
    this.tagsManager = tagsManager;
  }

  public void store(Note note) {
    noteDao.upsert(
        note.identifier().uuid(),
        note.ownerIdentifier().uuid(),
        note.subjectIdentifier().uuid(),
        note.eventIdentifier() != null ? note.eventIdentifier().uuid() : null,
        note.value(),
        note.timestamp().toOffsetDateTime());
    tagsManager.syncTags(note.identifier(), note.tags());
  }

  public Optional<Note> get(Owner owner, Identifier identifier) {
    return noteDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid())
        .map(this::hydrateTags);
  }

  public boolean delete(Owner owner, Identifier identifier) {
    boolean deleted = noteDao.deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
    if (deleted) {
      tagsManager.deleteAllTags(identifier);
    }
    return deleted;
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
    return noteDao.findByOwnerAndSubject(owner.identifier().uuid(), subject.identifier().uuid())
        .stream().map(this::hydrateTags).toList();
  }

  public List<Note> findBySubjectAndTimeRange(Owner owner, Subject subject,
                                               Timestamp from, Timestamp to) {
    return noteDao.findByOwnerSubjectAndTimeRange(
        owner.identifier().uuid(),
        subject.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime())
        .stream().map(this::hydrateTags).toList();
  }

  public List<Note> findByEvent(Owner owner, Identifier eventIdentifier) {
    return noteDao.findByOwnerAndEvent(owner.identifier().uuid(), eventIdentifier.uuid())
        .stream().map(this::hydrateTags).toList();
  }

  public List<Note> findByEventAndTimeRange(Owner owner, Identifier eventIdentifier,
                                             Timestamp from, Timestamp to) {
    return noteDao.findByOwnerEventAndTimeRange(
        owner.identifier().uuid(),
        eventIdentifier.uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime())
        .stream().map(this::hydrateTags).toList();
  }

  private Note hydrateTags(Note note) {
    List<Tag> tags = tagsManager.tagsFor(note.identifier());
    return Note.from(note).tags(tags).build();
  }
}
