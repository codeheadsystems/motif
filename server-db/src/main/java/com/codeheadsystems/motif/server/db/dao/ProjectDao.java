package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Project;
import com.codeheadsystems.motif.server.db.model.ProjectStatus;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(ProjectDao.ProjectRowMapper.class)
public interface ProjectDao {

  String SELECT = "SELECT p.uuid, p.owner_uuid, p.name, p.description, p.status, "
      + "p.created_at, p.updated_at FROM projects p";

  @SqlUpdate("INSERT INTO projects (uuid, owner_uuid, name, description, status, created_at, updated_at) "
      + "VALUES (:uuid, :ownerUuid, :name, :description, :status, :createdAt, :updatedAt) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "name = EXCLUDED.name, "
      + "description = EXCLUDED.description, "
      + "status = EXCLUDED.status, "
      + "updated_at = EXCLUDED.updated_at")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("name") String name,
              @Bind("description") String description,
              @Bind("status") String status,
              @Bind("createdAt") OffsetDateTime createdAt,
              @Bind("updatedAt") OffsetDateTime updatedAt);

  @SqlQuery(SELECT + " WHERE p.owner_uuid = :ownerUuid AND p.uuid = :uuid")
  Optional<Project> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                             @Bind("uuid") UUID uuid);

  @SqlQuery(SELECT + " WHERE p.owner_uuid = :ownerUuid AND p.name = :name")
  Optional<Project> findByOwnerAndName(@Bind("ownerUuid") UUID ownerUuid,
                                       @Bind("name") String name);

  @SqlQuery(SELECT + " WHERE p.owner_uuid = :ownerUuid "
      + "ORDER BY p.status, p.name LIMIT :limit OFFSET :offset")
  List<Project> findByOwner(@Bind("ownerUuid") UUID ownerUuid,
                            @Bind("limit") int limit,
                            @Bind("offset") int offset);

  @SqlUpdate("DELETE FROM projects WHERE owner_uuid = :ownerUuid AND uuid = :uuid")
  int deleteByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                 @Bind("uuid") UUID uuid);

  class ProjectRowMapper implements RowMapper<Project> {
    @Override
    public Project map(ResultSet rs, StatementContext ctx) throws SQLException {
      return Project.builder()
          .ownerIdentifier(new Identifier(rs.getObject("owner_uuid", UUID.class)))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .name(rs.getString("name"))
          .description(rs.getString("description"))
          .status(ProjectStatus.valueOf(rs.getString("status")))
          .createdAt(new Timestamp(rs.getTimestamp("created_at").toInstant()))
          .updatedAt(new Timestamp(rs.getTimestamp("updated_at").toInstant()))
          .build();
    }
  }
}
