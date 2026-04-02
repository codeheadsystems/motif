package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.dao.OwnerDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
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
class OwnerManagerIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;
  private OwnerManager ownerManager;

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
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM events");
      handle.execute("DELETE FROM subjects");
      handle.execute("DELETE FROM owners");
    });
    OwnerDao ownerDao = jdbi.onDemand(OwnerDao.class);
    ownerManager = new OwnerManager(ownerDao);
  }

  @Test
  void storeAndGetOwner() {
    Owner owner = new Owner("TEST-OWNER");
    ownerManager.store(owner);

    Optional<Owner> result = ownerManager.get(owner.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("TEST-OWNER");
    assertThat(result.get().identifier().uuid()).isEqualTo(owner.identifier().uuid());
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(ownerManager.get(new Identifier())).isEmpty();
  }

  @Test
  void storeUpdatesExistingOwner() {
    Owner original = new Owner("ORIGINAL");
    ownerManager.store(original);

    Owner updated = new Owner("UPDATED", original.identifier());
    ownerManager.store(updated);

    Optional<Owner> result = ownerManager.get(original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("UPDATED");
  }

  @Test
  void deleteExistingOwner() {
    Owner owner = new Owner("TO-DELETE");
    ownerManager.store(owner);

    assertThat(ownerManager.delete(owner.identifier())).isTrue();
    assertThat(ownerManager.get(owner.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenOwnerDoesNotExist() {
    assertThat(ownerManager.delete(new Identifier())).isFalse();
  }

  @Test
  void findByValue() {
    Owner owner = new Owner("FINDABLE");
    ownerManager.store(owner);

    Optional<Owner> result = ownerManager.find("findable");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(owner.identifier().uuid());
  }

  @Test
  void findByValueReturnsEmptyWhenNotFound() {
    assertThat(ownerManager.find("NONEXISTENT")).isEmpty();
  }
}
