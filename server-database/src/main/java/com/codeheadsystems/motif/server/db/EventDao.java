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

  // --- SQL-level methods ---

  @SqlUpdate("INSERT INTO events (identifier_class, identifier_uuid, subject_category, subject_value, value, timestamp) "
      + "VALUES (:clazz, :uuid, :category, :subject, :value, :timestamp) "
      + "ON CONFLICT (identifier_class, identifier_uuid) DO UPDATE SET "
      + "subject_category = EXCLUDED.subject_category, "
      + "subject_value = EXCLUDED.subject_value, "
      + "value = EXCLUDED.value, "
      + "timestamp = EXCLUDED.timestamp")
  void upsert(@Bind("clazz") String clazz,
              @Bind("uuid") UUID uuid,
              @Bind("category") String category,
              @Bind("subject") String subject,
              @Bind("value") String value,
              @Bind("timestamp") OffsetDateTime timestamp);

  @SqlQuery("SELECT * FROM events WHERE identifier_class = :clazz AND identifier_uuid = :uuid")
  Optional<Event> findByIdentifier(@Bind("clazz") String clazz, @Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM events WHERE identifier_class = :clazz AND identifier_uuid = :uuid")
  int deleteByIdentifier(@Bind("clazz") String clazz, @Bind("uuid") UUID uuid);

  @SqlQuery("SELECT * FROM events WHERE subject_category = :category AND subject_value = :subject "
      + "ORDER BY timestamp")
  List<Event> findBySubject(@Bind("category") String category, @Bind("subject") String subject);

  @SqlQuery("SELECT * FROM events WHERE timestamp >= :from AND timestamp <= :to "
      + "ORDER BY timestamp")
  List<Event> findByTimeRange(@Bind("from") OffsetDateTime from, @Bind("to") OffsetDateTime to);

  @SqlQuery("SELECT * FROM events WHERE subject_category = :category AND subject_value = :subject "
      + "AND timestamp >= :from AND timestamp <= :to "
      + "ORDER BY timestamp")
  List<Event> findBySubjectAndTimeRange(@Bind("category") String category,
                                        @Bind("subject") String subject,
                                        @Bind("from") OffsetDateTime from,
                                        @Bind("to") OffsetDateTime to);

  // --- Domain-level methods ---

  default void store(Event event) {
    upsert(
        event.identifier().clazz().getSimpleName(),
        event.identifier().uuid(),
        event.subject().category().value(),
        event.subject().value(),
        event.value(),
        event.timestamp().timestamp().atOffset(ZoneOffset.UTC));
  }

  default Optional<Event> get(Identifier identifier) {
    return findByIdentifier(identifier.clazz().getSimpleName(), identifier.uuid());
  }

  default boolean delete(Identifier identifier) {
    return deleteByIdentifier(identifier.clazz().getSimpleName(), identifier.uuid()) > 0;
  }

  default List<Event> findBySubject(Subject subject) {
    return findBySubject(subject.category().value(), subject.value());
  }

  default List<Event> findByTimeRange(Timestamp from, Timestamp to) {
    return findByTimeRange(
        from.timestamp().atOffset(ZoneOffset.UTC),
        to.timestamp().atOffset(ZoneOffset.UTC));
  }

  default List<Event> findBySubjectAndTimeRange(Subject subject, Timestamp from, Timestamp to) {
    return findBySubjectAndTimeRange(
        subject.category().value(), subject.value(),
        from.timestamp().atOffset(ZoneOffset.UTC),
        to.timestamp().atOffset(ZoneOffset.UTC));
  }

  // --- Row mapper ---

  class EventRowMapper implements RowMapper<Event> {
    @Override
    public Event map(ResultSet rs, StatementContext ctx) throws SQLException {
      Subject subject = new Subject(
          new Category(rs.getString("subject_category")),
          rs.getString("subject_value"));
      Identifier identifier = new Identifier(
          Event.class,
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
