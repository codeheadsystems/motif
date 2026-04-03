package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Tag;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TagsManager {

  private final TagsDao tagsDao;

  @Inject
  public TagsManager(final TagsDao tagsDao) {
    this.tagsDao = tagsDao;
  }

  public List<Tag> tagsFor(Identifier identifier) {
    return tagsDao.tagValuesFor(identifier.uuid())
        .stream()
        .map(Tag::new)
        .toList();
  }

  public void addTags(Identifier identifier, List<Tag> tags) {
    UUID uuid = identifier.uuid();
    for (Tag tag : tags) {
      tagsDao.insertTag(uuid, tag.value());
    }
  }

  public boolean removeTags(Identifier identifier, List<Tag> tags) {
    UUID uuid = identifier.uuid();
    int removed = 0;
    for (Tag tag : tags) {
      removed += tagsDao.deleteTag(uuid, tag.value());
    }
    return removed > 0;
  }

  public boolean deleteAllTags(Identifier identifier) {
    return deleteAllTags(tagsDao, identifier);
  }

  boolean deleteAllTags(TagsDao txTagsDao, Identifier identifier) {
    return txTagsDao.deleteAllTags(identifier.uuid()) > 0;
  }

  public void syncTags(Identifier identifier, List<Tag> desiredTags) {
    syncTags(tagsDao, identifier, desiredTags);
  }

  void syncTags(TagsDao txTagsDao, Identifier identifier, List<Tag> desiredTags) {
    Set<String> desired = new HashSet<>();
    for (Tag tag : desiredTags) {
      desired.add(tag.value());
    }
    Set<String> existing = new HashSet<>(txTagsDao.tagValuesFor(identifier.uuid()));
    for (String tag : existing) {
      if (!desired.contains(tag)) {
        txTagsDao.deleteTag(identifier.uuid(), tag);
      }
    }
    for (String tag : desired) {
      if (!existing.contains(tag)) {
        txTagsDao.insertTag(identifier.uuid(), tag);
      }
    }
  }
}
