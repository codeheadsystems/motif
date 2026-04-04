package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.OpaqueSessionRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(OpaqueSessionDao.OpaqueSessionRowMapper.class)
public interface OpaqueSessionDao {

  @SqlUpdate("INSERT INTO opaque_sessions (jti, credential_identifier, session_key, issued_at, expires_at) "
      + "VALUES (:jti, :credentialIdentifier, :sessionKey, :issuedAt, :expiresAt) "
      + "ON CONFLICT (jti) DO UPDATE SET "
      + "credential_identifier = EXCLUDED.credential_identifier, "
      + "session_key = EXCLUDED.session_key, "
      + "issued_at = EXCLUDED.issued_at, "
      + "expires_at = EXCLUDED.expires_at")
  void upsert(@Bind("jti") String jti,
              @Bind("credentialIdentifier") String credentialIdentifier,
              @Bind("sessionKey") String sessionKey,
              @Bind("issuedAt") OffsetDateTime issuedAt,
              @Bind("expiresAt") OffsetDateTime expiresAt);

  @SqlQuery("SELECT * FROM opaque_sessions WHERE jti = :jti")
  Optional<OpaqueSessionRecord> findByJti(@Bind("jti") String jti);

  @SqlUpdate("DELETE FROM opaque_sessions WHERE jti = :jti")
  int deleteByJti(@Bind("jti") String jti);

  @SqlUpdate("DELETE FROM opaque_sessions WHERE credential_identifier = :credentialIdentifier")
  int deleteByCredentialIdentifier(@Bind("credentialIdentifier") String credentialIdentifier);

  @SqlUpdate("DELETE FROM opaque_sessions WHERE expires_at < :now")
  int deleteExpired(@Bind("now") OffsetDateTime now);

  class OpaqueSessionRowMapper implements RowMapper<OpaqueSessionRecord> {
    @Override
    public OpaqueSessionRecord map(ResultSet rs, StatementContext ctx) throws SQLException {
      return new OpaqueSessionRecord(
          rs.getString("jti"),
          rs.getString("credential_identifier"),
          rs.getString("session_key"),
          rs.getObject("issued_at", OffsetDateTime.class),
          rs.getObject("expires_at", OffsetDateTime.class));
    }
  }
}
