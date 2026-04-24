package com.codeheadsystems.motif.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.hofmann.client.accessor.HofmannOpaqueAccessor;
import com.codeheadsystems.hofmann.client.manager.HofmannOpaqueClientManager;
import com.codeheadsystems.hofmann.client.model.ServerConnectionInfo;
import com.codeheadsystems.hofmann.client.model.ServerIdentifier;
import com.codeheadsystems.hofmann.model.opaque.AuthFinishResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration test for the full OPAQUE flow:
 * register → authenticate → change password → verify old password fails → authenticate with new password.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpaqueFlowIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:latest");

  private static DropwizardTestSupport<MotifConfiguration> SUPPORT;
  private static Client jerseyClient;
  private static HofmannOpaqueClientManager opaqueClient;
  private static ServerIdentifier SERVER_ID;

  private static final String CRED_ID = "testuser";
  private static final String OLD_PASSWORD = "old-password-123";
  private static final String NEW_PASSWORD = "new-password-456";

  // Shared state across ordered tests
  private static String jwtToken;

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
    jerseyClient = ClientBuilder.newClient();

    // Set up hofmann-client pointing at the test server
    String baseUrl = String.format("http://localhost:%d", SUPPORT.getLocalPort());
    SERVER_ID = new ServerIdentifier("test");
    ServerConnectionInfo connInfo = new ServerConnectionInfo(URI.create(baseUrl));
    HofmannOpaqueAccessor accessor = new HofmannOpaqueAccessor(
        HttpClient.newHttpClient(),
        new ObjectMapper(),
        Map.of(SERVER_ID, connInfo));
    opaqueClient = new HofmannOpaqueClientManager(accessor);
  }

  @AfterAll
  static void tearDown() {
    if (jerseyClient != null) jerseyClient.close();
    if (SUPPORT != null) SUPPORT.after();
  }

  private static void migrateDatabase() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
  }

  @Test
  @Order(1)
  void register() {
    opaqueClient.register(SERVER_ID,
        CRED_ID.getBytes(StandardCharsets.UTF_8),
        OLD_PASSWORD.getBytes(StandardCharsets.UTF_8));
    // No exception = success
  }

  @Test
  @Order(2)
  void authenticateWithOriginalPassword() {
    AuthFinishResponse response = opaqueClient.authenticate(SERVER_ID,
        CRED_ID.getBytes(StandardCharsets.UTF_8),
        OLD_PASSWORD.getBytes(StandardCharsets.UTF_8));

    assertThat(response.token()).isNotNull().isNotEmpty();
    jwtToken = response.token();
  }

  @Test
  @Order(3)
  void jwtTokenAccessesProtectedEndpoint() {
    Response response = jerseyClient.target(
            String.format("http://localhost:%d/api/owner", SUPPORT.getLocalPort()))
        .request()
        .header("Authorization", "Bearer " + jwtToken)
        .get();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @Order(4)
  void changePassword() {
    // Authenticate with old password to get a fresh token for the change
    AuthFinishResponse authResp = opaqueClient.authenticate(SERVER_ID,
        CRED_ID.getBytes(StandardCharsets.UTF_8),
        OLD_PASSWORD.getBytes(StandardCharsets.UTF_8));

    opaqueClient.changePassword(SERVER_ID,
        CRED_ID.getBytes(StandardCharsets.UTF_8),
        NEW_PASSWORD.getBytes(StandardCharsets.UTF_8),
        authResp.token());
  }

  @Test
  @Order(5)
  void oldPasswordNoLongerWorks() {
    assertThatThrownBy(() -> opaqueClient.authenticate(SERVER_ID,
        CRED_ID.getBytes(StandardCharsets.UTF_8),
        OLD_PASSWORD.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  @Order(6)
  void authenticateWithNewPassword() {
    AuthFinishResponse response = opaqueClient.authenticate(SERVER_ID,
        CRED_ID.getBytes(StandardCharsets.UTF_8),
        NEW_PASSWORD.getBytes(StandardCharsets.UTF_8));

    assertThat(response.token()).isNotNull().isNotEmpty();
    jwtToken = response.token();
  }

  @Test
  @Order(7)
  void newJwtTokenAccessesProtectedEndpoint() {
    Response response = jerseyClient.target(
            String.format("http://localhost:%d/api/owner", SUPPORT.getLocalPort()))
        .request()
        .header("Authorization", "Bearer " + jwtToken)
        .get();

    assertThat(response.getStatus()).isEqualTo(200);
  }
}
