package com.codeheadsystems.motif.server;

import com.codeheadsystems.motif.server.db.dao.OpaqueCredentialDao;
import com.codeheadsystems.motif.server.db.dao.OpaquePendingSessionDao;
import com.codeheadsystems.motif.server.db.dao.OpaqueSessionDao;
import com.codeheadsystems.motif.server.store.JdbiCredentialStore;
import com.codeheadsystems.motif.server.store.JdbiPendingSessionStore;
import com.codeheadsystems.motif.server.store.JdbiSessionStore;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registered before HofmannBundle to validate required secrets and initialize JDBI + stores.
 * <p>
 * Secrets (OPAQUE seeds, OPRF master key, JWT signing secret) are expected to be present on
 * the configuration object by the time this bundle runs — loaded from YAML with
 * {@code ${ENV_VAR}} substitution (dev: env vars in docker-compose; prod: AWS Secrets Manager
 * injected as ECS task env vars; tests: {@link io.dropwizard.testing.ConfigOverride}).
 * <p>
 * This bundle does NOT run Flyway migrations. Run the {@code init-db} command before starting
 * the server against a fresh database.
 */
public class SetupBundle implements ConfiguredBundle<MotifConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(SetupBundle.class);

  private final JdbiCredentialStore credentialStore;
  private final JdbiSessionStore sessionStore;
  private final JdbiPendingSessionStore pendingSessionStore;
  private Jdbi jdbi;

  public SetupBundle(JdbiCredentialStore credentialStore,
                     JdbiSessionStore sessionStore,
                     JdbiPendingSessionStore pendingSessionStore) {
    this.credentialStore = credentialStore;
    this.sessionStore = sessionStore;
    this.pendingSessionStore = pendingSessionStore;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no-op
  }

  @Override
  public void run(MotifConfiguration configuration, Environment environment) {
    verifyRequiredSecrets(configuration);

    DataSource dataSource = buildDataSource(configuration);
    this.jdbi = Jdbi.create(dataSource);
    jdbi.installPlugin(new SqlObjectPlugin());

    credentialStore.initialize(jdbi.onDemand(OpaqueCredentialDao.class));
    sessionStore.initialize(jdbi.onDemand(OpaqueSessionDao.class));
    pendingSessionStore.initialize(jdbi.onDemand(OpaquePendingSessionDao.class));
  }

  /**
   * Fail-fast check that OPAQUE seeds and JWT secret are set. Prevents silent fallback to
   * HofmannBundle's dev-mode random generation (which invalidates all registrations on restart).
   */
  private void verifyRequiredSecrets(MotifConfiguration configuration) {
    List<String> missing = new ArrayList<>();
    if (isBlank(configuration.getServerKeySeedHex())) missing.add("serverKeySeedHex");
    if (isBlank(configuration.getOprfSeedHex())) missing.add("oprfSeedHex");
    if (isBlank(configuration.getOprfMasterKeyHex())) missing.add("oprfMasterKeyHex");
    if (isBlank(configuration.getJwtSecretHex())) missing.add("jwtSecretHex");
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Missing required secrets in configuration: " + missing
              + ". Set environment variables: "
              + "MOTIF_OPAQUE_SERVER_KEY_SEED_HEX, "
              + "MOTIF_OPAQUE_OPRF_SEED_HEX, "
              + "MOTIF_OPAQUE_OPRF_MASTER_KEY_HEX, "
              + "MOTIF_JWT_SECRET_HEX. "
              + "Generate values with: openssl rand -hex 32");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  public Jdbi getJdbi() {
    return Objects.requireNonNull(jdbi, "SetupBundle has not been run yet");
  }

  private DataSource buildDataSource(MotifConfiguration configuration) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(configuration.getDatabaseUrl());
    ds.setUser(configuration.getDatabaseUser());
    ds.setPassword(configuration.getDatabasePassword());
    return ds;
  }
}
