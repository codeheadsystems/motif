package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.OpaqueCredentialRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(OpaqueCredentialDao.OpaqueCredentialRowMapper.class)
public interface OpaqueCredentialDao {

  @SqlUpdate("INSERT INTO opaque_credentials (credential_id, client_public_key, masking_key, "
      + "envelope_nonce, auth_tag, key_version) "
      + "VALUES (:credentialId, :clientPublicKey, :maskingKey, :envelopeNonce, :authTag, :keyVersion) "
      + "ON CONFLICT (credential_id) DO UPDATE SET "
      + "client_public_key = EXCLUDED.client_public_key, "
      + "masking_key = EXCLUDED.masking_key, "
      + "envelope_nonce = EXCLUDED.envelope_nonce, "
      + "auth_tag = EXCLUDED.auth_tag, "
      + "key_version = EXCLUDED.key_version")
  void upsert(@Bind("credentialId") byte[] credentialId,
              @Bind("clientPublicKey") byte[] clientPublicKey,
              @Bind("maskingKey") byte[] maskingKey,
              @Bind("envelopeNonce") byte[] envelopeNonce,
              @Bind("authTag") byte[] authTag,
              @Bind("keyVersion") int keyVersion);

  @SqlQuery("SELECT * FROM opaque_credentials WHERE credential_id = :credentialId")
  Optional<OpaqueCredentialRecord> findByCredentialId(@Bind("credentialId") byte[] credentialId);

  @SqlUpdate("DELETE FROM opaque_credentials WHERE credential_id = :credentialId")
  int deleteByCredentialId(@Bind("credentialId") byte[] credentialId);

  class OpaqueCredentialRowMapper implements RowMapper<OpaqueCredentialRecord> {
    @Override
    public OpaqueCredentialRecord map(ResultSet rs, StatementContext ctx) throws SQLException {
      return new OpaqueCredentialRecord(
          rs.getBytes("credential_id"),
          rs.getBytes("client_public_key"),
          rs.getBytes("masking_key"),
          rs.getBytes("envelope_nonce"),
          rs.getBytes("auth_tag"),
          rs.getInt("key_version"));
    }
  }
}
