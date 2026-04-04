package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.model.OpaquePendingSessionRecord;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class OpaquePendingSessionDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;
  private OpaquePendingSessionDao dao;

  @BeforeAll
  static void setupJdbi() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
    jdbi = Jdbi.create(ds);
    jdbi.installPlugin(new SqlObjectPlugin());
  }

  @BeforeEach
  void setUp() {
    jdbi.useHandle(h -> h.execute("DELETE FROM opaque_pending_sessions"));
    dao = jdbi.onDemand(OpaquePendingSessionDao.class);
  }

  @Test
  void insertAndFindAndDelete() {
    dao.insert("token-1", new byte[]{1, 2}, new byte[]{3, 4}, "cred-b64", 0);

    Optional<OpaquePendingSessionRecord> result = dao.findAndDeleteByToken("token-1");

    assertThat(result).isPresent();
    assertThat(result.get().expectedClientMac()).isEqualTo(new byte[]{1, 2});
    assertThat(result.get().sessionKey()).isEqualTo(new byte[]{3, 4});
    assertThat(result.get().credentialIdentifierB64()).isEqualTo("cred-b64");
    assertThat(result.get().keyVersion()).isEqualTo(0);

    // Consume-once: second call returns empty
    assertThat(dao.findAndDeleteByToken("token-1")).isEmpty();
  }

  @Test
  void findAndDeleteReturnsEmptyWhenNotFound() {
    assertThat(dao.findAndDeleteByToken("nonexistent")).isEmpty();
  }

  @Test
  void deleteExpired() {
    dao.insert("old", new byte[]{1}, new byte[]{2}, "cred", 0);
    // Wait briefly then insert a new one
    dao.insert("new", new byte[]{3}, new byte[]{4}, "cred", 0);

    OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1);
    // Both were created within the last second, so neither should be expired yet
    assertThat(dao.deleteExpired(cutoff.minusMinutes(10))).isEqualTo(0);
  }

  @Test
  void keyVersionIsStored() {
    dao.insert("token-v2", new byte[]{1}, new byte[]{2}, "cred", 2);

    OpaquePendingSessionRecord result = dao.findAndDeleteByToken("token-v2").orElseThrow();
    assertThat(result.keyVersion()).isEqualTo(2);
  }
}
