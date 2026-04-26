package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import com.codeheadsystems.motif.server.db.model.Workflow;
import com.codeheadsystems.motif.server.db.model.WorkflowStep;
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

/**
 * Header-only DAO for the workflows table. Steps live in {@link WorkflowStepDao}; the
 * manager assembles the two for callers that want full Workflows.
 *
 * <p>Returned {@link Workflow} instances always have an empty steps list — call
 * {@link WorkflowStepDao#findByWorkflow} to populate.
 */
@RegisterRowMapper(WorkflowDao.WorkflowHeaderRowMapper.class)
public interface WorkflowDao {

  String SELECT = "SELECT w.uuid, w.owner_uuid, w.name, w.description, w.created_at, w.updated_at "
      + "FROM workflows w";

  @SqlUpdate("INSERT INTO workflows (uuid, owner_uuid, name, description, created_at, updated_at) "
      + "VALUES (:uuid, :ownerUuid, :name, :description, :createdAt, :updatedAt) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "name = EXCLUDED.name, "
      + "description = EXCLUDED.description, "
      + "updated_at = EXCLUDED.updated_at")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("name") String name,
              @Bind("description") String description,
              @Bind("createdAt") OffsetDateTime createdAt,
              @Bind("updatedAt") OffsetDateTime updatedAt);

  @SqlQuery(SELECT + " WHERE w.owner_uuid = :ownerUuid AND w.uuid = :uuid")
  Optional<Workflow> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                              @Bind("uuid") UUID uuid);

  @SqlQuery(SELECT + " WHERE w.owner_uuid = :ownerUuid "
      + "ORDER BY w.name LIMIT :limit OFFSET :offset")
  List<Workflow> findByOwner(@Bind("ownerUuid") UUID ownerUuid,
                             @Bind("limit") int limit,
                             @Bind("offset") int offset);

  @SqlUpdate("DELETE FROM workflows WHERE owner_uuid = :ownerUuid AND uuid = :uuid")
  int deleteByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                 @Bind("uuid") UUID uuid);

  class WorkflowHeaderRowMapper implements RowMapper<Workflow> {
    @Override
    public Workflow map(ResultSet rs, StatementContext ctx) throws SQLException {
      return Workflow.builder()
          .ownerIdentifier(new Identifier(rs.getObject("owner_uuid", UUID.class)))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .name(rs.getString("name"))
          .description(rs.getString("description"))
          .steps(List.<WorkflowStep>of())
          .createdAt(new Timestamp(rs.getTimestamp("created_at").toInstant()))
          .updatedAt(new Timestamp(rs.getTimestamp("updated_at").toInstant()))
          .build();
    }
  }
}
