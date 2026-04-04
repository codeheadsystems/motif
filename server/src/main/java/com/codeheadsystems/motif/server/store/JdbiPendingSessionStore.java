package com.codeheadsystems.motif.server.store;

import com.codeheadsystems.hofmann.server.store.PendingSessionStore;
import com.codeheadsystems.motif.server.db.dao.OpaquePendingSessionDao;
import com.codeheadsystems.rfc.opaque.model.ServerAuthState;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PostgreSQL-backed {@link PendingSessionStore} using JDBI.
 * Must be initialized with {@link #initialize(OpaquePendingSessionDao)} before use.
 */
public class JdbiPendingSessionStore implements PendingSessionStore {

  private final AtomicReference<OpaquePendingSessionDao> daoRef = new AtomicReference<>();

  public void initialize(OpaquePendingSessionDao dao) {
    if (!daoRef.compareAndSet(null, dao)) {
      throw new IllegalStateException("JdbiPendingSessionStore already initialized");
    }
  }

  @Override
  public void store(String sessionToken, ServerAuthState state, String credentialIdentifierBase64) {
    store(sessionToken, state, credentialIdentifierBase64, 0);
  }

  @Override
  public void store(String sessionToken, ServerAuthState state,
                    String credentialIdentifierBase64, int keyVersion) {
    dao().insert(
        sessionToken,
        state.expectedClientMac(),
        state.sessionKey(),
        credentialIdentifierBase64,
        keyVersion);
  }

  @Override
  public Optional<PendingSession> remove(String sessionToken) {
    return dao().findAndDeleteByToken(sessionToken)
        .map(r -> new PendingSession(
            new ServerAuthState(r.expectedClientMac(), r.sessionKey()),
            r.credentialIdentifierB64(),
            r.keyVersion()));
  }

  private OpaquePendingSessionDao dao() {
    OpaquePendingSessionDao dao = daoRef.get();
    if (dao == null) {
      throw new IllegalStateException("JdbiPendingSessionStore not initialized");
    }
    return dao;
  }
}
