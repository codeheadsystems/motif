package com.codeheadsystems.motif.server.db;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests that need a PostgreSQL database.
 * Starts a Testcontainers PostgreSQL instance, runs Flyway migrations,
 * and provides a JDBI instance to subclasses.
 */
@Testcontainers
public abstract class DatabaseTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:latest");

  protected static Jdbi jdbi;

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
}
