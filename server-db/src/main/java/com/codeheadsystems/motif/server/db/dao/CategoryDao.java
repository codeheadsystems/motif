package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Category;
import com.codeheadsystems.motif.server.db.model.Identifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(CategoryDao.CategoryRowMapper.class)
public interface CategoryDao {

  String SELECT = "SELECT c.uuid, c.owner_uuid, c.name, c.color, c.icon FROM categories c";

  @SqlUpdate("INSERT INTO categories (uuid, owner_uuid, name, color, icon) "
      + "VALUES (:uuid, :ownerUuid, :name, :color, :icon) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "name = EXCLUDED.name, color = EXCLUDED.color, icon = EXCLUDED.icon")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("name") String name,
              @Bind("color") String color,
              @Bind("icon") String icon);

  @SqlUpdate("INSERT INTO categories (uuid, owner_uuid, name, color, icon) "
      + "VALUES (:uuid, :ownerUuid, :name, :color, :icon) "
      + "ON CONFLICT (owner_uuid, name) DO NOTHING")
  int insertIfAbsent(@Bind("uuid") UUID uuid,
                     @Bind("ownerUuid") UUID ownerUuid,
                     @Bind("name") String name,
                     @Bind("color") String color,
                     @Bind("icon") String icon);

  @SqlQuery(SELECT + " WHERE c.uuid = :uuid")
  Optional<Category> findByIdentifier(@Bind("uuid") UUID uuid);

  @SqlQuery(SELECT + " WHERE c.owner_uuid = :ownerUuid AND c.uuid = :uuid")
  Optional<Category> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                              @Bind("uuid") UUID uuid);

  @SqlQuery(SELECT + " WHERE c.owner_uuid = :ownerUuid AND c.name = :name")
  Optional<Category> findByOwnerAndName(@Bind("ownerUuid") UUID ownerUuid,
                                        @Bind("name") String name);

  @SqlQuery(SELECT + " WHERE c.owner_uuid = :ownerUuid "
      + "ORDER BY c.name LIMIT :limit OFFSET :offset")
  List<Category> findByOwner(@Bind("ownerUuid") UUID ownerUuid,
                             @Bind("limit") int limit,
                             @Bind("offset") int offset);

  @SqlQuery("SELECT COUNT(*) FROM subjects WHERE category_uuid = :categoryUuid")
  long countSubjectsByCategory(@Bind("categoryUuid") UUID categoryUuid);

  @SqlUpdate("DELETE FROM categories WHERE owner_uuid = :ownerUuid AND uuid = :uuid")
  int deleteByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                 @Bind("uuid") UUID uuid);

  class CategoryRowMapper implements RowMapper<Category> {
    @Override
    public Category map(ResultSet rs, StatementContext ctx) throws SQLException {
      return Category.builder()
          .ownerIdentifier(new Identifier(rs.getObject("owner_uuid", UUID.class)))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .name(rs.getString("name"))
          .color(rs.getString("color"))
          .icon(rs.getString("icon"))
          .build();
    }
  }
}
