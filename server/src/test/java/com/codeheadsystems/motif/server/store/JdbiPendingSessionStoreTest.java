package com.codeheadsystems.motif.server.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.server.store.PendingSessionStore;
import com.codeheadsystems.motif.server.db.dao.OpaquePendingSessionDao;
import com.codeheadsystems.motif.server.db.model.OpaquePendingSessionRecord;
import com.codeheadsystems.rfc.opaque.model.ServerAuthState;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JdbiPendingSessionStoreTest {

  @Mock
  private OpaquePendingSessionDao dao;

  private JdbiPendingSessionStore store;

  private static final byte[] MAC = {1, 2, 3};
  private static final byte[] KEY = {4, 5, 6};

  @BeforeEach
  void setUp() {
    store = new JdbiPendingSessionStore();
    store.initialize(dao);
  }

  @Test
  void storeExtractsServerAuthStateFields() {
    ServerAuthState state = new ServerAuthState(MAC, KEY);

    store.store("token-1", state, "cred-b64", 2);

    verify(dao).insert("token-1", MAC, KEY, "cred-b64", 2);
  }

  @Test
  void storeDefaultKeyVersionIsZero() {
    ServerAuthState state = new ServerAuthState(MAC, KEY);

    store.store("token-1", state, "cred-b64");

    verify(dao).insert("token-1", MAC, KEY, "cred-b64", 0);
  }

  @Test
  void removeReconstructsPendingSession() {
    when(dao.findAndDeleteByToken("token-1"))
        .thenReturn(Optional.of(new OpaquePendingSessionRecord(
            "token-1", MAC, KEY, "cred-b64", 3, OffsetDateTime.now(ZoneOffset.UTC))));

    Optional<PendingSessionStore.PendingSession> result = store.remove("token-1");

    assertThat(result).isPresent();
    assertThat(result.get().state().expectedClientMac()).isEqualTo(MAC);
    assertThat(result.get().state().sessionKey()).isEqualTo(KEY);
    assertThat(result.get().credentialIdentifierBase64()).isEqualTo("cred-b64");
    assertThat(result.get().keyVersion()).isEqualTo(3);
  }

  @Test
  void removeReturnsEmptyWhenNotFound() {
    when(dao.findAndDeleteByToken("token-1")).thenReturn(Optional.empty());

    assertThat(store.remove("token-1")).isEmpty();
  }
}
