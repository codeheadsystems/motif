package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.model.ConfigurationValue;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurationValueDaoTest extends DatabaseTest {

  private ConfigurationValueDao dao;

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
