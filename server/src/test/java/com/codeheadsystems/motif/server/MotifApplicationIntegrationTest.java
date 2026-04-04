package com.codeheadsystems.motif.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class MotifApplicationIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static DropwizardTestSupport<MotifConfiguration> SUPPORT;
  private static Client client;

  @BeforeAll
  static void setUp() throws Exception {
    // Initialize the database (migrations + keys) before the app starts — same as init-db command
    initializeDatabase();

    SUPPORT = new DropwizardTestSupport<>(
        MotifApplication.class,
        ResourceHelpers.resourceFilePath("test-config.yml"),
        ConfigOverride.config("databaseUrl", POSTGRES.getJdbcUrl()),
        ConfigOverride.config("databaseUser", POSTGRES.getUsername()),
        ConfigOverride.config("databasePassword", POSTGRES.getPassword()));
    SUPPORT.before();
    client = ClientBuilder.newClient();
  }

  @AfterAll
  static void tearDown() {
    if (client != null) {
      client.close();
    }
    if (SUPPORT != null) {
      SUPPORT.after();
    }
  }

  private static void initializeDatabase() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());

    Flyway.configure().dataSource(ds).load().migrate();

    Jdbi jdbi = Jdbi.create(ds);
    jdbi.installPlugin(new SqlObjectPlugin());

    SecureRandom random = new SecureRandom();
    HexFormat hex = HexFormat.of();

    List<String> hexKeys = List.of(
        "hofmann.serverKeySeedHex",
        "hofmann.oprfSeedHex",
        "hofmann.oprfMasterKeyHex",
        "hofmann.jwtSecretHex");

    jdbi.useHandle(handle -> {
      for (String key : hexKeys) {
        byte[] value = new byte[32];
        random.nextBytes(value);
        handle.execute(
            "INSERT INTO configuration_values (key, value) VALUES (?, ?) ON CONFLICT DO NOTHING",
            key, hex.formatHex(value));
      }
      handle.execute(
          "INSERT INTO configuration_values (key, value) VALUES (?, ?) ON CONFLICT DO NOTHING",
          "hofmann.context", "motif-test-v1");
    });
  }

  @Test
  void helloWorldEndpointReturnsOk() {
    Response response = client.target(
            String.format("http://localhost:%d/", SUPPORT.getLocalPort()))
        .request()
        .get();

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.readEntity(String.class);
    assertThat(body).contains("Hello, World!");
  }

  @Test
  void opaqueConfigEndpointReturnsOk() {
    Response response = client.target(
            String.format("http://localhost:%d/opaque/config", SUPPORT.getLocalPort()))
        .request()
        .get();

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.readEntity(String.class);
    assertThat(body).contains("cipherSuite");
  }
}
