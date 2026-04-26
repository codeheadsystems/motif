package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.WorkflowStep;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(WorkflowStepDao.WorkflowStepRowMapper.class)
public interface WorkflowStepDao {

  // CAST(:expectedDurationSeconds AS BIGINT) and CAST(:notes AS varchar) coerce JDBI's
  // null-as-VARCHAR default into the column types when the values are absent.
  @SqlUpdate("INSERT INTO workflow_steps (uuid, workflow_uuid, position, name, "
      + "expected_duration_seconds, notes) "
      + "VALUES (:uuid, :workflowUuid, :position, :name, "
      + "CAST(:expectedDurationSeconds AS BIGINT), :notes)")
  void insert(@Bind("uuid") UUID uuid,
              @Bind("workflowUuid") UUID workflowUuid,
              @Bind("position") int position,
              @Bind("name") String name,
              @Bind("expectedDurationSeconds") Long expectedDurationSeconds,
              @Bind("notes") String notes);

  @SqlQuery("SELECT uuid, position, name, expected_duration_seconds, notes "
      + "FROM workflow_steps WHERE workflow_uuid = :workflowUuid ORDER BY position")
  List<WorkflowStep> findByWorkflow(@Bind("workflowUuid") UUID workflowUuid);

  @SqlUpdate("DELETE FROM workflow_steps WHERE workflow_uuid = :workflowUuid")
  int deleteByWorkflow(@Bind("workflowUuid") UUID workflowUuid);

  class WorkflowStepRowMapper implements RowMapper<WorkflowStep> {
    @Override
    public WorkflowStep map(ResultSet rs, StatementContext ctx) throws SQLException {
      long duration = rs.getLong("expected_duration_seconds");
      Long expectedDuration = rs.wasNull() ? null : duration;
      return new WorkflowStep(
          new Identifier(rs.getObject("uuid", UUID.class)),
          rs.getInt("position"),
          rs.getString("name"),
          expectedDuration,
          rs.getString("notes"));
    }
  }
}
