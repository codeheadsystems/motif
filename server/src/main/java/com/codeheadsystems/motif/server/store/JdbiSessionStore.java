package com.codeheadsystems.motif.server.store;

import com.codeheadsystems.hofmann.server.store.SessionData;
import com.codeheadsystems.hofmann.server.store.SessionStore;
import com.codeheadsystems.motif.server.db.dao.OpaqueSessionDao;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PostgreSQL-backed {@link SessionStore} using JDBI.
 * Must be initialized with {@link #initialize(OpaqueSessionDao)} before use.
 */
public class JdbiSessionStore implements SessionStore {

  private final AtomicReference<OpaqueSessionDao> daoRef = new AtomicReference<>();

  public void initialize(OpaqueSessionDao dao) {
    if (!daoRef.compareAndSet(null, dao)) {
      throw new IllegalStateException("JdbiSessionStore already initialized");
    }
  }

  @Override
  public void store(String jti, SessionData sessionData) {
    dao().upsert(
        jti,
        sessionData.credentialIdentifier(),
        sessionData.sessionKey(),
        sessionData.issuedAt().atOffset(ZoneOffset.UTC),
        sessionData.expiresAt().atOffset(ZoneOffset.UTC));
  }

  @Override
  public Optional<SessionData> load(String jti) {
    return dao().findByJti(jti)
        .filter(r -> r.expiresAt().toInstant().isAfter(Instant.now()))
        .map(r -> new SessionData(
            r.credentialIdentifier(),
            r.sessionKey(),
            r.issuedAt().toInstant(),
            r.expiresAt().toInstant()));
  }

  @Override
  public void revoke(String jti) {
    dao().deleteByJti(jti);
  }

  @Override
  public void revokeByCredentialIdentifier(String credentialIdentifierBase64) {
    dao().deleteByCredentialIdentifier(credentialIdentifierBase64);
  }

  private OpaqueSessionDao dao() {
    OpaqueSessionDao dao = daoRef.get();
    if (dao == null) {
      throw new IllegalStateException("JdbiSessionStore not initialized");
    }
    return dao;
  }
}
