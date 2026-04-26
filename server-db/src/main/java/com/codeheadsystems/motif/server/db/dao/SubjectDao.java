package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Subject;
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

@RegisterRowMapper(SubjectDao.SubjectRowMapper.class)
public interface SubjectDao {

  String SELECT = "SELECT s.uuid, s.owner_uuid, s.category_uuid, s.project_uuid, s.value FROM subjects s";

  // ::uuid casts on the bind site coerce null bindings (which JDBI sends as VARCHAR by
  // default) into the column type. Without this Postgres rejects the INSERT when projectUuid
  // is null. Non-null UUIDs are unaffected by the cast.
  @SqlUpdate("INSERT INTO subjects (uuid, owner_uuid, category_uuid, project_uuid, value) "
      + "VALUES (:uuid, :ownerUuid, :categoryUuid, CAST(:projectUuid AS uuid), :value) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "category_uuid = EXCLUDED.category_uuid, "
      + "project_uuid = EXCLUDED.project_uuid, "
      + "value = EXCLUDED.value")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("categoryUuid") UUID categoryUuid,
              @Bind("projectUuid") UUID projectUuid,
              @Bind("value") String value);

  /** Backwards-compatible 4-arg overload that leaves project_uuid null. */
  default void upsert(UUID uuid, UUID ownerUuid, UUID categoryUuid, String value) {
    upsert(uuid, ownerUuid, categoryUuid, null, value);
  }

  @SqlQuery(SELECT + " WHERE s.uuid = :uuid")
  Optional<Subject> findByIdentifier(@Bind("uuid") UUID uuid);

  @SqlQuery(SELECT + " WHERE s.owner_uuid = :ownerUuid AND s.uuid = :uuid")
  Optional<Subject> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                              @Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM subjects WHERE owner_uuid = :ownerUuid AND uuid = :uuid")
  int deleteByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid, @Bind("uuid") UUID uuid);

  @SqlQuery(SELECT + " WHERE s.owner_uuid = :ownerUuid AND s.category_uuid = :categoryUuid "
      + "ORDER BY s.value LIMIT :limit OFFSET :offset")
  List<Subject> findByOwnerAndCategory(@Bind("ownerUuid") UUID ownerUuid,
                                        @Bind("categoryUuid") UUID categoryUuid,
                                        @Bind("limit") int limit,
                                        @Bind("offset") int offset);

  @SqlQuery(SELECT + " WHERE s.owner_uuid = :ownerUuid AND s.project_uuid = :projectUuid "
      + "ORDER BY s.value LIMIT :limit OFFSET :offset")
  List<Subject> findByOwnerAndProject(@Bind("ownerUuid") UUID ownerUuid,
                                       @Bind("projectUuid") UUID projectUuid,
                                       @Bind("limit") int limit,
                                       @Bind("offset") int offset);

  @SqlQuery(SELECT + " WHERE s.owner_uuid = :ownerUuid "
      + "AND s.category_uuid = :categoryUuid AND s.value = :value")
  Optional<Subject> findByOwnerCategoryAndValue(@Bind("ownerUuid") UUID ownerUuid,
                                                 @Bind("categoryUuid") UUID categoryUuid,
                                                 @Bind("value") String value);

  @SqlQuery(SELECT + " WHERE s.value = :value")
  List<Subject> findByValue(@Bind("value") String value);

  @SqlUpdate("DELETE FROM subjects WHERE owner_uuid = :ownerUuid")
  int deleteByOwner(@Bind("ownerUuid") UUID ownerUuid);

  class SubjectRowMapper implements RowMapper<Subject> {
    @Override
    public Subject map(ResultSet rs, StatementContext ctx) throws SQLException {
      UUID projectUuid = rs.getObject("project_uuid", UUID.class);
      return Subject.builder()
          .ownerIdentifier(new Identifier(rs.getObject("owner_uuid", UUID.class)))
          .categoryIdentifier(new Identifier(rs.getObject("category_uuid", UUID.class)))
          .projectIdentifier(projectUuid == null ? null : new Identifier(projectUuid))
          .value(rs.getString("value"))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .build();
    }
  }

}
