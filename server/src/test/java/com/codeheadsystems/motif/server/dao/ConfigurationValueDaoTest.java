package com.codeheadsystems.motif.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.model.ConfigurationValue;
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
class ConfigurationValueDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;
  private ConfigurationValueDao dao;

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
    jdbi.useHandle(handle -> handle.execute("DELETE FROM configuration_values"));
    dao = jdbi.onDemand(ConfigurationValueDao.class);
  }

  @Test
  void upsertAndFindByKey() {
    dao.upsert("db.host", "localhost");

    Optional<ConfigurationValue> result = dao.findByKey("db.host");

    assertThat(result).isPresent();
    assertThat(result.get().key()).isEqualTo("db.host");
    assertThat(result.get().value()).isEqualTo("localhost");
  }

  @Test
  void findByKeyReturnsEmptyWhenNotFound() {
    assertThat(dao.findByKey("nonexistent")).isEmpty();
  }

  @Test
  void upsertUpdatesExistingKey() {
    dao.upsert("db.host", "localhost");
    dao.upsert("db.host", "remotehost");

    Optional<ConfigurationValue> result = dao.findByKey("db.host");

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("remotehost");
  }

  @Test
  void deleteByKeyReturnsOneWhenKeyExists() {
    dao.upsert("db.host", "localhost");

    assertThat(dao.deleteByKey("db.host")).isEqualTo(1);
    assertThat(dao.findByKey("db.host")).isEmpty();
  }

  @Test
  void deleteByKeyReturnsZeroWhenKeyDoesNotExist() {
    assertThat(dao.deleteByKey("nonexistent")).isEqualTo(0);
  }

  @Test
  void multipleKeysAreIsolated() {
    dao.upsert("key.one", "value-1");
    dao.upsert("key.two", "value-2");

    assertThat(dao.findByKey("key.one").get().value()).isEqualTo("value-1");
    assertThat(dao.findByKey("key.two").get().value()).isEqualTo("value-2");

    dao.deleteByKey("key.one");

    assertThat(dao.findByKey("key.one")).isEmpty();
    assertThat(dao.findByKey("key.two")).isPresent();
  }
}
