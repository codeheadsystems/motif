package com.codeheadsystems.motif.server;

import com.codeheadsystems.motif.server.db.dao.ConfigurationValueDao;
import com.codeheadsystems.motif.server.db.dao.OpaqueCredentialDao;
import com.codeheadsystems.motif.server.db.dao.OpaquePendingSessionDao;
import com.codeheadsystems.motif.server.db.dao.OpaqueSessionDao;
import com.codeheadsystems.motif.server.db.model.ConfigurationValue;
import com.codeheadsystems.motif.server.store.JdbiCredentialStore;
import com.codeheadsystems.motif.server.store.JdbiPendingSessionStore;
import com.codeheadsystems.motif.server.store.JdbiSessionStore;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registered before HofmannBundle to initialize JDBI, stores, and load config from the database.
 * <p>
 * This bundle does NOT run Flyway migrations or generate keys. The database must be
 * initialized beforehand by running the {@code init-db} command.
 */
public class SetupBundle implements ConfiguredBundle<MotifConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(SetupBundle.class);
  private static final List<String> REQUIRED_KEYS = List.of(
      "hofmann.serverKeySeedHex",
      "hofmann.oprfSeedHex",
      "hofmann.oprfMasterKeyHex",
      "hofmann.jwtSecretHex",
      "hofmann.context"
  );

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
    DataSource dataSource = buildDataSource(configuration);

    this.jdbi = Jdbi.create(dataSource);
    jdbi.installPlugin(new SqlObjectPlugin());

    // Initialize stores with DAOs
    credentialStore.initialize(jdbi.onDemand(OpaqueCredentialDao.class));
    sessionStore.initialize(jdbi.onDemand(OpaqueSessionDao.class));
    pendingSessionStore.initialize(jdbi.onDemand(OpaquePendingSessionDao.class));

    // Load OPAQUE config from database — fail if keys are missing
    ConfigurationValueDao configDao = jdbi.onDemand(ConfigurationValueDao.class);
    verifyRequiredKeys(configDao);
    loadConfigFromDatabase(configuration, configDao);
  }

  private void verifyRequiredKeys(ConfigurationValueDao configDao) {
    List<String> missing = REQUIRED_KEYS.stream()
        .filter(key -> configDao.findByKey(key).isEmpty())
        .toList();
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Missing required OPAQUE configuration keys in database: " + missing
              + ". Run 'java -jar motif.jar init-db config.yml' to initialize the database.");
    }
  }

  private void loadConfigFromDatabase(MotifConfiguration configuration, ConfigurationValueDao configDao) {
    configDao.findByKey("hofmann.serverKeySeedHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setServerKeySeedHex);
    configDao.findByKey("hofmann.oprfSeedHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setOprfSeedHex);
    configDao.findByKey("hofmann.oprfMasterKeyHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setOprfMasterKeyHex);
    configDao.findByKey("hofmann.jwtSecretHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setJwtSecretHex);
    configDao.findByKey("hofmann.context")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setContext);
  }

  /**
   * Returns the JDBI instance created during run(). Available after SetupBundle.run() completes.
   */
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
