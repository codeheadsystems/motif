package com.codeheadsystems.motif.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@ExtendWith(DropwizardExtensionsSupport.class)
class MotifApplicationIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static final DropwizardAppExtension<MotifConfiguration> APP =
      new DropwizardAppExtension<>(
          MotifApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"),
          ConfigOverride.config("databaseUrl", () -> POSTGRES.getJdbcUrl()),
          ConfigOverride.config("databaseUser", () -> POSTGRES.getUsername()),
          ConfigOverride.config("databasePassword", () -> POSTGRES.getPassword()));

  @Test
  void helloWorldEndpointReturnsOk() {
    Client client = APP.client();
    Response response = client.target(
            String.format("http://localhost:%d/", APP.getLocalPort()))
        .request()
        .get();

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.readEntity(String.class);
    assertThat(body).contains("Hello, World!");
  }

  @Test
  void opaqueConfigEndpointReturnsOk() {
    Client client = APP.client();
    Response response = client.target(
            String.format("http://localhost:%d/opaque/config", APP.getLocalPort()))
        .request()
        .get();

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.readEntity(String.class);
    assertThat(body).contains("cipherSuite");
  }
}
