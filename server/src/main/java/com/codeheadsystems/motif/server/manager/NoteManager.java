package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.dao.NoteDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Note;
import com.codeheadsystems.motif.server.model.Owner;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Tag;
import com.codeheadsystems.motif.server.model.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

@Singleton
public class NoteManager {

  private final Jdbi jdbi;
  private final NoteDao noteDao;
  private final TagsManager tagsManager;

  @Inject
  public NoteManager(final Jdbi jdbi, final NoteDao noteDao, final TagsManager tagsManager) {
    this.jdbi = jdbi;
    this.noteDao = noteDao;
    this.tagsManager = tagsManager;
  }

  public void store(Note note) {
    jdbi.useTransaction(handle -> storeInTransaction(handle, note));
  }

  public Optional<Note> get(Owner owner, Identifier identifier) {
    return noteDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid())
        .map(this::hydrateTags);
  }

  public boolean delete(Owner owner, Identifier identifier) {
    return jdbi.inTransaction(handle -> {
      NoteDao txNoteDao = handle.attach(NoteDao.class);
      TagsDao txTagsDao = handle.attach(TagsDao.class);
      boolean deleted = txNoteDao.deleteByOwnerAndIdentifier(
          owner.identifier().uuid(), identifier.uuid()) > 0;
      if (deleted) {
        tagsManager.deleteAllTags(txTagsDao, identifier);
      }
      return deleted;
    });
  }

  public void update(Note note) {
    jdbi.useTransaction(handle -> {
      NoteDao txNoteDao = handle.attach(NoteDao.class);
      Optional<Note> existing = txNoteDao.findByOwnerAndIdentifier(
          note.ownerIdentifier().uuid(), note.identifier().uuid());
      if (existing.isEmpty()) {
        throw new NotFoundException("Note not found: " + note.identifier().uuid());
      }
      storeInTransaction(handle, note);
    });
  }

  public Page<Note> findBySubject(Owner owner, Subject subject, PageRequest pageRequest) {
    List<Note> results = noteDao.findByOwnerAndSubject(
        owner.identifier().uuid(), subject.identifier().uuid(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  public Page<Note> findBySubjectAndTimeRange(Owner owner, Subject subject,
                                               Timestamp from, Timestamp to,
                                               PageRequest pageRequest) {
    List<Note> results = noteDao.findByOwnerSubjectAndTimeRange(
        owner.identifier().uuid(),
        subject.identifier().uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  public Page<Note> findByEvent(Owner owner, Identifier eventIdentifier,
                                 PageRequest pageRequest) {
    List<Note> results = noteDao.findByOwnerAndEvent(
        owner.identifier().uuid(), eventIdentifier.uuid(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  public Page<Note> findByEventAndTimeRange(Owner owner, Identifier eventIdentifier,
                                             Timestamp from, Timestamp to,
                                             PageRequest pageRequest) {
    List<Note> results = noteDao.findByOwnerEventAndTimeRange(
        owner.identifier().uuid(),
        eventIdentifier.uuid(),
        from.toOffsetDateTime(),
        to.toOffsetDateTime(),
        pageRequest.pageSize() + 1, pageRequest.offset());
    return Page.of(hydrateTagsBatch(results), pageRequest);
  }

  private void storeInTransaction(Handle handle, Note note) {
    NoteDao txNoteDao = handle.attach(NoteDao.class);
    TagsDao txTagsDao = handle.attach(TagsDao.class);
    txNoteDao.upsert(
        note.identifier().uuid(),
        note.ownerIdentifier().uuid(),
        note.subjectIdentifier().uuid(),
        note.eventIdentifier() != null ? note.eventIdentifier().uuid() : null,
        note.value(),
        note.timestamp().toOffsetDateTime());
    tagsManager.syncTags(txTagsDao, note.identifier(), note.tags());
  }

  private Note hydrateTags(Note note) {
    List<Tag> tags = tagsManager.tagsFor(note.identifier());
    return Note.from(note).tags(tags).build();
  }

  private List<Note> hydrateTagsBatch(List<Note> notes) {
    List<Identifier> ids = notes.stream().map(Note::identifier).toList();
    Map<Identifier, List<Tag>> tagMap = tagsManager.tagsFor(ids);
    return notes.stream()
        .map(n -> Note.from(n).tags(tagMap.getOrDefault(n.identifier(), List.of())).build())
        .toList();
  }
}
