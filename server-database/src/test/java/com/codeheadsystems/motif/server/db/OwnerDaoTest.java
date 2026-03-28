package com.codeheadsystems.motif.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Owner;
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
class OwnerDaoTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static Jdbi jdbi;
  private OwnerDao dao;

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
    dao = jdbi.onDemand(OwnerDao.class);
  }

  @Test
  void storeAndRetrieveOwner() {
    Owner owner = new Owner("TEST-OWNER");
    dao.store(owner);

    Optional<Owner> result = dao.get(owner.identifier());

    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("TEST-OWNER");
    assertThat(result.get().identifier().uuid()).isEqualTo(owner.identifier().uuid());
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    assertThat(dao.get(new Identifier())).isEmpty();
  }

  @Test
  void storeUpdatesExistingOwner() {
    Owner original = new Owner("ORIGINAL");
    dao.store(original);

    Owner updated = new Owner("UPDATED", original.identifier());
    dao.store(updated);

    Optional<Owner> result = dao.get(original.identifier());
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo("UPDATED");
  }

  @Test
  void deleteReturnsTrueWhenOwnerExists() {
    Owner owner = new Owner("TO-DELETE");
    dao.store(owner);

    assertThat(dao.delete(owner.identifier())).isTrue();
    assertThat(dao.get(owner.identifier())).isEmpty();
  }

  @Test
  void deleteReturnsFalseWhenOwnerDoesNotExist() {
    assertThat(dao.delete(new Identifier())).isFalse();
  }

  @Test
  void findByValueReturnsMatch() {
    Owner owner = new Owner("FINDABLE");
    dao.store(owner);

    Optional<Owner> result = dao.find("findable");

    assertThat(result).isPresent();
    assertThat(result.get().identifier().uuid()).isEqualTo(owner.identifier().uuid());
  }

  @Test
  void findByValueReturnsEmptyWhenNotFound() {
    assertThat(dao.find("NONEXISTENT")).isEmpty();
  }
}
