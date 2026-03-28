package com.codeheadsystems.motif.server.db;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Event;
import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Subject;
import com.codeheadsystems.motif.model.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(EventDao.EventRowMapper.class)
public interface EventDao {

  String SELECT_WITH_SUBJECT = "SELECT e.identifier_uuid, e.subject_uuid, e.value, e.timestamp, "
      + "s.category AS subject_category, s.value AS subject_value, "
      + "s.identifier_uuid AS subject_identifier_uuid "
      + "FROM events e JOIN subjects s ON e.subject_uuid = s.identifier_uuid";

  // --- SQL-level methods ---

  @SqlUpdate("INSERT INTO events (identifier_uuid, subject_uuid, value, timestamp) "
      + "VALUES (:uuid, :subjectUuid, :value, :timestamp) "
      + "ON CONFLICT (identifier_uuid) DO UPDATE SET "
      + "subject_uuid = EXCLUDED.subject_uuid, "
      + "value = EXCLUDED.value, "
      + "timestamp = EXCLUDED.timestamp")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("subjectUuid") UUID subjectUuid,
              @Bind("value") String value,
              @Bind("timestamp") OffsetDateTime timestamp);

  @SqlQuery(SELECT_WITH_SUBJECT + " WHERE e.identifier_uuid = :uuid")
  Optional<Event> findByIdentifier(@Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM events WHERE identifier_uuid = :uuid")
  int deleteByIdentifier(@Bind("uuid") UUID uuid);

  @SqlQuery(SELECT_WITH_SUBJECT + " WHERE e.subject_uuid = :subjectUuid ORDER BY e.timestamp")
  List<Event> findBySubjectUuid(@Bind("subjectUuid") UUID subjectUuid);

  @SqlQuery(SELECT_WITH_SUBJECT + " WHERE e.timestamp >= :from AND e.timestamp <= :to "
      + "ORDER BY e.timestamp")
  List<Event> findByTimeRange(@Bind("from") OffsetDateTime from, @Bind("to") OffsetDateTime to);

  @SqlQuery(SELECT_WITH_SUBJECT + " WHERE e.subject_uuid = :subjectUuid "
      + "AND e.timestamp >= :from AND e.timestamp <= :to "
      + "ORDER BY e.timestamp")
  List<Event> findBySubjectAndTimeRange(@Bind("subjectUuid") UUID subjectUuid,
                                        @Bind("from") OffsetDateTime from,
                                        @Bind("to") OffsetDateTime to);

  // --- Domain-level methods ---

  default void store(Event event) {
    upsert(
        event.identifier().uuid(),
        event.subject().identifier().uuid(),
        event.value(),
        event.timestamp().timestamp().atOffset(ZoneOffset.UTC));
  }

  default Optional<Event> get(Identifier identifier) {
    return findByIdentifier(identifier.uuid());
  }

  default boolean delete(Identifier identifier) {
    return deleteByIdentifier(identifier.uuid()) > 0;
  }

  default List<Event> findBySubject(Subject subject) {
    return findBySubjectUuid(subject.identifier().uuid());
  }

  default List<Event> findByTimeRange(Timestamp from, Timestamp to) {
    return findByTimeRange(
        from.timestamp().atOffset(ZoneOffset.UTC),
        to.timestamp().atOffset(ZoneOffset.UTC));
  }

  default List<Event> findBySubjectAndTimeRange(Subject subject, Timestamp from, Timestamp to) {
    return findBySubjectAndTimeRange(
        subject.identifier().uuid(),
        from.timestamp().atOffset(ZoneOffset.UTC),
        to.timestamp().atOffset(ZoneOffset.UTC));
  }

  // --- Row mapper ---

  class EventRowMapper implements RowMapper<Event> {
    @Override
    public Event map(ResultSet rs, StatementContext ctx) throws SQLException {
      Subject subject = Subject.builder(
              new Category(rs.getString("subject_category")),
              rs.getString("subject_value"))
          .identifier(new Identifier(rs.getObject("subject_identifier_uuid", UUID.class)))
          .build();
      Identifier identifier = new Identifier(
          rs.getObject("identifier_uuid", UUID.class));
      Timestamp timestamp = new Timestamp(
          rs.getTimestamp("timestamp").toInstant());
      return Event.builder(subject, rs.getString("value"))
          .identifier(identifier)
          .timestamp(timestamp)
          .build();
    }
  }

}
