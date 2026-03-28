package com.codeheadsystems.motif.server.db;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Event;
import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Owner;
import com.codeheadsystems.motif.model.Subject;
import com.codeheadsystems.motif.model.Timestamp;
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

@RegisterRowMapper(EventDao.EventRowMapper.class)
public interface EventDao {

  String SELECT_WITH_JOINS = "SELECT e.uuid, e.owner_uuid, e.subject_uuid, "
      + "e.value, e.timestamp, "
      + "s.category AS subject_category, s.value AS subject_value, "
      + "s.uuid AS subject_uuid, "
      + "o.value AS owner_value "
      + "FROM events e "
      + "JOIN subjects s ON e.subject_uuid = s.uuid "
      + "JOIN owners o ON e.owner_uuid = o.uuid";

  @SqlUpdate("INSERT INTO events (uuid, owner_uuid, subject_uuid, value, timestamp) "
      + "VALUES (:uuid, :ownerUuid, :subjectUuid, :value, :timestamp) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "subject_uuid = EXCLUDED.subject_uuid, "
      + "value = EXCLUDED.value, "
      + "timestamp = EXCLUDED.timestamp")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("subjectUuid") UUID subjectUuid,
              @Bind("value") String value,
              @Bind("timestamp") OffsetDateTime timestamp);

  @SqlQuery(SELECT_WITH_JOINS + " WHERE e.owner_uuid = :ownerUuid AND e.uuid = :uuid")
  Optional<Event> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                            @Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM events WHERE owner_uuid = :ownerUuid AND uuid = :uuid")
  int deleteByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid, @Bind("uuid") UUID uuid);

  @SqlQuery(SELECT_WITH_JOINS + " WHERE e.owner_uuid = :ownerUuid "
      + "AND e.subject_uuid = :subjectUuid ORDER BY e.timestamp")
  List<Event> findByOwnerAndSubject(@Bind("ownerUuid") UUID ownerUuid,
                                     @Bind("subjectUuid") UUID subjectUuid);

  @SqlQuery(SELECT_WITH_JOINS + " WHERE e.owner_uuid = :ownerUuid "
      + "AND e.timestamp >= :from AND e.timestamp <= :to ORDER BY e.timestamp")
  List<Event> findByOwnerAndTimeRange(@Bind("ownerUuid") UUID ownerUuid,
                                       @Bind("from") OffsetDateTime from,
                                       @Bind("to") OffsetDateTime to);

  @SqlQuery(SELECT_WITH_JOINS + " WHERE e.owner_uuid = :ownerUuid "
      + "AND e.subject_uuid = :subjectUuid "
      + "AND e.timestamp >= :from AND e.timestamp <= :to ORDER BY e.timestamp")
  List<Event> findByOwnerSubjectAndTimeRange(@Bind("ownerUuid") UUID ownerUuid,
                                              @Bind("subjectUuid") UUID subjectUuid,
                                              @Bind("from") OffsetDateTime from,
                                              @Bind("to") OffsetDateTime to);

  default void store(Event event) {
    upsert(event.identifier().uuid(),
        event.owner().identifier().uuid(),
        event.subject().identifier().uuid(),
        event.value(),
        event.timestamp().toOffsetDateTime());
  }

  default Optional<Event> get(Owner owner, Identifier identifier) {
    return findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  default boolean delete(Owner owner, Identifier identifier) {
    return deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
  }

  default List<Event> findBySubject(Owner owner, Subject subject) {
    return findByOwnerAndSubject(owner.identifier().uuid(), subject.identifier().uuid());
  }

  default List<Event> findByTimeRange(Owner owner, Timestamp from, Timestamp to) {
    return findByOwnerAndTimeRange(owner.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime());
  }

  default List<Event> findBySubjectAndTimeRange(Owner owner, Subject subject,
                                                 Timestamp from, Timestamp to) {
    return findByOwnerSubjectAndTimeRange(owner.identifier().uuid(),
        subject.identifier().uuid(),
        from.toOffsetDateTime(), to.toOffsetDateTime());
  }

  class EventRowMapper implements RowMapper<Event> {
    @Override
    public Event map(ResultSet rs, StatementContext ctx) throws SQLException {
      Owner owner = Owner.builder(rs.getString("owner_value"))
          .identifier(new Identifier(rs.getObject("owner_uuid", UUID.class)))
          .build();
      Subject subject = Subject.builder(owner,
              new Category(rs.getString("subject_category")),
              rs.getString("subject_value"))
          .identifier(new Identifier(rs.getObject("subject_uuid", UUID.class)))
          .build();
      Identifier identifier = new Identifier(
          rs.getObject("uuid", UUID.class));
      Timestamp timestamp = new Timestamp(
          rs.getTimestamp("timestamp").toInstant());
      return Event.builder(owner, subject, rs.getString("value"))
          .identifier(identifier)
          .timestamp(timestamp)
          .build();
    }
  }

}
