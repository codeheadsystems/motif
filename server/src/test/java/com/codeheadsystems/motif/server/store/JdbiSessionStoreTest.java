package com.codeheadsystems.motif.server.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.server.store.SessionData;
import com.codeheadsystems.motif.server.db.dao.OpaqueSessionDao;
import com.codeheadsystems.motif.server.db.model.OpaqueSessionRecord;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JdbiSessionStoreTest {

  @Mock
  private OpaqueSessionDao dao;

  private JdbiSessionStore store;

  @BeforeEach
  void setUp() {
    store = new JdbiSessionStore();
    store.initialize(dao);
  }

  @Test
  void storeCallsUpsert() {
    Instant now = Instant.now();
    Instant later = now.plusSeconds(3600);
    SessionData data = new SessionData("cred-b64", "key-b64", now, later);

    store.store("jti-1", data);

    verify(dao).upsert(eq("jti-1"), eq("cred-b64"), eq("key-b64"), any(), any());
  }

  @Test
  void loadReturnsDataWhenNotExpired() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime future = now.plusHours(1);
    when(dao.findByJti("jti-1"))
        .thenReturn(Optional.of(new OpaqueSessionRecord("jti-1", "cred", "key", now, future)));

    Optional<SessionData> result = store.load("jti-1");

    assertThat(result).isPresent();
    assertThat(result.get().credentialIdentifier()).isEqualTo("cred");
  }

  @Test
  void loadReturnsEmptyWhenExpired() {
    OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
    OffsetDateTime pastMore = past.minusHours(1);
    when(dao.findByJti("jti-1"))
        .thenReturn(Optional.of(new OpaqueSessionRecord("jti-1", "cred", "key", pastMore, past)));

    assertThat(store.load("jti-1")).isEmpty();
  }

  @Test
  void loadReturnsEmptyWhenNotFound() {
    when(dao.findByJti("jti-1")).thenReturn(Optional.empty());

    assertThat(store.load("jti-1")).isEmpty();
  }

  @Test
  void revokeCallsDelete() {
    store.revoke("jti-1");

    verify(dao).deleteByJti("jti-1");
  }

  @Test
  void revokeByCredentialIdentifierCallsDelete() {
    store.revokeByCredentialIdentifier("cred-b64");

    verify(dao).deleteByCredentialIdentifier("cred-b64");
  }
}
