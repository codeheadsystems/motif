package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.model.OpaqueSessionRecord;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpaqueSessionDaoTest extends DatabaseTest {

  private OpaqueSessionDao dao;

  private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);
  private static final OffsetDateTime FUTURE = NOW.plusHours(1);
  private static final OffsetDateTime PAST = NOW.minusHours(1);

  @BeforeEach
  void setUp() {
    jdbi.useHandle(h -> h.execute("DELETE FROM opaque_sessions"));
    dao = jdbi.onDemand(OpaqueSessionDao.class);
  }

  @Test
  void upsertAndFindByJti() {
    dao.upsert("jti-1", "cred-b64", "key-b64", NOW, FUTURE);

    OpaqueSessionRecord result = dao.findByJti("jti-1").orElseThrow();
    assertThat(result.credentialIdentifier()).isEqualTo("cred-b64");
    assertThat(result.sessionKey()).isEqualTo("key-b64");
  }

  @Test
  void findByJtiReturnsEmptyWhenNotFound() {
    assertThat(dao.findByJti("nonexistent")).isEmpty();
  }

  @Test
  void deleteByJti() {
    dao.upsert("jti-1", "cred-b64", "key-b64", NOW, FUTURE);

    assertThat(dao.deleteByJti("jti-1")).isEqualTo(1);
    assertThat(dao.findByJti("jti-1")).isEmpty();
  }

  @Test
  void deleteByCredentialIdentifier() {
    dao.upsert("jti-1", "cred-a", "key-1", NOW, FUTURE);
    dao.upsert("jti-2", "cred-a", "key-2", NOW, FUTURE);
    dao.upsert("jti-3", "cred-b", "key-3", NOW, FUTURE);

    assertThat(dao.deleteByCredentialIdentifier("cred-a")).isEqualTo(2);
    assertThat(dao.findByJti("jti-1")).isEmpty();
    assertThat(dao.findByJti("jti-2")).isEmpty();
    assertThat(dao.findByJti("jti-3")).isPresent();
  }

  @Test
  void deleteExpired() {
    dao.upsert("expired", "cred", "key", PAST.minusHours(2), PAST);
    dao.upsert("active", "cred", "key", NOW, FUTURE);

    assertThat(dao.deleteExpired(NOW)).isEqualTo(1);
    assertThat(dao.findByJti("expired")).isEmpty();
    assertThat(dao.findByJti("active")).isPresent();
  }
}
