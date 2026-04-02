package com.codeheadsystems.motif.server.dao;

import java.util.List;
import java.util.UUID;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface TagsDao {

  @SqlQuery("SELECT tag_value FROM tags WHERE uuid = :uuid")
  List<String> tagValuesFor(@Bind("uuid") UUID uuid);

  @SqlUpdate("INSERT INTO tags (uuid, tag_value) "
      + "VALUES (:uuid, :tag) ON CONFLICT DO NOTHING")
  void insertTag(@Bind("uuid") UUID uuid, @Bind("tag") String tag);

  @SqlUpdate("DELETE FROM tags WHERE uuid = :uuid AND tag_value = :tag")
  int deleteTag(@Bind("uuid") UUID uuid, @Bind("tag") String tag);

}
