package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Tag;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  public Map<Identifier, List<Tag>> tagsFor(List<Identifier> identifiers) {
    if (identifiers.isEmpty()) {
      return Map.of();
    }
    UUID[] uuids = identifiers.stream().map(Identifier::uuid).toArray(UUID[]::new);
    return tagsDao.tagValuesForBatch(uuids).stream()
        .collect(Collectors.groupingBy(
            e -> new Identifier(e.uuid()),
            Collectors.mapping(e -> new Tag(e.tagValue()), Collectors.toList())));
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

  public <T> T hydrate(T entity, Function<T, Identifier> idExtractor,
                       BiFunction<T, List<Tag>, T> withTags) {
    List<Tag> tags = tagsFor(idExtractor.apply(entity));
    return withTags.apply(entity, tags);
  }

  public <T> List<T> hydrateBatch(List<T> entities, Function<T, Identifier> idExtractor,
                                   BiFunction<T, List<Tag>, T> withTags) {
    List<Identifier> ids = entities.stream().map(idExtractor).toList();
    Map<Identifier, List<Tag>> tagMap = tagsFor(ids);
    return entities.stream()
        .map(e -> withTags.apply(e, tagMap.getOrDefault(idExtractor.apply(e), List.of())))
        .toList();
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
