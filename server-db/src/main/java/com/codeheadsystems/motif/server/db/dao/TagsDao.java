package com.codeheadsystems.motif.server.db.dao;

import java.util.List;
import java.util.UUID;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface TagsDao {

  @SqlQuery("SELECT tag_value FROM tags WHERE uuid = :uuid")
  List<String> tagValuesFor(@Bind("uuid") UUID uuid);

  @SqlQuery("SELECT uuid, tag_value FROM tags WHERE uuid = ANY(:uuids)")
  @RegisterRowMapper(TagEntry.TagEntryRowMapper.class)
  List<TagEntry> tagValuesForBatch(@Bind("uuids") UUID[] uuids);

  @SqlUpdate("INSERT INTO tags (uuid, tag_value) "
      + "VALUES (:uuid, :tag) ON CONFLICT DO NOTHING")
  void insertTag(@Bind("uuid") UUID uuid, @Bind("tag") String tag);

  @SqlUpdate("DELETE FROM tags WHERE uuid = :uuid AND tag_value = :tag")
  int deleteTag(@Bind("uuid") UUID uuid, @Bind("tag") String tag);

  @SqlUpdate("DELETE FROM tags WHERE uuid = :uuid")
  int deleteAllTags(@Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM tags WHERE uuid IN (SELECT uuid FROM events WHERE owner_uuid = :ownerUuid)")
  int deleteTagsForOwnerEvents(@Bind("ownerUuid") UUID ownerUuid);

  @SqlUpdate("DELETE FROM tags WHERE uuid IN (SELECT uuid FROM notes WHERE owner_uuid = :ownerUuid)")
  int deleteTagsForOwnerNotes(@Bind("ownerUuid") UUID ownerUuid);

}
