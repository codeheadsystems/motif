package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.OpaquePendingSessionRecord;
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

@RegisterRowMapper(OpaquePendingSessionDao.OpaquePendingSessionRowMapper.class)
public interface OpaquePendingSessionDao {

  @SqlUpdate("INSERT INTO opaque_pending_sessions "
      + "(session_token, expected_client_mac, session_key, credential_identifier_b64, key_version) "
      + "VALUES (:sessionToken, :expectedClientMac, :sessionKey, :credentialIdentifierB64, :keyVersion)")
  void insert(@Bind("sessionToken") String sessionToken,
              @Bind("expectedClientMac") byte[] expectedClientMac,
              @Bind("sessionKey") byte[] sessionKey,
              @Bind("credentialIdentifierB64") String credentialIdentifierB64,
              @Bind("keyVersion") int keyVersion);

  @SqlQuery("DELETE FROM opaque_pending_sessions WHERE session_token = :sessionToken "
      + "RETURNING *")
  Optional<OpaquePendingSessionRecord> findAndDeleteByToken(@Bind("sessionToken") String sessionToken);

  @SqlUpdate("DELETE FROM opaque_pending_sessions WHERE created_at < :cutoff")
  int deleteExpired(@Bind("cutoff") OffsetDateTime cutoff);

  class OpaquePendingSessionRowMapper implements RowMapper<OpaquePendingSessionRecord> {
    @Override
    public OpaquePendingSessionRecord map(ResultSet rs, StatementContext ctx) throws SQLException {
      return new OpaquePendingSessionRecord(
          rs.getString("session_token"),
          rs.getBytes("expected_client_mac"),
          rs.getBytes("session_key"),
          rs.getString("credential_identifier_b64"),
          rs.getInt("key_version"),
          rs.getObject("created_at", OffsetDateTime.class));
    }
  }
}
