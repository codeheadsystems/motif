package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Note;
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

@RegisterRowMapper(NoteDao.NoteRowMapper.class)
public interface NoteDao {

  String SELECT = "SELECT n.uuid, n.owner_uuid, n.subject_uuid, n.event_uuid, "
      + "n.value, n.timestamp "
      + "FROM notes n";

  @SqlUpdate("INSERT INTO notes (uuid, owner_uuid, subject_uuid, event_uuid, value, timestamp) "
      + "VALUES (:uuid, :ownerUuid, :subjectUuid, CAST(:eventUuid AS UUID), :value, :timestamp) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "subject_uuid = EXCLUDED.subject_uuid, "
      + "event_uuid = EXCLUDED.event_uuid, "
      + "value = EXCLUDED.value, "
      + "timestamp = EXCLUDED.timestamp")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("subjectUuid") UUID subjectUuid,
              @Bind("eventUuid") UUID eventUuid,
              @Bind("value") String value,
              @Bind("timestamp") OffsetDateTime timestamp);

  @SqlQuery(SELECT + " WHERE n.owner_uuid = :ownerUuid AND n.uuid = :uuid")
  Optional<Note> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                           @Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM notes WHERE owner_uuid = :ownerUuid AND uuid = :uuid")
  int deleteByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid, @Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM notes WHERE owner_uuid = :ownerUuid")
  int deleteByOwner(@Bind("ownerUuid") UUID ownerUuid);

  @SqlQuery(SELECT + " WHERE n.owner_uuid = :ownerUuid "
      + "AND n.subject_uuid = :subjectUuid ORDER BY n.timestamp "
      + "LIMIT :limit OFFSET :offset")
  List<Note> findByOwnerAndSubject(@Bind("ownerUuid") UUID ownerUuid,
                                    @Bind("subjectUuid") UUID subjectUuid,
                                    @Bind("limit") int limit,
                                    @Bind("offset") int offset);

  @SqlQuery(SELECT + " WHERE n.owner_uuid = :ownerUuid "
      + "AND n.subject_uuid = :subjectUuid "
      + "AND n.timestamp >= :from AND n.timestamp <= :to ORDER BY n.timestamp "
      + "LIMIT :limit OFFSET :offset")
  List<Note> findByOwnerSubjectAndTimeRange(@Bind("ownerUuid") UUID ownerUuid,
                                             @Bind("subjectUuid") UUID subjectUuid,
                                             @Bind("from") OffsetDateTime from,
                                             @Bind("to") OffsetDateTime to,
                                             @Bind("limit") int limit,
                                             @Bind("offset") int offset);

  @SqlQuery(SELECT + " WHERE n.owner_uuid = :ownerUuid "
      + "AND n.event_uuid = :eventUuid ORDER BY n.timestamp "
      + "LIMIT :limit OFFSET :offset")
  List<Note> findByOwnerAndEvent(@Bind("ownerUuid") UUID ownerUuid,
                                  @Bind("eventUuid") UUID eventUuid,
                                  @Bind("limit") int limit,
                                  @Bind("offset") int offset);

  @SqlQuery(SELECT + " WHERE n.owner_uuid = :ownerUuid "
      + "AND n.event_uuid = :eventUuid "
      + "AND n.timestamp >= :from AND n.timestamp <= :to ORDER BY n.timestamp "
      + "LIMIT :limit OFFSET :offset")
  List<Note> findByOwnerEventAndTimeRange(@Bind("ownerUuid") UUID ownerUuid,
                                           @Bind("eventUuid") UUID eventUuid,
                                           @Bind("from") OffsetDateTime from,
                                           @Bind("to") OffsetDateTime to,
                                           @Bind("limit") int limit,
                                           @Bind("offset") int offset);

  class NoteRowMapper implements RowMapper<Note> {
    @Override
    public Note map(ResultSet rs, StatementContext ctx) throws SQLException {
      Identifier ownerIdentifier = new Identifier(rs.getObject("owner_uuid", UUID.class));
      Identifier subjectIdentifier = new Identifier(rs.getObject("subject_uuid", UUID.class));
      UUID eventUuid = rs.getObject("event_uuid", UUID.class);
      Identifier eventIdentifier = eventUuid != null ? new Identifier(eventUuid) : null;
      Identifier identifier = new Identifier(rs.getObject("uuid", UUID.class));
      Timestamp timestamp = new Timestamp(rs.getTimestamp("timestamp").toInstant());
      return Note.builder()
          .ownerIdentifier(ownerIdentifier)
          .subjectIdentifier(subjectIdentifier)
          .eventIdentifier(eventIdentifier)
          .value(rs.getString("value"))
          .identifier(identifier)
          .timestamp(timestamp)
          .build();
    }
  }

}
