package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Pattern;
import com.codeheadsystems.motif.server.db.model.PeriodClassification;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(PatternDao.PatternRowMapper.class)
public interface PatternDao {

  String SELECT = "SELECT p.uuid, p.owner_uuid, p.subject_uuid, p.event_value, "
      + "p.period_classification, p.interval_mean_seconds, p.occurrence_count, "
      + "p.confidence, p.last_seen_at, p.next_expected_at, p.score, p.detected_at "
      + "FROM patterns p";

  @SqlUpdate("INSERT INTO patterns (uuid, owner_uuid, subject_uuid, event_value, "
      + "period_classification, interval_mean_seconds, occurrence_count, confidence, "
      + "last_seen_at, next_expected_at, score, detected_at) "
      + "VALUES (:uuid, :ownerUuid, :subjectUuid, :eventValue, :period, :intervalMeanSeconds, "
      + ":occurrenceCount, :confidence, :lastSeenAt, :nextExpectedAt, :score, :detectedAt) "
      + "ON CONFLICT (owner_uuid, subject_uuid, event_value) DO UPDATE SET "
      + "period_classification = EXCLUDED.period_classification, "
      + "interval_mean_seconds = EXCLUDED.interval_mean_seconds, "
      + "occurrence_count = EXCLUDED.occurrence_count, "
      + "confidence = EXCLUDED.confidence, "
      + "last_seen_at = EXCLUDED.last_seen_at, "
      + "next_expected_at = EXCLUDED.next_expected_at, "
      + "score = EXCLUDED.score, "
      + "detected_at = EXCLUDED.detected_at")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("subjectUuid") UUID subjectUuid,
              @Bind("eventValue") String eventValue,
              @Bind("period") String period,
              @Bind("intervalMeanSeconds") long intervalMeanSeconds,
              @Bind("occurrenceCount") int occurrenceCount,
              @Bind("confidence") double confidence,
              @Bind("lastSeenAt") OffsetDateTime lastSeenAt,
              @Bind("nextExpectedAt") OffsetDateTime nextExpectedAt,
              @Bind("score") double score,
              @Bind("detectedAt") OffsetDateTime detectedAt);

  @SqlQuery(SELECT + " WHERE p.owner_uuid = :ownerUuid "
      + "ORDER BY p.score DESC LIMIT :limit OFFSET :offset")
  List<Pattern> findByOwner(@Bind("ownerUuid") UUID ownerUuid,
                            @Bind("limit") int limit,
                            @Bind("offset") int offset);

  @SqlQuery(SELECT + " WHERE p.owner_uuid = :ownerUuid AND p.uuid = :uuid")
  java.util.Optional<Pattern> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                                        @Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM patterns WHERE owner_uuid = :ownerUuid")
  int deleteByOwner(@Bind("ownerUuid") UUID ownerUuid);

  class PatternRowMapper implements RowMapper<Pattern> {
    @Override
    public Pattern map(ResultSet rs, StatementContext ctx) throws SQLException {
      return Pattern.builder()
          .ownerIdentifier(new Identifier(rs.getObject("owner_uuid", UUID.class)))
          .subjectIdentifier(new Identifier(rs.getObject("subject_uuid", UUID.class)))
          .eventValue(rs.getString("event_value"))
          .period(PeriodClassification.valueOf(rs.getString("period_classification")))
          .intervalMeanSeconds(rs.getLong("interval_mean_seconds"))
          .occurrenceCount(rs.getInt("occurrence_count"))
          .confidence(rs.getDouble("confidence"))
          .lastSeenAt(new Timestamp(rs.getTimestamp("last_seen_at").toInstant()))
          .nextExpectedAt(new Timestamp(rs.getTimestamp("next_expected_at").toInstant()))
          .score(rs.getDouble("score"))
          .detectedAt(new Timestamp(rs.getTimestamp("detected_at").toInstant()))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .build();
    }
  }
}
