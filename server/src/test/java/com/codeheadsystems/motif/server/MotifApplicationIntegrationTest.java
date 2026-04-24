package com.codeheadsystems.motif.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.flywaydb.core.Flyway;
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
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:latest");

  private static DropwizardTestSupport<MotifConfiguration> SUPPORT;
  private static Client client;

  @BeforeAll
  static void setUp() throws Exception {
    migrateDatabase();

    SUPPORT = new DropwizardTestSupport<>(
        MotifApplication.class,
        ResourceHelpers.resourceFilePath("test-config.yml"),
        ConfigOverride.config("databaseUrl", POSTGRES.getJdbcUrl()),
        ConfigOverride.config("databaseUser", POSTGRES.getUsername()),
        ConfigOverride.config("databasePassword", POSTGRES.getPassword()),
        ConfigOverride.config("serverKeySeedHex", TestSecrets.SERVER_KEY_SEED_HEX),
        ConfigOverride.config("oprfSeedHex", TestSecrets.OPRF_SEED_HEX),
        ConfigOverride.config("oprfMasterKeyHex", TestSecrets.OPRF_MASTER_KEY_HEX),
        ConfigOverride.config("jwtSecretHex", TestSecrets.JWT_SECRET_HEX));
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

  private static void migrateDatabase() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
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
