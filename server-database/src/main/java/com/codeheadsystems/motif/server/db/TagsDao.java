package com.codeheadsystems.motif.server.db;

import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Tag;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface TagsDao {

  @SqlQuery("SELECT tag_value FROM tags WHERE identifier_class = :clazz AND identifier_uuid = :uuid")
  List<String> tagValuesFor(@Bind("clazz") String clazz, @Bind("uuid") UUID uuid);

  @SqlUpdate("INSERT INTO tags (identifier_class, identifier_uuid, tag_value) "
      + "VALUES (:clazz, :uuid, :tag) ON CONFLICT DO NOTHING")
  void insertTag(@Bind("clazz") String clazz, @Bind("uuid") UUID uuid, @Bind("tag") String tag);

  @SqlUpdate("DELETE FROM tags WHERE identifier_class = :clazz "
      + "AND identifier_uuid = :uuid AND tag_value = :tag")
  int deleteTag(@Bind("clazz") String clazz, @Bind("uuid") UUID uuid, @Bind("tag") String tag);

  default List<Tag> tagsFor(Identifier identifier) {
    return tagValuesFor(identifier.clazz().getSimpleName(), identifier.uuid())
        .stream()
        .map(Tag::new)
        .toList();
  }

  default boolean addTags(Identifier identifier, List<Tag> tags) {
    String clazz = identifier.clazz().getSimpleName();
    UUID uuid = identifier.uuid();
    for (Tag tag : tags) {
      insertTag(clazz, uuid, tag.value());
    }
    return true;
  }

  default boolean removeTags(Identifier identifier, List<Tag> tags) {
    String clazz = identifier.clazz().getSimpleName();
    UUID uuid = identifier.uuid();
    int removed = 0;
    for (Tag tag : tags) {
      removed += deleteTag(clazz, uuid, tag.value());
    }
    return removed > 0;
  }

}
